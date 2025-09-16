package iuh.fit.cscore_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "test_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private Question question;
    
    @Column(columnDefinition = "TEXT")
    private String input;
    
    @Column(name = "expected_output", columnDefinition = "TEXT")
    private String expectedOutput;
    
    @Column(name = "is_hidden")
    private Boolean isHidden = false;
    
    @Column(name = "weight")
    private Double weight = 1.0;
    
    @Column(name = "time_limit")
    private Integer timeLimit = 1000; // milliseconds
    
    @Column(name = "memory_limit")
    private Integer memoryLimit = 128; // MB
}
