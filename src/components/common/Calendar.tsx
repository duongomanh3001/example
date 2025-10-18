"use client";

import { useState, useMemo } from 'react';
import { StudentAssignmentResponse } from '@/types/api';

interface CalendarProps {
  assignments: StudentAssignmentResponse[];
  onDateClick?: (date: Date, assignments: StudentAssignmentResponse[]) => void;
}

interface CalendarDay {
  date: Date;
  isCurrentMonth: boolean;
  isToday: boolean;
  assignments: StudentAssignmentResponse[];
}

export default function Calendar({ assignments, onDateClick }: CalendarProps) {
  const [currentDate, setCurrentDate] = useState(new Date());

  const monthNames = [
    'Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6',
    'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'
  ];

  const dayNames = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];

  // Get calendar days for current month
  const calendarDays = useMemo(() => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    
    // First day of the month
    const firstDay = new Date(year, month, 1);
    // Last day of the month
    const lastDay = new Date(year, month + 1, 0);
    
    // Start from Sunday of the week containing the first day
    const startDate = new Date(firstDay);
    startDate.setDate(firstDay.getDate() - firstDay.getDay());
    
    // End on Saturday of the week containing the last day
    const endDate = new Date(lastDay);
    endDate.setDate(lastDay.getDate() + (6 - lastDay.getDay()));
    
    const days: CalendarDay[] = [];
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    // Generate all days in the calendar
    const currentDateIter = new Date(startDate);
    while (currentDateIter <= endDate) {
      const dateStr = currentDateIter.toISOString().split('T')[0];
      
      // Filter assignments for this day
      const dayAssignments = assignments.filter(assignment => {
        if (!assignment.endTime) return false;
        const assignmentDate = new Date(assignment.endTime);
        return assignmentDate.toISOString().split('T')[0] === dateStr;
      });
      
      days.push({
        date: new Date(currentDateIter),
        isCurrentMonth: currentDateIter.getMonth() === month,
        isToday: currentDateIter.getTime() === today.getTime(),
        assignments: dayAssignments
      });
      
      currentDateIter.setDate(currentDateIter.getDate() + 1);
    }
    
    return days;
  }, [currentDate, assignments]);

  const goToPreviousMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1));
  };

  const goToNextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1));
  };

  const goToToday = () => {
    setCurrentDate(new Date());
  };

  const getStatusColor = (assignment: StudentAssignmentResponse) => {
    if (assignment.isSubmitted) {
      return 'bg-green-500';
    }
    
    if (assignment.endTime) {
      const now = new Date();
      const deadline = new Date(assignment.endTime);
      const daysUntilDeadline = Math.floor((deadline.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
      
      if (daysUntilDeadline < 0) {
        return 'bg-gray-400'; // Quá hạn
      } else if (daysUntilDeadline <= 2) {
        return 'bg-red-500'; // Sắp đến hạn
      } else {
        return 'bg-orange-500'; // Đang tiến hành
      }
    }
    
    return 'bg-orange-500';
  };

  const getStatusText = (assignment: StudentAssignmentResponse) => {
    if (assignment.isSubmitted) {
      return 'Đã nộp';
    }
    
    if (assignment.endTime) {
      const now = new Date();
      const deadline = new Date(assignment.endTime);
      const daysUntilDeadline = Math.floor((deadline.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
      
      if (daysUntilDeadline < 0) {
        return 'Quá hạn';
      } else if (daysUntilDeadline === 0) {
        return 'Hôm nay';
      } else if (daysUntilDeadline === 1) {
        return 'Ngày mai';
      } else {
        return `${daysUntilDeadline} ngày nữa`;
      }
    }
    
    return 'Đang tiến hành';
  };

  const handleDayClick = (day: CalendarDay) => {
    if (day.assignments.length > 0 && onDateClick) {
      onDateClick(day.date, day.assignments);
    }
  };

  return (
    <div className="bg-white rounded-lg border border-slate-200 p-4">
      {/* Calendar Header */}
      <div className="flex items-center justify-between mb-4">
        <button
          onClick={goToPreviousMonth}
          className="p-2 hover:bg-slate-100 rounded-md transition-colors"
          title="Tháng trước"
        >
          <svg className="w-5 h-5 text-slate-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        
        <div className="flex items-center gap-3">
          <h3 className="text-lg font-semibold text-slate-900">
            {monthNames[currentDate.getMonth()]} {currentDate.getFullYear()}
          </h3>
          <button
            onClick={goToToday}
            className="px-3 py-1 text-sm text-slate-600 hover:bg-slate-100 rounded-md transition-colors"
          >
            Hôm nay
          </button>
        </div>
        
        <button
          onClick={goToNextMonth}
          className="p-2 hover:bg-slate-100 rounded-md transition-colors"
          title="Tháng sau"
        >
          <svg className="w-5 h-5 text-slate-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </button>
      </div>

      {/* Day Names */}
      <div className="grid grid-cols-7 gap-1 mb-2">
        {dayNames.map(day => (
          <div key={day} className="text-center text-xs font-medium text-slate-600 py-2">
            {day}
          </div>
        ))}
      </div>

      {/* Calendar Grid */}
      <div className="grid grid-cols-7 gap-1">
        {calendarDays.map((day, index) => (
          <div
            key={index}
            onClick={() => handleDayClick(day)}
            className={`
              min-h-[80px] p-2 border border-slate-100 rounded-md
              ${!day.isCurrentMonth ? 'bg-slate-50 opacity-50' : 'bg-white'}
              ${day.isToday ? 'ring-2 ring-blue-500' : ''}
              ${day.assignments.length > 0 ? 'cursor-pointer hover:bg-slate-50' : ''}
              transition-all
            `}
          >
            {/* Date Number */}
            <div className={`
              text-sm font-medium mb-1 text-center
              ${day.isToday ? 'text-blue-600 font-bold' : day.isCurrentMonth ? 'text-slate-700' : 'text-slate-400'}
            `}>
              {day.date.getDate()}
            </div>

            {/* Assignment Indicators */}
            <div className="space-y-1">
              {day.assignments.slice(0, 2).map((assignment) => (
                <div
                  key={assignment.id}
                  className={`
                    ${getStatusColor(assignment)} 
                    text-white text-[10px] px-1.5 py-0.5 rounded truncate
                    hover:opacity-80 transition-opacity
                  `}
                  title={`${assignment.title} - ${getStatusText(assignment)}`}
                >
                  <div className="truncate">{assignment.title}</div>
                </div>
              ))}
              {day.assignments.length > 2 && (
                <div className="text-[10px] text-slate-500 text-center">
                  +{day.assignments.length - 2} khác
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Legend */}
      <div className="mt-4 pt-4 border-t border-slate-200">
        <div className="flex flex-wrap gap-4 text-xs">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded bg-orange-500"></div>
            <span className="text-slate-600">Đang tiến hành</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded bg-red-500"></div>
            <span className="text-slate-600">Sắp đến hạn</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded bg-green-500"></div>
            <span className="text-slate-600">Đã nộp</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded bg-gray-400"></div>
            <span className="text-slate-600">Quá hạn</span>
          </div>
        </div>
      </div>
    </div>
  );
}
