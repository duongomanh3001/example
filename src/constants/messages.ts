/**
 * User Messages and Notifications
 */
export const MESSAGES = {
  // Success messages
  SUCCESS: {
    LOGIN: 'Đăng nhập thành công!',
    LOGOUT: 'Đăng xuất thành công!',
    SAVE: 'Lưu thành công!',
    UPDATE: 'Cập nhật thành công!',
    DELETE: 'Xóa thành công!',
    CREATE: 'Tạo mới thành công!',
    UPLOAD: 'Tải lên thành công!',
    ENROLL: 'Đăng ký khóa học thành công!',
    SUBMIT: 'Nộp bài thành công!',
  },

  // Error messages
  ERROR: {
    GENERAL: 'Đã xảy ra lỗi, vui lòng thử lại!',
    NETWORK: 'Lỗi mạng, vui lòng kiểm tra kết nối!',
    UNAUTHORIZED: 'Bạn không có quyền truy cập!',
    FORBIDDEN: 'Truy cập bị từ chối!',
    NOT_FOUND: 'Không tìm thấy dữ liệu!',
    VALIDATION: 'Dữ liệu không hợp lệ!',
    LOGIN_FAILED: 'Đăng nhập thất bại, kiểm tra lại thông tin!',
    SESSION_EXPIRED: 'Phiên đăng nhập đã hết hạn!',
    FILE_TOO_LARGE: 'File quá lớn!',
    FILE_TYPE_INVALID: 'Loại file không được hỗ trợ!',
  },

  // Warning messages
  WARNING: {
    UNSAVED_CHANGES: 'Bạn có thay đổi chưa lưu, tiếp tục?',
    DELETE_CONFIRM: 'Bạn có chắc chắn muốn xóa?',
    LOGOUT_CONFIRM: 'Bạn có muốn đăng xuất?',
  },

  // Info messages
  INFO: {
    LOADING: 'Đang tải...',
    NO_DATA: 'Không có dữ liệu',
    EMPTY_STATE: 'Danh sách trống',
    PROCESSING: 'Đang xử lý...',
  },
} as const;

/**
 * Validation Messages
 */
export const VALIDATION_MESSAGES = {
  REQUIRED: 'Trường này là bắt buộc',
  EMAIL_INVALID: 'Email không hợp lệ',
  PASSWORD_TOO_SHORT: 'Mật khẩu phải có ít nhất 6 ký tự',
  PASSWORD_MISMATCH: 'Mật khẩu không khớp',
  USERNAME_INVALID: 'Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới',
  PHONE_INVALID: 'Số điện thoại không hợp lệ',
  DATE_INVALID: 'Ngày không hợp lệ',
  NUMBER_INVALID: 'Số không hợp lệ',
  MAX_LENGTH: (max: number) => `Không được vượt quá ${max} ký tự`,
  MIN_LENGTH: (min: number) => `Phải có ít nhất ${min} ký tự`,
} as const;