package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.dto.request.CreateSectionRequest;
import iuh.fit.cscore_be.dto.request.UpdateSectionRequest;
import iuh.fit.cscore_be.dto.response.SectionResponse;
import iuh.fit.cscore_be.entity.Course;
import iuh.fit.cscore_be.entity.Section;
import iuh.fit.cscore_be.exception.ResourceNotFoundException;
import iuh.fit.cscore_be.repository.CourseRepository;
import iuh.fit.cscore_be.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SectionService {
    
    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    
    @Transactional(readOnly = true)
    public List<SectionResponse> getSectionsByCourse(Long courseId) {
        List<Section> sections = sectionRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        return sections.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public SectionResponse createSection(CreateSectionRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + request.getCourseId()));
        
        Section section = new Section();
        section.setCourse(course);
        section.setName(request.getName());
        section.setDescription(request.getDescription());
        section.setOrderIndex(request.getOrderIndex());
        section.setIsCollapsed(false);
        
        Section savedSection = sectionRepository.save(section);
        return convertToResponse(savedSection);
    }
    
    @Transactional
    public SectionResponse updateSection(Long sectionId, UpdateSectionRequest request) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân mục với ID: " + sectionId));
        
        if (request.getName() != null) {
            section.setName(request.getName());
        }
        if (request.getDescription() != null) {
            section.setDescription(request.getDescription());
        }
        if (request.getOrderIndex() != null) {
            section.setOrderIndex(request.getOrderIndex());
        }
        if (request.getIsCollapsed() != null) {
            section.setIsCollapsed(request.getIsCollapsed());
        }
        
        Section updatedSection = sectionRepository.save(section);
        return convertToResponse(updatedSection);
    }
    
    @Transactional
    public void deleteSection(Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân mục với ID: " + sectionId));
        
        // Remove section reference from assignments
        section.getAssignments().forEach(assignment -> assignment.setSection(null));
        
        sectionRepository.delete(section);
    }
    
    // Note: toggleSectionCollapse method removed - collapse state handled on frontend only
    
    @Transactional
    public List<SectionResponse> reorderSections(Long courseId, List<Long> sectionIds) {
        for (int i = 0; i < sectionIds.size(); i++) {
            Long sectionId = sectionIds.get(i);
            Section section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân mục với ID: " + sectionId));
            section.setOrderIndex(i);
            sectionRepository.save(section);
        }
        
        return getSectionsByCourse(courseId);
    }
    
    private SectionResponse convertToResponse(Section section) {
        return SectionResponse.builder()
                .id(section.getId())
                .courseId(section.getCourse().getId())
                .name(section.getName())
                .description(section.getDescription())
                .orderIndex(section.getOrderIndex())
                .isCollapsed(section.getIsCollapsed())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .build();
    }
}
