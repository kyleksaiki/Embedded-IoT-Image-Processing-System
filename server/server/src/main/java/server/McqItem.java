package server;

import jakarta.persistence.*;
import java.time.Instant;

/*------------------------------------------------------------------------------
  Entity: McqItem
  Purpose:
    - Represents a single multiple-choice question (MCQ) extracted from an image.
    - Persisted via JPA/Hibernate to a relational database.

  Table:
    - Name: mcq_item
    - Indexes: created_at (for time-based queries),
               answer (for stats/analytics),
               image_hash (placeholder for future deduplication).

  Notes:
    - All option text is capped at 500 chars, question at 1000 chars.
    - Answer is a single char (A–D).
    - createdAt defaults to current timestamp.
    - Includes a convenience bulk setter (setAll) for populating fields quickly.
------------------------------------------------------------------------------*/
@Entity
@Table(
  name = "mcq_item",
  indexes = {
    @Index(name = "idx_mcq_item_created_at", columnList = "created_at"),
    @Index(name = "idx_mcq_item_answer",     columnList = "answer")
  }
)
public class McqItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment PK
  private Long id;

  @Column(nullable = false, length = 1000)
  private String question;

  @Column(name = "optionA", nullable = false, length = 500)
  private String optionA;

  @Column(name = "optionB", nullable = false, length = 500)
  private String optionB;

  @Column(name = "optionC", nullable = false, length = 500)
  private String optionC;

  @Column(name = "optionD", nullable = false, length = 500)
  private String optionD;

  @Column(nullable = false, columnDefinition = "CHAR(1)")
  private String answer;  // Expected values: "A", "B", "C", or "D"

  // Timestamp of record creation (default: now).
  // Using columnDefinition so DB assigns CURRENT_TIMESTAMP if not set.
  @Column(name = "created_at", nullable = false,
          columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
  private Instant createdAt = Instant.now();

  // ---- Bulk setter ----
  // Convenience method to fill in all fields at once.
  public void setAll(String question, String optionA, String optionB,
                     String optionC, String optionD, String answer) {
    this.question = question;
    this.optionA = optionA;
    this.optionB = optionB;
    this.optionC = optionC;
    this.optionD = optionD;
    this.answer = answer;
    this.createdAt = Instant.now(); // Refresh timestamp when new data arrives
  }
}
