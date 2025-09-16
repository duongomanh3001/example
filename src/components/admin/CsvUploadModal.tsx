'use client';

import { useState } from 'react';
import { toast } from 'react-hot-toast';
import { APP_CONFIG } from '@/constants/app';

interface CsvUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  type: 'teachers' | 'students' | 'enrollment';
  courseId?: number;
  onSuccess?: () => void;
}

export default function CsvUploadModal({
  isOpen,
  onClose,
  type,
  courseId,
  onSuccess
}: CsvUploadModalProps) {
  const [file, setFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile && selectedFile.type === 'text/csv') {
      setFile(selectedFile);
    } else {
      toast.error('Vui lòng chọn file CSV hợp lệ');
    }
  };

  const downloadTemplate = async () => {
    try {
      const token = localStorage.getItem('token');
      let endpoint = '';
      
      switch (type) {
        case 'teachers':
          endpoint = '/api/admin/csv/template/teachers';
          break;
        case 'students':
          endpoint = '/api/admin/csv/template/students';
          break;
        case 'enrollment':
          endpoint = '/api/admin/csv/template/enrollment';
          break;
      }

      const response = await fetch(`${APP_CONFIG.API_BASE_URL}${endpoint}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.ok) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = url;
        a.download = `${type}_template.csv`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        toast.success('Template đã được tải xuống');
      } else {
        toast.error('Không thể tải template');
      }
    } catch (error) {
      console.error('Error downloading template:', error);
      toast.error('Có lỗi xảy ra khi tải template');
    }
  };

  const handleUpload = async () => {
    if (!file) {
      toast.error('Vui lòng chọn file CSV');
      return;
    }

    setIsUploading(true);
    setUploadProgress(0);

    try {
      const token = localStorage.getItem('token');
      const formData = new FormData();
      formData.append('file', file);

      let endpoint = '';
      switch (type) {
        case 'teachers':
          endpoint = '/api/admin/csv/import-teachers';
          break;
        case 'students':
          endpoint = '/api/admin/csv/import-students';
          break;
        case 'enrollment':
          endpoint = `/api/admin/csv/import-course-enrollment/${courseId}`;
          break;
      }

      // Simulate progress (since we can't track actual upload progress easily)
      const progressInterval = setInterval(() => {
        setUploadProgress(prev => {
          if (prev >= 90) return prev;
          return prev + 10;
        });
      }, 200);

      const response = await fetch(`${APP_CONFIG.API_BASE_URL}${endpoint}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
        body: formData,
      });

      clearInterval(progressInterval);
      setUploadProgress(100);

      if (response.ok) {
        const result = await response.json();
        toast.success(result.message || `Import thành công ${result.count} bản ghi`);
        setFile(null);
        onSuccess?.();
        onClose();
      } else {
        const error = await response.json();
        toast.error(error.message || 'Có lỗi xảy ra khi import');
      }
    } catch (error) {
      console.error('Error uploading CSV:', error);
      toast.error('Có lỗi xảy ra khi upload file');
    } finally {
      setIsUploading(false);
      setUploadProgress(0);
    }
  };

  const getTypeTitle = () => {
    switch (type) {
      case 'teachers':
        return 'Import Giáo Viên';
      case 'students':
        return 'Import Sinh Viên';
      case 'enrollment':
        return 'Import Danh Sách Ghi Danh';
      default:
        return 'Import CSV';
    }
  };

  const getTypeDescription = () => {
    switch (type) {
      case 'teachers':
        return 'Upload file CSV chứa danh sách giáo viên với các cột: username, email, fullName, password';
      case 'students':
        return 'Upload file CSV chứa danh sách sinh viên với các cột: username, email, fullName, studentId, password';
      case 'enrollment':
        return 'Upload file CSV chứa danh sách sinh viên để ghi danh vào khóa học với các cột: username hoặc email';
      default:
        return '';
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg max-w-md w-full p-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-bold text-gray-800">{getTypeTitle()}</h2>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700"
            disabled={isUploading}
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="mb-4">
          <p className="text-sm text-gray-600 mb-4">{getTypeDescription()}</p>
          
          <div className="mb-4">
            <button
              onClick={downloadTemplate}
              className="w-full px-4 py-2 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 transition-colors duration-200 flex items-center justify-center"
              disabled={isUploading}
            >
              <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              tải template csv
            </button>
          </div>

          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Chọn file CSV
            </label>
            <input
              type="file"
              accept=".csv"
              onChange={handleFileChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              disabled={isUploading}
            />
            {file && (
              <p className="text-sm text-green-600 mt-2">
                Đã chọn: {file.name}
              </p>
            )}
          </div>

          {isUploading && (
            <div className="mb-4">
              <div className="flex justify-between text-sm text-gray-600 mb-1">
                <span>Đang upload...</span>
                <span>{uploadProgress}%</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div
                  className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                  style={{ width: `${uploadProgress}%` }}
                ></div>
              </div>
            </div>
          )}
        </div>

        <div className="flex gap-3">
          <button
            onClick={onClose}
            className="flex-1 px-4 py-2 text-gray-600 border border-gray-300 rounded-md hover:bg-gray-50 transition-colors duration-200"
            disabled={isUploading}
          >
            Hủy
          </button>
          <button
            onClick={handleUpload}
            className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
            disabled={!file || isUploading}
          >
            {isUploading ? 'Đang Upload...' : 'Upload'}
          </button>
        </div>
      </div>
    </div>
  );
}
