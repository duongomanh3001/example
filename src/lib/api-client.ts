const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8086';

class ApiClient {
  private baseURL: string;

  constructor(baseURL: string = API_BASE_URL) {
    this.baseURL = baseURL;
  }

  private getAuthHeaders(): HeadersInit {
    const token = this.getToken();
    return {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` }),
    };
  }

  private getToken(): string | null {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('token');
    }
    return null;
  }

  async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseURL}${endpoint}`;
    
    const config: RequestInit = {
      headers: this.getAuthHeaders(),
      ...options,
    };

    try {
      const response = await fetch(url, config);
      
      if (!response.ok) {
        let errorMessage = '';
        let errorData: any = {};
        
        try {
          errorData = await response.json();
          errorMessage = errorData.message || errorData.error || '';
        } catch (parseError) {
          // Response is not JSON or empty
          console.warn('Could not parse error response as JSON:', parseError);
        }

        // Create a more descriptive error message
        switch (response.status) {
          case 400:
            throw new Error(errorMessage || 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại thông tin đã nhập.');
          case 401:
            throw new Error(errorMessage || 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
          case 403:
            throw new Error(errorMessage || 'Bạn không có quyền thực hiện hành động này.');
          case 404:
            throw new Error(errorMessage || 'Không tìm thấy tài nguyên yêu cầu.');
          case 500:
            throw new Error(errorMessage || 'Lỗi server nội bộ. Vui lòng thử lại sau.');
          case 503:
            throw new Error(errorMessage || 'Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau.');
          default:
            throw new Error(errorMessage || `Lỗi HTTP! Mã trạng thái: ${response.status}`);
        }
      }

      const contentType = response.headers.get('content-type');
      if (contentType && contentType.indexOf('application/json') !== -1) {
        return await response.json();
      }
      
      return {} as T;
    } catch (error) {
      console.error('API request failed:', { url, error });
      
      // Handle network errors
      if (error instanceof TypeError && error.message.includes('fetch')) {
        throw new Error('Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng và thử lại.');
      }
      
      // Re-throw our custom errors or other errors as-is
      throw error;
    }
  }

  async get<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'GET' });
  }

  async post<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async put<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async patch<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PATCH',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'DELETE' });
  }
}

export const apiClient = new ApiClient();
export default apiClient;
