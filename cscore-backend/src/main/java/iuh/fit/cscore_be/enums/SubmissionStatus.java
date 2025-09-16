package iuh.fit.cscore_be.enums;

public enum SubmissionStatus {
    NOT_SUBMITTED,      // Chưa nộp
    SUBMITTED,          // Đã nộp, chờ chấm điểm
    GRADING,            // Đang trong quá trình chấm điểm tự động
    GRADED,             // Đã chấm điểm thành công
    PASSED,             // Đạt yêu cầu (điểm >= 80%)
    PARTIAL,            // Đạt một phần (50% <= điểm < 80%)
    FAILED,             // Không đạt yêu cầu (điểm < 50%)
    COMPILATION_ERROR,  // Lỗi biên dịch
    ERROR,              // Lỗi hệ thống
    NO_TESTS,           // Không có test case để chấm
    LATE                // Nộp muộn
}
