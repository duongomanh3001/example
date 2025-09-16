import { apiClient } from '@/lib/api-client';

export class HealthService {
  /**
   * Check if backend is available
   */
  static async checkHealth(): Promise<{ isHealthy: boolean; message: string; details?: any }> {
    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8086';
      
      // Try to access the system health endpoint (no auth required)
      const response = await fetch(`${baseUrl}/api/system/health`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
      });
      
      if (response.ok) {
        return { 
          isHealthy: true, 
          message: 'Backend đang hoạt động bình thường' 
        };
      } else {
        return { 
          isHealthy: false, 
          message: `Backend phản hồi với lỗi: ${response.status}`,
          details: { status: response.status, statusText: response.statusText }
        };
      }
    } catch (error) {
      // If we get a network error, backend is down
      if (error instanceof TypeError && error.message.includes('fetch')) {
        return { 
          isHealthy: false, 
          message: 'Không thể kết nối đến backend. Vui lòng kiểm tra xem server có đang chạy không.',
          details: { error: error.message }
        };
      }
      
      return { 
        isHealthy: false, 
        message: 'Lỗi không xác định khi kiểm tra kết nối backend',
        details: { error: error instanceof Error ? error.message : String(error) }
      };
    }
  }

  /**
   * Quick health check with timeout
   */
  static async quickHealthCheck(timeoutMs: number = 3000): Promise<boolean> {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
      
      const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8086';
      const response = await fetch(`${baseUrl}/api/system/health`, {
        method: 'GET',
        signal: controller.signal,
        headers: { 'Content-Type': 'application/json' },
      });
      
      clearTimeout(timeoutId);
      return response.ok;
    } catch (error) {
      return false;
    }
  }

}

export default HealthService;
