package server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Base64;

/*------------------------------------------------------------------------------
  App: Spring Boot REST service
  Role: Accepts a JPEG image, calls OpenAI with the image embedded as a data URL,
        expects the model to return JSON with question/options/answer, persists
        an MCQ item via JPA, and returns a single-letter code ("A".."D" or "E").

  Endpoints:
    GET  /ping      -> "pong" (health check)
    POST /upload    -> body: image/jpeg; response: text/plain (single char)

  Notes:
    - API key is injected from application.properties (openai.api.key).
    - OkHttp is used for outbound HTTP to OpenAI (separate from Spring’s WebClient).
    - Timeouts are conservative to avoid hanging sockets on slow upstreams.
    - JPA repository is injected via constructor for persistence.
    - On any validation/processing failure, the response is "E".
------------------------------------------------------------------------------*/
@SpringBootApplication
@RestController
public class Server {

  public static void main(String[] args) {
    SpringApplication.run(Server.class, args);
  }

  /** Injected from application.properties (or env var OPENAI_API_KEY). */
  @Value("${openai.api.key}")
  private String apiKey;

  /** OkHttp client with timeouts. */
  private final OkHttpClient client = new OkHttpClient.Builder()
      .connectTimeout(Duration.ofSeconds(20))
      .writeTimeout(Duration.ofSeconds(30))
      .readTimeout(Duration.ofSeconds(60))
      .build();

  /** JSON (de)serializer for request/response bodies. */
  private final ObjectMapper mapper = new ObjectMapper();

  /** JPA repository injected by Spring. */
  private final org.springframework.data.jpa.repository.JpaRepository<server.McqItem, java.lang.Long> repo;

  // Constructor-based injection of a domain-specific repository interface.
  // The compiler accepts this assignment even though the field type is declared
  // as a more general JpaRepository (private) — it’s still compatible.
  public Server(McqItemRepository repo) {
    this.repo = repo; // ✅ this is legal even though repo is private
  }

  /** Simple health check.
   *  Returns "pong" with Connection: close to discourage keep-alive from tiny clients.
   */
  @GetMapping(path = "/ping", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> ping() {
    return ResponseEntity.ok()
        .header("Connection", "close")
        .body("pong");
  }

  /** Main upload endpoint.
   *  Contract:
   *    - Request: Content-Type image/jpeg, raw bytes in body.
   *    - Response: single ASCII char "A".."D" for a recognized answer; "E" on any error.
   *  Steps:
   *    1) Validate input length (simple sanity checks).
   *    2) Base64-encode and pack as data URL for OpenAI.
   *    3) Build chat payload with system+user+image message.
   *    4) POST to OpenAI; parse content text.
   *    5) Extract first {...} JSON block; validate presence of fields.
   *    6) Persist MCQ to DB; return extracted single-letter answer.
   *    7) Fallback to "E" for any failure paths.
   */
  @PostMapping(
      path = "/upload",
      consumes = MediaType.IMAGE_JPEG_VALUE,
      produces = MediaType.TEXT_PLAIN_VALUE
  )
  public ResponseEntity<String> handleUpload(@RequestBody byte[] imageBytes) {
    String letter = "E"; // default on failure
    try {
      // Basic input guardrails: null/empty or suspiciously large -> "E".
      if (imageBytes == null || imageBytes.length == 0 || imageBytes.length > 1_500_000) {
        return ResponseEntity.ok().header("Connection", "close").body("E");
      }

      // Encode as a data URL for the model’s image input.
      String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);

      // --- build OpenAI chat request ---
      ObjectNode payload = mapper.createObjectNode();
      payload.put("model", "gpt-4o");     // model name; ensure it matches your account access
      payload.put("temperature", 0);      // deterministic output for parsing
      payload.put("max_tokens", 250);     // cap response size
      ArrayNode messages = payload.putArray("messages");

      // System prompt: constrain output to pure JSON in a known schema.
      ObjectNode sys = mapper.createObjectNode();
      sys.put("role", "system");
      sys.put("content",
          "You are an OCR and multiple-choice extraction assistant. "
        + "Return ONLY valid JSON in EXACTLY this schema:\n"
        + "{ \"question\": \"<string>\", \"options\": {\"A\":\"..\",\"B\":\"..\",\"C\":\"..\",\"D\":\"..\"}, \"answer\":\"A|B|C|D\" }\n"
        + "Or { \"unreadable\": true } if unclear. No text outside JSON."
      );
      messages.add(sys);

      // User message: text instruction + image content (as data URL).
      ObjectNode user = mapper.createObjectNode();
      user.put("role", "user");
      ArrayNode content = mapper.createArrayNode();
      ObjectNode textPart = mapper.createObjectNode();
      textPart.put("type", "text");
      textPart.put("text", "Extract question, options A–D, and the correct answer letter (A–D).");
      content.add(textPart);
      ObjectNode imagePart = mapper.createObjectNode();
      imagePart.put("type", "image_url");
      ObjectNode imageUrl = mapper.createObjectNode();
      imageUrl.put("url", dataUrl);
      imageUrl.put("detail", "auto");   // let the API pick detail level
      imagePart.set("image_url", imageUrl);
      content.add(imagePart);
      user.set("content", content);
      messages.add(user);

      // Serialize payload as JSON.
      String jsonReq = mapper.writeValueAsString(payload);

      // Use fully qualified OkHttp types to avoid clash with Spring
      okhttp3.MediaType JSON = okhttp3.MediaType.get("application/json");
      okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonReq, JSON);

      // Build HTTP request to OpenAI; includes Authorization bearer token.
      Request req = new Request.Builder()
          .url("https://api.openai.com/v1/chat/completions")
          .header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json")
          .post(body)
          .build();

      // Execute synchronously; try-with-resources to ensure response is closed.
      try (Response resp = client.newCall(req).execute()) {
        if (!resp.isSuccessful()) {
          System.err.printf("OpenAI error %d%n", resp.code());
          return ResponseEntity.ok().header("Connection", "close").body("E");
        }

        // Read full JSON response as text (for debugging, also printed).
        String full = resp.body() != null ? resp.body().string() : "";
        System.out.println("FULL RESPONSE: " + full);

        // Extract assistant’s "content" field (free text); then isolate first block.
        String contentText = mapper.readTree(full)
            .path("choices").path(0).path("message").path("content").asText("");

        int start = contentText.indexOf('{');
        int end   = contentText.lastIndexOf('}');
        if (start >= 0 && end > start) {
          String jsonOnly = contentText.substring(start, end + 1);
          JsonNode root = mapper.readTree(jsonOnly);

          // If the model said it's unreadable, bail.
          boolean unreadable = root.has("unreadable") && root.path("unreadable").asBoolean(false);
          if (!unreadable) {
            // Pull out fields with basic safety trimming.
            String question = safeText(root.path("question"));
            JsonNode opts = root.path("options");
            String answer = safeText(root.path("answer"));

            // Validate presence and structure (options A-D and single-letter answer).
            if (notBlank(question)
                && opts.has("A") && opts.has("B") && opts.has("C") && opts.has("D")
                && answer != null && answer.length() == 1
                && "ABCD".contains(answer)) {

              // Construct entity and persist via JPA.
              McqItem item = new McqItem();
              item.setAll(
                  question,
                  safeText(opts.path("A")),
                  safeText(opts.path("B")),
                  safeText(opts.path("C")),
                  safeText(opts.path("D")),
                  answer
              );

              repo.save(item); // ✅ repo is private but accessible here
              letter = answer; // Single-letter response to return downstream
            }
          }
        }
      }

      // Return single-letter result (default "E" on failure).
      return ResponseEntity.ok()
          .contentType(MediaType.TEXT_PLAIN)
          .header("Connection", "close")
          .body(letter);

    } catch (Exception e) {
      e.printStackTrace();
      // Graceful failure: return "E" but still 200 OK to simplify microcontroller logic.
      return ResponseEntity.ok()
          .contentType(MediaType.TEXT_PLAIN)
          .header("Connection", "close")
          .body("E");
    }
  }

  // --- helpers ---
  private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }

  // Convert a JsonNode to trimmed text; null-safe.
  private static String safeText(JsonNode n) { return n == null ? null : n.asText("").trim(); }
}
