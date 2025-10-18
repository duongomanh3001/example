package iuh.fit.cscore_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionResponse {
    private Long id;
    private Long courseId;
    private String name;
    private String description;
    private Integer orderIndex;
    private Boolean isCollapsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
