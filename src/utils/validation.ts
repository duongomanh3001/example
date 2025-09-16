/**
 * Validation utilities
 */
export const validationUtils = {
  /**
   * Validate email format
   */
  isValidEmail: (email: string): boolean => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  },

  /**
   * Validate Vietnamese phone number
   */
  isValidPhone: (phone: string): boolean => {
    const phoneRegex = /^(0|\+84)[0-9]{9,10}$/;
    return phoneRegex.test(phone.replace(/\s/g, ''));
  },

  /**
   * Validate username (alphanumeric + underscore)
   */
  isValidUsername: (username: string): boolean => {
    const usernameRegex = /^[a-zA-Z0-9_]{3,20}$/;
    return usernameRegex.test(username);
  },

  /**
   * Validate password strength
   */
  isValidPassword: (password: string): { isValid: boolean; message: string } => {
    if (password.length < 6) {
      return { isValid: false, message: 'Mật khẩu phải có ít nhất 6 ký tự' };
    }
    
    if (password.length > 50) {
      return { isValid: false, message: 'Mật khẩu không được quá 50 ký tự' };
    }

    // Optional: Check for complexity
    const hasUpperCase = /[A-Z]/.test(password);
    const hasLowerCase = /[a-z]/.test(password);
    const hasNumber = /\d/.test(password);
    
    if (password.length >= 8 && (!hasUpperCase || !hasLowerCase || !hasNumber)) {
      return { 
        isValid: true, 
        message: 'Nên có ít nhất 1 chữ hoa, 1 chữ thường và 1 số để bảo mật tốt hơn' 
      };
    }
    
    return { isValid: true, message: 'Mật khẩu hợp lệ' };
  },

  /**
   * Validate URL
   */
  isValidUrl: (url: string): boolean => {
    try {
      new URL(url);
      return true;
    } catch {
      return false;
    }
  },

  /**
   * Check if string is empty or whitespace
   */
  isEmpty: (value: string | null | undefined): boolean => {
    return !value || value.trim().length === 0;
  },

  /**
   * Check if value is numeric
   */
  isNumeric: (value: string): boolean => {
    return !isNaN(parseFloat(value)) && isFinite(Number(value));
  },

  /**
   * Validate file type
   */
  isValidFileType: (fileName: string, allowedTypes: string[]): boolean => {
    const fileExtension = fileName.toLowerCase().split('.').pop();
    return allowedTypes.some(type => type.toLowerCase().replace('.', '') === fileExtension);
  },

  /**
   * Validate file size
   */
  isValidFileSize: (fileSize: number, maxSize: number): boolean => {
    return fileSize <= maxSize;
  },

  /**
   * Sanitize HTML input (basic)
   */
  sanitizeHtml: (input: string): string => {
    const div = document.createElement('div');
    div.textContent = input;
    return div.innerHTML;
  },
};