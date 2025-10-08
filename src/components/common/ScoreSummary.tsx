"use client";

import React from 'react';

interface ScoreSummaryProps {
  totalScore: number;
  maxScore: number;
  questionResults?: Array<{
    questionId: number;
    questionTitle: string;
    earnedScore: number;
    maxScore: number;
    status: 'CORRECT' | 'INCORRECT' | 'PARTIAL' | 'NOT_ANSWERED';
  }>;
}

function ScoreSummary({ totalScore, maxScore, questionResults }: ScoreSummaryProps) {
  const percentage = maxScore > 0 ? (totalScore / maxScore) * 100 : 0;
  
  const getGradeColor = (percentage: number) => {
    if (percentage >= 80) return 'text-green-600 bg-green-50 border-green-200';
    if (percentage >= 70) return 'text-blue-600 bg-blue-50 border-blue-200';
    if (percentage >= 60) return 'text-yellow-600 bg-yellow-50 border-yellow-200';
    return 'text-red-600 bg-red-50 border-red-200';
  };

  const getGradeText = (percentage: number) => {
    if (percentage >= 80) return 'Xuất sắc';
    if (percentage >= 70) return 'Tốt';
    if (percentage >= 60) return 'Khá';
    if (percentage >= 50) return 'Trung bình';
    return 'Yếu';
  };

  const getScaleScore = (percentage: number) => {
    // Convert percentage to 10-point scale
    return (percentage / 10).toFixed(1);
  };

  const correctQuestions = questionResults?.filter(q => q.status === 'CORRECT').length || 0;
  const totalQuestions = questionResults?.length || 0;

  return (
    <div className="space-y-4">
      {/* Main Score Display */}
      <div className={`rounded-lg border-2 p-6 ${getGradeColor(percentage)}`}>
        <div className="text-center">
          <div className="text-4xl font-bold mb-2">
            {getScaleScore(percentage)}/10
          </div>
          <div className="text-2xl font-semibold mb-1">
            {totalScore.toFixed(1)}/{maxScore} điểm ({percentage.toFixed(1)}%)
          </div>
          <div className="text-lg font-medium">
            {getGradeText(percentage)}
          </div>
        </div>
      </div>

      {/* Question Summary */}
      {questionResults && questionResults.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-green-50 border border-green-200 rounded-lg p-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-green-600">
                {correctQuestions}
              </div>
              <div className="text-green-700 font-medium">Câu đúng</div>
            </div>
          </div>
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-red-600">
                {totalQuestions - correctQuestions}
              </div>
              <div className="text-red-700 font-medium">Câu sai</div>
            </div>
          </div>
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-blue-600">
                {totalQuestions}
              </div>
              <div className="text-blue-700 font-medium">Tổng câu</div>
            </div>
          </div>
        </div>
      )}

      {/* Detailed Breakdown */}
      {questionResults && questionResults.length > 0 && (
        <div className="bg-white border rounded-lg p-4">
          <h3 className="text-lg font-semibold mb-4">Chi tiết điểm từng câu</h3>
          <div className="space-y-3">
            {questionResults.map((question, index) => {
              const qPercentage = question.maxScore > 0 ? (question.earnedScore / question.maxScore) * 100 : 0;
              const statusColor = question.status === 'CORRECT' ? 'text-green-600' :
                                question.status === 'PARTIAL' ? 'text-yellow-600' :
                                question.status === 'INCORRECT' ? 'text-red-600' : 'text-gray-600';
              
              return (
                <div key={question.questionId} className="flex items-center justify-between p-3 bg-gray-50 rounded">
                  <div className="flex-1">
                    <div className="font-medium">Câu {index + 1}: {question.questionTitle}</div>
                    <div className="text-sm text-gray-600">
                      {question.earnedScore.toFixed(1)}/{question.maxScore} điểm ({qPercentage.toFixed(1)}%)
                    </div>
                  </div>
                  <div className={`px-3 py-1 rounded-full text-sm font-medium ${statusColor}`}>
                    {question.status === 'CORRECT' ? '✓ Đúng' :
                     question.status === 'PARTIAL' ? '△ Đúng một phần' :
                     question.status === 'INCORRECT' ? '✗ Sai' : '- Chưa trả lời'}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Performance Analysis */}
      <div className="bg-gray-50 rounded-lg p-4">
        <h3 className="text-lg font-semibold mb-4">Phân tích kết quả</h3>
        <div className="space-y-2 text-sm">
          {percentage >= 80 && (
            <div className="text-green-700">
              🎉 Xuất sắc! Bạn đã nắm vững kiến thức và thực hiện tốt bài tập.
            </div>
          )}
          {percentage >= 70 && percentage < 80 && (
            <div className="text-blue-700">
              👍 Tốt! Bạn đã hiểu được phần lớn nội dung, cần cải thiện một số điểm nhỏ.
            </div>
          )}
          {percentage >= 60 && percentage < 70 && (
            <div className="text-yellow-700">
              📝 Khá! Bạn đã làm được một số câu, hãy ôn tập thêm để cải thiện.
            </div>
          )}
          {percentage < 60 && (
            <div className="text-red-700">
              📚 Cần cải thiện! Hãy xem lại lý thuyết và thực hành thêm.
            </div>
          )}
          
          {questionResults && (
            <div className="mt-3 p-3 bg-white rounded border">
              <div className="font-medium text-gray-800">Gợi ý học tập:</div>
              <ul className="mt-1 space-y-1 text-gray-700">
                {questionResults.filter(q => q.status === 'INCORRECT').length > 0 && (
                  <li>• Xem lại các câu sai để hiểu rõ lỗi</li>
                )}
                {correctQuestions < totalQuestions && (
                  <li>• Thực hành thêm với các bài tập tương tự</li>
                )}
                <li>• Tham khảo tài liệu học tập và ví dụ</li>
                <li>• Hỏi thầy cô khi gặp khó khăn</li>
              </ul>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default ScoreSummary;