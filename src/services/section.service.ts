import apiClient from '@/lib/api-client';
import {
  SectionResponse,
  CreateSectionRequest,
  UpdateSectionRequest,
} from '@/types/api';

class SectionServiceClass {
  // Get all sections for a course
  async getSectionsByCourse(courseId: number): Promise<SectionResponse[]> {
    const sections = await apiClient.get<SectionResponse[]>(
      `/api/teacher/courses/${courseId}/sections`
    );
    return sections.sort((a, b) => a.orderIndex - b.orderIndex);
  }

  // Create a new section
  async createSection(data: CreateSectionRequest): Promise<SectionResponse> {
    return apiClient.post<SectionResponse>(
      `/api/teacher/courses/${data.courseId}/sections`,
      data
    );
  }

  // Update a section
  async updateSection(
    sectionId: number,
    data: UpdateSectionRequest
  ): Promise<SectionResponse> {
    return apiClient.put<SectionResponse>(
      `/api/teacher/sections/${sectionId}`,
      data
    );
  }

  // Delete a section
  async deleteSection(sectionId: number): Promise<void> {
    return apiClient.delete(`/api/teacher/sections/${sectionId}`);
  }

  // Toggle section collapse state
  async toggleSectionCollapse(sectionId: number): Promise<SectionResponse> {
    return apiClient.put<SectionResponse>(
      `/api/teacher/sections/${sectionId}/toggle-collapse`
    );
  }

  // Reorder sections
  async reorderSections(
    courseId: number,
    sectionIds: number[]
  ): Promise<SectionResponse[]> {
    return apiClient.put<SectionResponse[]>(
      `/api/teacher/courses/${courseId}/sections/reorder`,
      { sectionIds }
    );
  }
}

export const SectionService = new SectionServiceClass();
