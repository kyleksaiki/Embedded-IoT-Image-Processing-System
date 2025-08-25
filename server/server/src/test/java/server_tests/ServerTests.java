package server_tests;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/*------------------------------------------------------------------------------
  Purpose: End-to-end tests for the Spring Boot app.
  Strategy:
    - Spins up the application on a random port (full Spring context).
    - Uses WebTestClient to POST JPEGs sitting on the classpath to /upload.
    - Asserts that the response body is a single expected letter.

------------------------------------------------------------------------------*/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "30000")  // 30 seconds timeout
class ServerTests {

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void configureTimeout() {
        // Ensure each request has a 30-second response timeout
        // (mutates the autoconfigured WebTestClient instance).
        this.webTestClient = this.webTestClient.mutate()
            .responseTimeout(Duration.ofSeconds(30))
            .build();
    }

    // Parameterized test: for each row, load an image and expect a specific letter.
    // The display name will be: "<filename> => <expectedLetter>"
    @ParameterizedTest(name = "{0} => {1}")
    @CsvSource({
        "test_case_1.jpg, A",
        "test_case_2.jpg, B",
        "test_case_3.jpg, C",
        "test_case_4.jpg, D"
    })
    void testUploads(String filename, String expectedLetter) throws Exception {
        // Load the image from test resources (e.g., src/test/resources/test_case_1.jpg).
        ClassPathResource res = new ClassPathResource(filename);
        assertTrue(res.exists(), "Resource not found: " + filename);

        // Read image bytes; also write a copy to the working directory for debugging.
        byte[] img = res.getInputStream().readAllBytes();
        Files.write(Paths.get("debug-" + filename), img);

        // Exercise the /upload endpoint and assert the exact single-letter response.
        webTestClient.post()
            .uri("/upload")
            .contentType(MediaType.IMAGE_JPEG)
            .bodyValue(img)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .isEqualTo(expectedLetter);
    }
}
