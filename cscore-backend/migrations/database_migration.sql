-- Thêm cột teacher_id vào bảng users
ALTER TABLE users 
ADD COLUMN teacher_id VARCHAR(8) NULL;

-- Tạo index cho teacher_id để tối ưu hóa tìm kiếm
CREATE INDEX idx_users_teacher_id ON users(teacher_id);

-- Đảm bảo teacher_id là duy nhất (không null)
ALTER TABLE users 
ADD CONSTRAINT uk_users_teacher_id UNIQUE (teacher_id);
