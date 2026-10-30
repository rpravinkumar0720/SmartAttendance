package edu.jsp.SmartAttendence.controller;

import edu.jsp.SmartAttendence.entity.ClassRoom;
import edu.jsp.SmartAttendence.entity.User;
import edu.jsp.SmartAttendence.service.AppUserPrincipal;
import edu.jsp.SmartAttendence.service.AttendanceService;
import edu.jsp.SmartAttendence.service.ClassRoomService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final AttendanceService attendanceService;
    private final ClassRoomService classRoomService;

    public StudentController(AttendanceService attendanceService, ClassRoomService classRoomService) {
        this.attendanceService = attendanceService;
        this.classRoomService = classRoomService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        User student = principal.getUser();
        Map<ClassRoom, int[]> summary = attendanceService.getAttendanceSummary(student);

        // Build a simple percentage map for the view
        Map<ClassRoom, Double> percentages = new LinkedHashMap<>();
        for (Map.Entry<ClassRoom, int[]> e : summary.entrySet()) {
            int present = e.getValue()[0];
            int total = e.getValue()[1];
            double pct = total == 0 ? 0.0 : (present * 100.0 / total);
            percentages.put(e.getKey(), Math.round(pct * 10) / 10.0);
        }

        model.addAttribute("student", student);
        model.addAttribute("summary", summary);
        model.addAttribute("percentages", percentages);
        model.addAttribute("pendingRequests", classRoomService.getRequestsForStudent(student));
        return "student/dashboard";
    }

    @GetMapping("/class/{id}/history")
    public String classHistory(@PathVariable Long id,
                                @AuthenticationPrincipal AppUserPrincipal principal,
                                Model model) {
        User student = principal.getUser();
        model.addAttribute("student", student);
        model.addAttribute("records", attendanceService.getRecordsForStudent(student));
        model.addAttribute("classId", id);
        return "student/history";
    }

    // ---- Join a class by code ----

    @GetMapping("/join-class")
    public String joinClassForm(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("student", principal.getUser());
        model.addAttribute("myRequests", classRoomService.getRequestsForStudent(principal.getUser()));
        return "student/join-class";
    }

    @PostMapping("/join-class")
    public String joinClass(@AuthenticationPrincipal AppUserPrincipal principal,
                             @RequestParam String classCode,
                             @RequestParam(required = false) String rollNumber,
                             RedirectAttributes redirectAttributes) {
        try {
            classRoomService.requestToJoin(principal.getUser(), classCode, rollNumber);
            redirectAttributes.addFlashAttribute("success", "Request sent. Your admin will review it shortly.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student/join-class";
    }

    // ---- Self-service face enrollment (no admin needed) ----

    @GetMapping("/enroll-face")
    public String enrollFacePage(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("student", principal.getUser());
        return "student/enroll-face";
    }
}
