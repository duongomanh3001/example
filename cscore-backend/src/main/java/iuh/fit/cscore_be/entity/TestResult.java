package iuh.fit.cscore_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "test_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    @JsonIgnore
    private TestCase testCase;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_submission_id")
    @JsonIgnore
    private QuestionSubmission questionSubmission;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    @JsonIgnore
    private Submission submission;
    
    @Column(name = "actual_output", columnDefinition = "TEXT")
    private String actualOutput;
    
    @Column(name = "is_passed")
    private Boolean isPassed;
    
    @Column(name = "execution_time")
    private Long executionTime; // milliseconds
    
    @Column(name = "memory_used")
    private Long memoryUsed;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
