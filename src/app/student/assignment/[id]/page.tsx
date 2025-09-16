"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { withAuth } from "@/components/hoc/withAuth";
import { Role } from "@/types/auth";
import { AssignmentService } from "@/services/assignment.service";
import { use } from "react";

type Props = { params: Promise<{ id: string }> };

function AssignmentRedirect({ params }: Props) {
  const resolvedParams = use(params);
  const router = useRouter();

  useEffect(() => {
    const redirectToAssignment = async () => {
      try {
        const assignmentId = parseInt(resolvedParams.id);
        
        // Get assignment details to find the course ID
        const assignment = await AssignmentService.getAssignmentForStudent(assignmentId);
        
        // Redirect to the proper course-based route
        router.replace(`/student/course/${assignment.courseId}/assignment/${assignment.id}`);
      } catch (err) {
        console.error('Failed to redirect to assignment:', err);
        // Redirect back to student dashboard on error
        router.replace('/student');
      }
    };

    redirectToAssignment();
  }, [resolvedParams.id, router]);

  return (
  
  <div className="text-slate-600">Đang chuyển hướng đến bài tập...</div>
    
  );
}

export default withAuth(AssignmentRedirect, {
  requiredRoles: [Role.STUDENT, Role.TEACHER, Role.ADMIN],
});
