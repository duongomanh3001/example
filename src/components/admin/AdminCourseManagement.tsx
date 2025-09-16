"use client";

import { useState, useEffect } from 'react';
import { CourseService } from '@/services/course.service';
import { UserService } from '@/services/user.service';
import CsvUploadModal from './CsvUploadModal';
import { 
  CourseResponse, 
  DetailedCourseResponse, 
  CreateCourseRequest, 
  UpdateCourseRequest,
  StudentResponse 
} from '@/types/api';
import { User, Role } from '@/types/auth';

interface AdminCourseManagementProps {
  initialCourses?: CourseResponse[];
}

export default function AdminCourseManagement({ initialCourses = [] }: AdminCourseManagementProps) {
  const [courses, setCourses] = useState<CourseResponse[]>(initialCourses);
  const [teachers, setTeachers] = useState<User[]>([]);
  const [students, setStudents] = useState<User[]>([]);
  const [selectedCourse, setSelectedCourse] = useState<DetailedCourseResponse | null>(null);
  const [courseStudents, setCourseStudents] = useState<StudentResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  // Modal states
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showStudentsModal, setShowStudentsModal] = useState(false);
  const [showAddStudentModal, setShowAddStudentModal] = useState(false);
  const [showCsvModal, setShowCsvModal] = useState(false);

  // Form data
  const [createForm, setCreateForm] = useState<CreateCourseRequest>({
    name: '',
    code: '',
    description: '',
    creditHours: 3,
    semester: '1',
    year: new Date().getFullYear(),
    maxStudents: 50,
    teacherId: 0
  });

  const [editForm, setEditForm] = useState<UpdateCourseRequest>({});

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setIsLoading(true);
      const [coursesData, teachersData, studentsData] = await Promise.all([
        CourseService.getAllCourses(),
        UserService.getAllTeachers(),
        UserService.getAllStudents()
      ]);
      setCourses(coursesData);
      setTeachers(teachersData);
      setStudents(studentsData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi tải dữ liệu');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateCourse = async (e: React.FormEvent) => {
    e.preventDefault();
    if (createForm.teacherId === 0) {
      setError('Vui lòng chọn giáo viên');
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      
      const newCourse = await CourseService.createCourse(createForm);
      await loadData(); // Refresh the courses list
      setShowCreateModal(false);
      setCreateForm({
        name: '',
        code: '',
        description: '',
        creditHours: 3,
        semester: '1',
        year: new Date().getFullYear(),
        maxStudents: 50,
        teacherId: 0
      });
      setSuccess('Khóa học đã được tạo thành công');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi tạo khóa học');
    } finally {
      setIsLoading(false);
    }
  };

  const handleEditCourse = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedCourse) return;

    try {
      setIsLoading(true);
      setError(null);
      
      await CourseService.updateCourse(selectedCourse.id, editForm);
      await loadData();
      setShowEditModal(false);
      setSelectedCourse(null);
      setEditForm({});
      setSuccess('Khóa học đã được cập nhật thành công');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi cập nhật khóa học');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteCourse = async (courseId: number) => {
    if (!confirm('Bạn có chắc chắn muốn vô hiệu hóa khóa học này?')) return;

    try {
      setIsLoading(true);
      setError(null);
      
      await CourseService.deleteCourse(courseId);
      await loadData();
      setSuccess('Khóa học đã được vô hiệu hóa thành công');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi vô hiệu hóa khóa học');
    } finally {
      setIsLoading(false);
    }
  };

  const handleViewStudents = async (course: CourseResponse) => {
    try {
      setIsLoading(true);
      const [courseDetails, studentsInCourse] = await Promise.all([
        CourseService.getCourseById(course.id),
        CourseService.getStudentsInCourse(course.id)
      ]);
      setSelectedCourse(courseDetails);
      setCourseStudents(studentsInCourse);
      setShowStudentsModal(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi tải danh sách sinh viên');
    } finally {
      setIsLoading(false);
    }
  };

  const reloadCourseStudents = async (courseId: number) => {
    try {
      const studentsInCourse = await CourseService.getStudentsInCourse(courseId);
      setCourseStudents(studentsInCourse);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi tải danh sách sinh viên');
    }
  };

  const handleAddStudent = async (studentId: number) => {
    if (!selectedCourse) return;

    try {
      setIsLoading(true);
      setError(null);
      
      await CourseService.enrollStudentToCourse(selectedCourse.id, studentId);
      // Refresh student list
      const updatedStudents = await CourseService.getStudentsInCourse(selectedCourse.id);
      setCourseStudents(updatedStudents);
      setShowAddStudentModal(false);
      setSuccess('Sinh viên đã được thêm vào khóa học thành công');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi thêm sinh viên');
    } finally {
      setIsLoading(false);
    }
  };

  const handleRemoveStudent = async (studentId: number) => {
    if (!selectedCourse) return;
    if (!confirm('Bạn có chắc chắn muốn xóa sinh viên này khỏi khóa học?')) return;

    try {
      setIsLoading(true);
      setError(null);
      
      await CourseService.removeStudentFromCourse(selectedCourse.id, studentId);
      // Refresh student list
      const updatedStudents = await CourseService.getStudentsInCourse(selectedCourse.id);
      setCourseStudents(updatedStudents);
      setSuccess('Sinh viên đã được xóa khỏi khóa học thành công');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi xóa sinh viên');
    } finally {
      setIsLoading(false);
    }
  };

  const openEditModal = async (course: CourseResponse) => {
    try {
      setIsLoading(true);
      const courseDetails = await CourseService.getCourseById(course.id);
      setSelectedCourse(courseDetails);
      setEditForm({
        name: courseDetails.name,
        description: courseDetails.description,
        creditHours: courseDetails.creditHours,
        semester: courseDetails.semester,
        year: courseDetails.year,
        maxStudents: courseDetails.maxStudents,
        teacherId: courseDetails.teacher.id,
        isActive: courseDetails.isActive
      });
      setShowEditModal(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra khi tải chi tiết khóa học');
    } finally {
      setIsLoading(false);
    }
  };

  // Get available students for adding to course
  const getAvailableStudents = () => {
    const enrolledStudentIds = courseStudents.map(s => s.id);
    return students.filter(student => !enrolledStudentIds.includes(student.id));
  };

  if (isLoading && courses.length === 0) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-slate-900">Quản lý Khóa học</h2>
        <button
          onClick={() => setShowCreateModal(true)}
          className="bg-emerald-600 text-white px-4 py-2 rounded-md hover:bg-emerald-700"
        >
          Tạo khóa học mới
        </button>
      </div>

      {/* Success/Error Messages */}
      {success && (
        <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-md">
          {success}
          <button onClick={() => setSuccess(null)} className="ml-4 text-green-900">×</button>
        </div>
      )}
      
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
          {error}
          <button onClick={() => setError(null)} className="ml-4 text-red-900">×</button>
        </div>
      )}

      {/* Courses Grid */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {courses.map((course) => (
          <div key={course.id} className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm">
            <div className="flex justify-between items-start mb-3">
              <div className="w-12 h-12 bg-emerald-100 rounded-lg flex items-center justify-center">
                <span className="text-emerald-600 font-semibold text-sm">
                  {course.code.substring(0, 2).toUpperCase()}
                </span>
              </div>
              <div className="flex items-center gap-2">
                <span className={`text-xs px-2 py-1 rounded-full ${
                  course.isActive 
                    ? 'bg-green-100 text-green-700' 
                    : 'bg-red-100 text-red-700'
                }`}>
                  {course.isActive ? 'Hoạt động' : 'Vô hiệu hóa'}
                </span>
              </div>
            </div>
            
            <h3 className="font-semibold text-slate-900 mb-1">{course.name}</h3>
            <p className="text-sm text-slate-500 mb-2">Mã: {course.code}</p>
            <p className="text-xs text-slate-400 mb-2">{course.semester}/{course.year}</p>
            
            <div className="text-xs text-slate-500 mb-3">
              Sinh viên: {course.currentStudentCount}/{course.maxStudents}
            </div>
            
            {course.teacher && (
              <div className="text-xs text-slate-500 mb-3">
                GV: {course.teacher.fullName}
              </div>
            )}
            
            <div className="flex flex-wrap gap-2">
              <button
                onClick={() => openEditModal(course)}
                className="text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded hover:bg-blue-200"
              >
                Sửa
              </button>
              <button
                onClick={() => handleViewStudents(course)}
                className="text-xs bg-purple-100 text-purple-700 px-2 py-1 rounded hover:bg-purple-200"
              >
                Sinh viên
              </button>
              <button
                onClick={() => handleDeleteCourse(course.id)}
                className="text-xs bg-red-100 text-red-700 px-2 py-1 rounded hover:bg-red-200"
                disabled={isLoading}
              >
                Vô hiệu hóa
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Create Course Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <h3 className="text-lg font-semibold mb-4">Tạo khóa học mới</h3>
            <form onSubmit={handleCreateCourse} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Tên khóa học *
                  </label>
                  <input
                    type="text"
                    required
                    value={createForm.name}
                    onChange={(e) => setCreateForm({...createForm, name: e.target.value})}
                    className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    placeholder="Nhập tên khóa học"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Mã khóa học *
                  </label>
                  <input
                    type="text"
                    required
                    value={createForm.code}
                    onChange={(e) => setCreateForm({...createForm, code: e.target.value.toUpperCase()})}
                    className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    placeholder="VD: CS01, ABC101, MATH1001"
                  />
                </div>
              </div>
              
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Mô tả
                </label>
                <textarea
                  value={createForm.description}
                  onChange={(e) => setCreateForm({...createForm, description: e.target.value})}
                  className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  rows={3}
                  placeholder="Mô tả khóa học"
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Số tín chỉ *
                  </label>
                  <input
                    type="number"
                    required
                    min="1"
                    max="10"
                    value={createForm.creditHours}
                    onChange={(e) => setCreateForm({...createForm, creditHours: parseInt(e.target.value)})}
                    className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Học kỳ *
                  </label>
                  <select
                    required
                    value={createForm.semester}
                    onChange={(e) => setCreateForm({...createForm, semester: e.target.value})}
                    className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  >
                    <option value="1">Học kỳ 1</option>
                    <option value="2">Học kỳ 2</option>
                    <option value="3">Học kỳ hè</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Năm học *
                  </label>
                  <input
                    type="number"
                    required
                    min="2020"
                    max="2030"
                    value={createForm.year}
                    onChange={(e) => setCreateForm({...createForm, year: parseInt(e.target.value)})}
                    className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Số lượng sinh viên tối đa *
                  </label>
                  <input
                    type="number"
                    required
                    min="5"
                    max="200"
                    value={createForm.maxStudents}
                    onChange={(e) => setCreateForm({...createForm, maxStudents: parseInt(e.target.value)})}
                    className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Giáo viên *
                  </label>
                  <select
                    required
                    value={createForm.teacherId}
                    onChange={(e) => setCreateForm({...createForm, teacherId: parseInt(e.target.value)})}
                    className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  >
                    <option value={0}>Chọn giáo viên</option>
                    {teachers.map((teacher) => (
                      <option key={teacher.id} value={teacher.id}>
                        {teacher.fullName} ({teacher.username})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:text-slate-800"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="px-4 py-2 bg-emerald-600 text-white text-sm rounded-md hover:bg-emerald-700 disabled:opacity-50"
                >
                  {isLoading ? 'Đang tạo...' : 'Tạo khóa học'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Students Modal */}
      {showStudentsModal && selectedCourse && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-4xl mx-4 max-h-[90vh] overflow-auto">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-semibold text-slate-900">
                Sinh viên khóa học: {selectedCourse.name}
              </h3>
              <button
                onClick={() => setShowStudentsModal(false)}
                className="text-slate-400 hover:text-slate-600"
              >
                ✕
              </button>
            </div>

            <div className="flex justify-between items-center mb-4">
              <p className="text-slate-600">
                Tổng: {courseStudents.length} sinh viên
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setShowCsvModal(true)}
                  className="px-4 py-2 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700 flex items-center gap-2"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                  </svg>
                  Import CSV
                </button>
                <button
                  onClick={() => setShowAddStudentModal(true)}
                  className="px-4 py-2 bg-emerald-600 text-white text-sm rounded-md hover:bg-emerald-700"
                >
                  Thêm sinh viên
                </button>
              </div>
            </div>

            {courseStudents.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full border border-slate-200">
                  <thead>
                    <tr className="bg-slate-50">
                      <th className="border border-slate-200 px-3 py-2 text-left text-sm font-medium text-slate-600">
                        Họ tên
                      </th>
                      <th className="border border-slate-200 px-3 py-2 text-left text-sm font-medium text-slate-600">
                        Username
                      </th>
                      <th className="border border-slate-200 px-3 py-2 text-left text-sm font-medium text-slate-600">
                        Email
                      </th>
                      <th className="border border-slate-200 px-3 py-2 text-left text-sm font-medium text-slate-600">
                        Ngày đăng ký
                      </th>
                      <th className="border border-slate-200 px-3 py-2 text-center text-sm font-medium text-slate-600">
                        Thao tác
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {courseStudents.map((student) => (
                      <tr key={student.id} className="hover:bg-slate-50">
                        <td className="border border-slate-200 px-3 py-2 text-sm">
                          {student.fullName}
                        </td>
                        <td className="border border-slate-200 px-3 py-2 text-sm">
                          {student.username}
                        </td>
                        <td className="border border-slate-200 px-3 py-2 text-sm">
                          {student.email}
                        </td>
                        <td className="border border-slate-200 px-3 py-2 text-sm">
                          {student.enrolledAt || 'N/A'}
                        </td>
                        <td className="border border-slate-200 px-3 py-2 text-center">
                          <button
                            onClick={() => handleRemoveStudent(student.id)}
                            className="text-red-600 hover:text-red-800 text-sm"
                          >
                            Xóa
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="text-slate-500 text-center py-8">
                Chưa có sinh viên nào trong khóa học này
              </p>
            )}
          </div>
        </div>
      )}

      {/* Add Student Modal */}
      {showAddStudentModal && selectedCourse && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-lg mx-4">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-semibold text-slate-900">
                Thêm sinh viên vào khóa học
              </h3>
              <button
                onClick={() => setShowAddStudentModal(false)}
                className="text-slate-400 hover:text-slate-600"
              >
                ✕
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  Chọn sinh viên
                </label>
                <select
                  onChange={(e) => {
                    const studentId = parseInt(e.target.value);
                    if (studentId > 0) {
                      handleAddStudent(studentId);
                    }
                  }}
                  className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                >
                  <option value={0}>Chọn sinh viên</option>
                  {students
                    .filter(student => !courseStudents.find(cs => cs.id === student.id))
                    .map((student) => (
                      <option key={student.id} value={student.id}>
                        {student.fullName} ({student.username})
                      </option>
                    ))}
                </select>
              </div>

              {students.filter(student => !courseStudents.find(cs => cs.id === student.id)).length === 0 && (
                <p className="text-slate-500 text-sm">
                  Tất cả sinh viên đã được thêm vào khóa học này
                </p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* CSV Upload Modal */}
      <CsvUploadModal
        isOpen={showCsvModal}
        onClose={() => setShowCsvModal(false)}
        type="enrollment"
        courseId={selectedCourse?.id}
        onSuccess={() => {
          reloadCourseStudents(selectedCourse!.id);
        }}
      />

      {/* Similar modals for Edit would go here... */}
    </div>
  );
}
