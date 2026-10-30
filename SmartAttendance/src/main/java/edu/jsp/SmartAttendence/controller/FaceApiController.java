package edu.jsp.SmartAttendence.controller;

import edu.jsp.SmartAttendence.dto.*;
import edu.jsp.SmartAttendence.entity.*;
import edu.jsp.SmartAttendence.repository.EnrollmentRepository;
import edu.jsp.SmartAttendence.service.AppUserPrincipal;
import edu.jsp.SmartAttendence.service.AttendanceService;
import edu.jsp.SmartAttendence.service.ClassRoomService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class FaceApiController {

    private final ClassRoomService classRoomService;
    private final AttendanceService attendanceService;
    private final EnrollmentRepository enrollmentRepository;

    public FaceApiController(ClassRoomService classRoomService,
                              AttendanceService attendanceService,
                              EnrollmentRepository enrollmentRepository) {
        this.classRoomService = classRoomService;
        this.attendanceService = attendanceService;
        this.enrollmentRepository = enrollmentRepository;
    }

    // Save the face descriptor captured in the browser during student enrollment (admin-assisted)
    @PostMapping("/face/enroll")
    public Map<String, Object> enrollFace(@RequestBody FaceEnrollRequest request) {
        classRoomService.saveFaceEnrollment(request.getStudentId(), request.getDescriptor(), request.getPhotoBase64());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    // Save the face descriptor when a student enrolls their OWN face from their portal.
    // Deliberately ignores any studentId in the request body and uses the authenticated
    // principal instead, so a student can never overwrite another student's face data.
    @PostMapping("/face/enroll-self")
    public Map<String, Object> enrollSelf(@RequestBody FaceEnrollRequest request,
                                           @AuthenticationPrincipal AppUserPrincipal principal) {
        classRoomService.saveFaceEnrollment(principal.getUser().getId(), request.getDescriptor(), request.getPhotoBase64());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    // Returns every enrolled student's face descriptor for a class, so the browser
    // can run the matching (euclidean distance) entirely client-side during a live session.
    @GetMapping("/class/{id}/face-data")
    public List<StudentFaceData> getFaceData(@PathVariable Long id) {
        ClassRoom classRoom = classRoomService.getClassById(id);
        List<Enrollment> enrollments = enrollmentRepository.findByClassRoom(classRoom);
        return enrollments.stream()
                .map(e -> e.getStudent())
                .filter(User::isFaceEnrolled)
                .map(s -> new StudentFaceData(s.getId(), s.getFullName(), s.getRollNumber(), s.getFaceDescriptor()))
                .collect(Collectors.toList());
    }

    // Starts a new attendance session for a class
    @PostMapping("/class/{id}/session/start")
    public Map<String, Object> startSession(@PathVariable Long id,
                                             @AuthenticationPrincipal AppUserPrincipal principal) {
        ClassRoom classRoom = classRoomService.getClassById(id);
        AttendanceSession session = attendanceService.startSession(classRoom, principal.getUser());
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        return result;
    }

    // Finalizes attendance: everyone recognized by the browser is PRESENT, the rest ABSENT
    @PostMapping("/attendance/finalize")
    public Map<String, Object> finalizeAttendance(@RequestBody FinalizeAttendanceRequest request) {
        AttendanceSession session = attendanceService.getSessionById(request.getSessionId());
        Map<Long, Double> recognized = new HashMap<>();
        if (request.getRecognized() != null) {
            for (RecognizedStudent rs : request.getRecognized()) {
                recognized.put(rs.getStudentId(), rs.getDistance());
            }
        }
        attendanceService.finalizeSession(session, recognized);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("presentCount", recognized.size());
        return result;
    }
}
