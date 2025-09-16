/**
 * Date formatting utilities using native JavaScript Date
 */
export const dateUtils = {
  /**
   * Format date to Vietnamese format (DD/MM/YYYY)
   */
  formatDate: (date: string | Date | null): string => {
    if (!date) return '-';
    
    try {
      const parsedDate = typeof date === 'string' ? new Date(date) : date;
      if (isNaN(parsedDate.getTime())) return '-';
      
      const day = parsedDate.getDate().toString().padStart(2, '0');
      const month = (parsedDate.getMonth() + 1).toString().padStart(2, '0');
      const year = parsedDate.getFullYear();
      
      return `${day}/${month}/${year}`;
    } catch (error) {
      return '-';
    }
  },

  /**
   * Format date and time
   */
  formatDateTime: (date: string | Date | null): string => {
    if (!date) return '-';
    
    try {
      const parsedDate = typeof date === 'string' ? new Date(date) : date;
      if (isNaN(parsedDate.getTime())) return '-';
      
      const day = parsedDate.getDate().toString().padStart(2, '0');
      const month = (parsedDate.getMonth() + 1).toString().padStart(2, '0');
      const year = parsedDate.getFullYear();
      const hours = parsedDate.getHours().toString().padStart(2, '0');
      const minutes = parsedDate.getMinutes().toString().padStart(2, '0');
      
      return `${day}/${month}/${year} ${hours}:${minutes}`;
    } catch (error) {
      return '-';
    }
  },

  /**
   * Format time only
   */
  formatTime: (date: string | Date | null): string => {
    if (!date) return '-';
    
    try {
      const parsedDate = typeof date === 'string' ? new Date(date) : date;
      if (isNaN(parsedDate.getTime())) return '-';
      
      const hours = parsedDate.getHours().toString().padStart(2, '0');
      const minutes = parsedDate.getMinutes().toString().padStart(2, '0');
      
      return `${hours}:${minutes}`;
    } catch (error) {
      return '-';
    }
  },

  /**
   * Get relative time (e.g., "2 giờ trước")
   */
  formatRelativeTime: (date: string | Date | null): string => {
    if (!date) return '-';
    
    try {
      const parsedDate = typeof date === 'string' ? new Date(date) : date;
      if (isNaN(parsedDate.getTime())) return '-';
      
      const now = new Date();
      const diffMs = now.getTime() - parsedDate.getTime();
      const diffSeconds = Math.floor(diffMs / 1000);
      const diffMinutes = Math.floor(diffSeconds / 60);
      const diffHours = Math.floor(diffMinutes / 60);
      const diffDays = Math.floor(diffHours / 24);

      if (diffSeconds < 60) return 'Vừa xong';
      if (diffMinutes < 60) return `${diffMinutes} phút trước`;
      if (diffHours < 24) return `${diffHours} giờ trước`;
      if (diffDays < 30) return `${diffDays} ngày trước`;
      
      return dateUtils.formatDate(parsedDate);
    } catch (error) {
      return '-';
    }
  },

  /**
   * Check if date is in the past
   */
  isPastDate: (date: string | Date | null): boolean => {
    if (!date) return false;
    
    try {
      const parsedDate = typeof date === 'string' ? new Date(date) : date;
      if (isNaN(parsedDate.getTime())) return false;
      
      return parsedDate < new Date();
    } catch (error) {
      return false;
    }
  },

  /**
   * Check if date is today
   */
  isToday: (date: string | Date | null): boolean => {
    if (!date) return false;
    
    try {
      const parsedDate = typeof date === 'string' ? new Date(date) : date;
      if (isNaN(parsedDate.getTime())) return false;
      
      const today = new Date();
      return (
        parsedDate.getDate() === today.getDate() &&
        parsedDate.getMonth() === today.getMonth() &&
        parsedDate.getFullYear() === today.getFullYear()
      );
    } catch (error) {
      return false;
    }
  },
};