package edu.jsp.SmartAttendence.controller;

import edu.jsp.SmartAttendence.entity.*;
import edu.jsp.SmartAttendence.repository.UserRepository;
import edu.jsp.SmartAttendence.service.AppUserPrincipal;
import edu.jsp.SmartAttendence.service.AttendanceService;
import edu.jsp.SmartAttendence.service.ClassRoomService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ClassRoomService classRoomService;
    private final AttendanceService attendanceService;
    private final UserRepository userRepository;

    public AdminController(ClassRoomService classRoomService,
                            AttendanceService attendanceService,
                            UserRepository userRepository) {
        this.classRoomService = classRoomService;
        this.attendanceService = attendanceService;
        this.userRepository = userRepository;
    }

    private User currentAdmin(AppUserPrincipal principal) {
        return principal.getUser();
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        User admin = currentAdmin(principal);
        List<ClassRoom> classes = classRoomService.getClassesForAdmin(admin);
        model.addAttribute("admin", admin);
        model.addAttribute("classes", classes);
        model.addAttribute("totalStudents", userRepository.findByRole(Role.STUDENT).size());
        model.addAttribute("pendingRequestCount", classRoomService.getPendingRequestsForAdmin(admin).size());
        return "admin/dashboard";
    }

    @GetMapping("/class/create")
    public String createClassForm(Model model) {
        return "admin/create-class";
    }

    @PostMapping("/class/create")
    public String createClass(@RequestParam String className,
                               @RequestParam String subject,
                               @AuthenticationPrincipal AppUserPrincipal principal) {
        classRoomService.createClass(className, subject, currentAdmin(principal));
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/class/{id}")
    public String classDetail(@PathVariable Long id, Model model) {
        ClassRoom classRoom = classRoomService.getClassById(id);
        List<Enrollment> enrollments = classRoomService.getEnrollments(classRoom);
        List<AttendanceSession> sessions = attendanceService.getSessionsForClass(classRoom);

        // Calculate attendance percentages for each enrolled student
        Map<Long, Integer> studentPercentages = new HashMap<>();
        int atRiskCount = 0;
        for (Enrollment e : enrollments) {
            int pct = attendanceService.getStudentAttendancePercentageInClass(e.getStudent(), classRoom);
            studentPercentages.put(e.getStudent().getId(), pct);
            if (!sessions.isEmpty() && pct < 75) {
                atRiskCount++;
            }
        }

        model.addAttribute("classRoom", classRoom);
        model.addAttribute("enrollments", enrollments);
        model.addAttribute("sessions", sessions);
        model.addAttribute("pendingRequests", classRoomService.getPendingForClass(classRoom));
        model.addAttribute("studentPercentages", studentPercentages);
        model.addAttribute("atRiskCount", atRiskCount);
        return "admin/class-detail";
    }

    @GetMapping("/class/{id}/add-student")
    public String addStudentForm(@PathVariable Long id, Model model) {
        model.addAttribute("classRoom", classRoomService.getClassById(id));
        return "admin/add-student";
    }

    @PostMapping("/class/{id}/add-student")
    public String addStudent(@PathVariable Long id,
                              @RequestParam String fullName,
                              @RequestParam String email,
                              @RequestParam String rollNumber,
                              @RequestParam(required = false) String password) {
        ClassRoom classRoom = classRoomService.getClassById(id);
        classRoomService.addStudentToClass(classRoom, fullName, email, rollNumber, password);
        return "redirect:/admin/class/" + id + "?added=true";
    }

    // ---- Face enrollment capture page for a specific student ----
    @GetMapping("/student/{studentId}/enroll-face")
    public String enrollFacePage(@PathVariable Long studentId,
                                  @RequestParam(required = false) Long classId,
                                  Model model) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        model.addAttribute("student", student);
        model.addAttribute("classId", classId);
        return "admin/enroll-face";
    }

    // ---- Take attendance (camera) page ----
    @GetMapping("/class/{id}/take-attendance")
    public String takeAttendancePage(@PathVariable Long id, Model model) {
        ClassRoom classRoom = classRoomService.getClassById(id);
        model.addAttribute("classRoom", classRoom);
        return "admin/take-attendance";
    }

    @GetMapping("/class/{id}/session/{sessionId}")
    public String sessionDetail(@PathVariable Long id, @PathVariable Long sessionId, Model model) {
        ClassRoom classRoom = classRoomService.getClassById(id);
        AttendanceSession session = attendanceService.getSessionById(sessionId);
        model.addAttribute("classRoom", classRoom);
        model.addAttribute("session", session);
        model.addAttribute("records", attendanceService.getRecordsForSession(session));
        return "admin/session-detail";
    }

    // ---- Manual Attendance Override ----
    @PostMapping("/class/{classId}/session/{sessionId}/record/{recordId}/override")
    public String overrideRecord(@PathVariable Long classId,
                                  @PathVariable Long sessionId,
                                  @PathVariable Long recordId,
                                  @RequestParam AttendanceStatus status,
                                  @RequestParam(required = false) String reason) {
        attendanceService.overrideRecord(recordId, status, reason);
        return "redirect:/admin/class/" + classId + "/session/" + sessionId + "?overridden=true";
    }

    // ---- CSV Export: Full Class Attendance Roster ----
    @GetMapping("/class/{id}/export/csv")
    public void exportClassCsv(@PathVariable Long id, HttpServletResponse response) throws IOException {
        ClassRoom classRoom = classRoomService.getClassById(id);
        List<Enrollment> enrollments = classRoomService.getEnrollments(classRoom);
        List<AttendanceSession> sessions = attendanceService.getSessionsForClass(classRoom);
        int totalSessions = sessions.size();

        response.setContentType("text/csv");
        String filename = "attendance_" + classRoom.getClassName().replaceAll("[^a-zA-Z0-9_-]", "_") + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        PrintWriter writer = response.getWriter();
        writer.println("Student Name,Roll Number,Email,Total Sessions,Attended Sessions,Attendance Percentage,Status");

        for (Enrollment e : enrollments) {
            User student = e.getStudent();
            int pct = attendanceService.getStudentAttendancePercentageInClass(student, classRoom);
            int attended = (int) Math.round((pct / 100.0) * totalSessions);
            String status = (totalSessions > 0 && pct < 75) ? "At-Risk (<75%)" : "Good Standing";

            writer.printf("\"%s\",\"%s\",\"%s\",%d,%d,%d%%,\"%s\"%n",
                    student.getFullName(),
                    student.getRollNumber() != null ? student.getRollNumber() : "",
                    student.getEmail(),
                    totalSessions,
                    attended,
                    pct,
                    status);
        }
        writer.flush();
    }

    // ---- CSV Export: Specific Session Log ----
    @GetMapping("/class/{id}/session/{sessionId}/export/csv")
    public void exportSessionCsv(@PathVariable Long id, @PathVariable Long sessionId, HttpServletResponse response) throws IOException {
        ClassRoom classRoom = classRoomService.getClassById(id);
        AttendanceSession session = attendanceService.getSessionById(sessionId);
        List<AttendanceRecord> records = attendanceService.getRecordsForSession(session);

        response.setContentType("text/csv");
        String dateStr = session.getSessionTime().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        String filename = "session_" + classRoom.getClassName().replaceAll("[^a-zA-Z0-9_-]", "_") + "_" + dateStr + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        PrintWriter writer = response.getWriter();
        writer.println("Student Name,Roll Number,Email,Status,Confidence Distance,Manual Override,Override Reason");

        for (AttendanceRecord r : records) {
            User s = r.getStudent();
            String dist = r.getMatchDistance() != null ? String.format("%.3f", r.getMatchDistance()) : "N/A";
            String overridden = Boolean.TRUE.equals(r.getIsOverridden()) ? "Yes" : "No";
            String reason = r.getOverrideReason() != null ? r.getOverrideReason() : "";

            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    s.getFullName(),
                    s.getRollNumber() != null ? s.getRollNumber() : "",
                    s.getEmail(),
                    r.getStatus().name(),
                    dist,
                    overridden,
                    reason);
        }
        writer.flush();
    }

    // ---- Join requests: students request to join via class code, admin approves/rejects ----

    @GetMapping("/join-requests")
    public String joinRequests(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("requests", classRoomService.getPendingRequestsForAdmin(currentAdmin(principal)));
        return "admin/join-requests";
    }

    @PostMapping("/join-requests/{id}/approve")
    public String approveJoinRequest(@PathVariable Long id,
                                      @RequestParam(required = false) String redirect) {
        classRoomService.approveJoinRequest(id);
        return "redirect:" + (redirect != null && !redirect.isBlank() ? redirect : "/admin/join-requests");
    }

    @PostMapping("/join-requests/{id}/reject")
    public String rejectJoinRequest(@PathVariable Long id,
                                     @RequestParam(required = false) String redirect) {
        classRoomService.rejectJoinRequest(id);
        return "redirect:" + (redirect != null && !redirect.isBlank() ? redirect : "/admin/join-requests");
    }
}
