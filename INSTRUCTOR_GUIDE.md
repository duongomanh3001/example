# Hướng dẫn sử dụng hệ thống Auto-Grading đã cải tiến

## ✅ Kết quả demo: Hệ thống đã hoạt động đúng!

Với ví dụ `countCharacter` mà bạn đưa ra, hệ thống **đã có thể chấm điểm chính xác** sau khi được cải tiến.

## 📋 Cách thiết lập để sinh viên test PASS

### Bước 1: Tạo Question với Reference Implementation

Trong giao diện tạo câu hỏi, điền:

**Question Details:**
- **Title**: "Count Character Function"
- **Description**: "Viết hàm đếm số lần xuất hiện của ký tự trong chuỗi"
- **Points**: 10 (hoặc điểm tùy ý)

**Function Metadata:**
- **Function Name**: `countCharacter`
- **Function Signature**: `int countCharacter(const char str[], char key)`
- **Programming Language**: `c`

**Reference Implementation** (Đáp án của giảng viên):
```c
int countCharacter(const char str[], char key)
{
	int count = 0;
	for (int i = 0; str[i] != '\0'; i++)
	{
		if(str[i] == key)
			count++;
	}
	return count;
}
```

### Bước 2: Tạo Test Cases

Tạo 3 test cases với format sau:

**Test Case 1:**
- **Input**: `Hello l`
- **Expected Output**: `2`
- **Weight**: 1.0

**Test Case 2:**
- **Input**: `HelloHelloHello o`  
- **Expected Output**: `3`
- **Weight**: 1.0

**Test Case 3:**
- **Input**: `"" o`
- **Expected Output**: `0`
- **Weight**: 1.0

⚠️ **Lưu ý quan trọng**: 
- Input format: `"string_value character_value"`
- Không cần có dấu ngoặc kép bao quanh toàn bộ input
- Cho empty string, dùng `"" o` (không phải `"\"\" o"`)

### Bước 3: Kích hoạt Auto-Grade

- ✅ Bật "Auto Grade" cho assignment
- ✅ Đảm bảo assignment có Reference Implementation

## 🎯 Kết quả mong đợi

Khi sinh viên submit code giống hệt với reference implementation:

```c
int countCharacter(const char str[], char key)
{
	int count = 0;
	for (int i = 0; str[i] != '\0'; i++)
	{
		if(str[i] == key)
			count++;
	}
	return count;
}
```

**Kết quả sẽ là:**
```
✅ Biên dịch chương trình thành công
✅ Chấm điểm tự động: Code của bạn được chạy với 3 test case(s) từ giảng viên. 
   Kết quả: 3/3 test case(s) pass. 

🎉 Hoàn thành! 3/3 test cases đạt yêu cầu

Chi tiết test cases:
Test              Expected    Got
Hello l           2           2      ✅
HelloHelloHello o 3           3      ✅  
"" o              0           0      ✅

Điểm số: 100/100
```

## 🔧 Điều chỉnh nếu cần

### Nếu vẫn gặp vấn đề "no output":

1. **Kiểm tra Function Metadata**: 
   - Function name phải chính xác
   - Function signature phải đúng format
   - Programming language phải đặt là `c`

2. **Kiểm tra Test Case Input Format**:
   - Đúng: `Hello l`
   - Sai: `"Hello" 'l'` hoặc `Hello, l`

3. **Đảm bảo có Reference Implementation**:
   - Hệ thống chỉ dùng enhanced grading khi có reference implementation
   - Nếu không có, sẽ fallback về traditional grading (có thể gây "no output")

### Nếu muốn test với function khác:

**Ví dụ: `int sum(int a, int b)`**
- **Input format**: `5 3` (số cách nhau bởi space)
- **Function signature**: `int sum(int a, int b)`
- **Test case input**: `5 3` → expected `8`

## 🚀 Ưu điểm của hệ thống cải tiến

1. **Accurate Testing**: So sánh trực tiếp với reference implementation thay vì dựa vào expected output có sẵn
2. **Better Error Handling**: Parse input thông minh hơn, xử lý được nhiều edge case
3. **Detailed Feedback**: Sinh viên thấy được output thực tế vs expected
4. **Language Support**: Hỗ trợ C, C++, Java, Python
5. **Automatic Selection**: Tự động chọn enhanced grading khi có reference implementation

## 📞 Khi cần hỗ trợ

Nếu vẫn gặp vấn đề, kiểm tra:
1. ✅ Function metadata đã đúng chưa
2. ✅ Test case input format đã đúng chưa  
3. ✅ Reference implementation có compile được không
4. ✅ Auto-grade đã được bật chưa

Hệ thống hiện tại **đã có thể chấm điểm chính xác** cho loại bài tập function-based như `countCharacter`!