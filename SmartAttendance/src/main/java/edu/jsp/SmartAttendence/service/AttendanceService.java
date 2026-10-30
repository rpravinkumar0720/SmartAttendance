package edu.jsp.SmartAttendence.service;

import edu.jsp.SmartAttendence.entity.*;
import edu.jsp.SmartAttendence.repository.AttendanceRecordRepository;
import edu.jsp.SmartAttendence.repository.AttendanceSessionRepository;
import edu.jsp.SmartAttendence.repository.EnrollmentRepository;
import edu.jsp.SmartAttendence.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public AttendanceService(AttendanceSessionRepository sessionRepository,
                              AttendanceRecordRepository recordRepository,
                              EnrollmentRepository enrollmentRepository,
                              UserRepository userRepository,
                              EmailService emailService) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public AttendanceSession startSession(ClassRoom classRoom, User takenBy) {
        AttendanceSession session = new AttendanceSession();
        session.setClassRoom(classRoom);
        session.setTakenBy(takenBy);
        return sessionRepository.save(session);
    }

    /**
     * Persists the attendance for a session. recognizedStudentIdsWithDistance maps
     * studentId -> matchDistance for everyone the browser's face-api.js recognized.
     * Every other student enrolled in the class is marked ABSENT.
     */
    public AttendanceSession finalizeSession(AttendanceSession session,
                                              Map<Long, Double> recognizedStudentIdsWithDistance) {
        List<Enrollment> enrollments = enrollmentRepository.findByClassRoom(session.getClassRoom());

        for (Enrollment enrollment : enrollments) {
            User student = enrollment.getStudent();
            AttendanceRecord record = new AttendanceRecord();
            record.setSession(session);
            record.setStudent(student);

            if (recognizedStudentIdsWithDistance.containsKey(student.getId())) {
                record.setStatus(AttendanceStatus.PRESENT);
                record.setMatchDistance(recognizedStudentIdsWithDistance.get(student.getId()));
            } else {
                record.setStatus(AttendanceStatus.ABSENT);
            }
            recordRepository.save(record);

            // Check if attendance dropped below 75% threshold
            try {
                int percent = getStudentAttendancePercentageInClass(student, session.getClassRoom());
                List<AttendanceSession> allSessions = sessionRepository.findByClassRoomOrderBySessionTimeDesc(session.getClassRoom());
                if (allSessions.size() >= 2 && percent < 75) {
                    new Thread(() -> {
                        try {
                            emailService.sendLowAttendanceWarning(student.getEmail(), student.getFullName(), session.getClassRoom().getClassName(), percent);
                        } catch (Exception ignored) {}
                    }).start();
                }
            } catch (Exception ignored) {}
        }
        return session;
    }

    /**
     * Allows faculty to manually override a student's attendance status for a session.
     */
    public AttendanceRecord overrideRecord(Long recordId, AttendanceStatus newStatus, String reason) {
        AttendanceRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));
        record.setStatus(newStatus);
        record.setIsOverridden(true);
        record.setOverrideReason(reason != null && !reason.isBlank() ? reason : "Manual faculty correction");
        return recordRepository.save(record);
    }

    public int getStudentAttendancePercentageInClass(User student, ClassRoom classRoom) {
        List<AttendanceSession> sessions = sessionRepository.findByClassRoomOrderBySessionTimeDesc(classRoom);
        if (sessions.isEmpty()) return 0;
        int presentCount = 0;
        for (AttendanceSession s : sessions) {
            boolean present = recordRepository.findBySession(s).stream()
                    .anyMatch(r -> r.getStudent().getId().equals(student.getId()) && r.getStatus() == AttendanceStatus.PRESENT);
            if (present) presentCount++;
        }
        return (int) Math.round(((double) presentCount / sessions.size()) * 100);
    }

    public List<AttendanceSession> getSessionsForClass(ClassRoom classRoom) {
        return sessionRepository.findByClassRoomOrderBySessionTimeDesc(classRoom);
    }

    public List<AttendanceRecord> getRecordsForSession(AttendanceSession session) {
        return recordRepository.findBySession(session);
    }

    public List<AttendanceRecord> getRecordsForStudent(User student) {
        return recordRepository.findByStudent(student);
    }

    /**
     * Simple summary: total sessions vs sessions present, per class, for a student.
     */
    public Map<ClassRoom, int[]> getAttendanceSummary(User student) {
        Map<ClassRoom, int[]> summary = new HashMap<>(); // [present, total]
        List<Enrollment> enrollments = enrollmentRepository.findByStudent(student);
        for (Enrollment e : enrollments) {
            ClassRoom cr = e.getClassRoom();
            List<AttendanceSession> sessions = sessionRepository.findByClassRoomOrderBySessionTimeDesc(cr);
            int total = sessions.size();
            int present = 0;
            for (AttendanceSession s : sessions) {
                boolean wasPresent = recordRepository.findBySession(s).stream()
                        .anyMatch(r -> r.getStudent().getId().equals(student.getId())
                                && r.getStatus() == AttendanceStatus.PRESENT);
                if (wasPresent) present++;
            }
            summary.put(cr, new int[]{present, total});
        }
        return summary;
    }

    public AttendanceSession getSessionById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + id));
    }
}
