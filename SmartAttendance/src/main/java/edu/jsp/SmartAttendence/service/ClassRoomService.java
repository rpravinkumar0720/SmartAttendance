package edu.jsp.SmartAttendence.service;

import edu.jsp.SmartAttendence.entity.ClassRoom;
import edu.jsp.SmartAttendence.entity.Enrollment;
import edu.jsp.SmartAttendence.entity.JoinRequest;
import edu.jsp.SmartAttendence.entity.JoinRequestStatus;
import edu.jsp.SmartAttendence.entity.Role;
import edu.jsp.SmartAttendence.entity.User;
import edu.jsp.SmartAttendence.repository.ClassRoomRepository;
import edu.jsp.SmartAttendence.repository.EnrollmentRepository;
import edu.jsp.SmartAttendence.repository.JoinRequestRepository;
import edu.jsp.SmartAttendence.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClassRoomService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    private final ClassRoomRepository classRoomRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public ClassRoomService(ClassRoomRepository classRoomRepository,
                             EnrollmentRepository enrollmentRepository,
                             UserRepository userRepository,
                             JoinRequestRepository joinRequestRepository,
                             PasswordEncoder passwordEncoder,
                             EmailService emailService) {
        this.classRoomRepository = classRoomRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public ClassRoom createClass(String className, String subject, User admin) {
        ClassRoom classRoom = new ClassRoom();
        classRoom.setClassName(className);
        classRoom.setSubject(subject);
        classRoom.setCreatedBy(admin);
        classRoom.setClassCode(generateUniqueCode());
        return classRoomRepository.save(classRoom);
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (classRoomRepository.existsByClassCode(code));
        return code;
    }

    public List<ClassRoom> getClassesForAdmin(User admin) {
        return classRoomRepository.findByCreatedBy(admin);
    }

    public List<ClassRoom> getAllClasses() {
        return classRoomRepository.findAll();
    }

    public ClassRoom getClassById(Long id) {
        return classRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + id));
    }

    public List<Enrollment> getEnrollments(ClassRoom classRoom) {
        return enrollmentRepository.findByClassRoom(classRoom);
    }

    /**
     * Adds a student to a class. If a student with the given email doesn't exist yet,
     * creates a new STUDENT account (with a default password derived from roll number)
     * and enrolls them; otherwise just enrolls the existing student.
     */
    public User addStudentToClass(ClassRoom classRoom, String fullName, String email,
                                   String rollNumber, String rawPassword) {
        User student = userRepository.findByEmail(email).orElse(null);

        if (student == null) {
            student = new User();
            student.setFullName(fullName);
            student.setEmail(email);
            student.setRollNumber(rollNumber);
            student.setRole(Role.STUDENT);
            String password = (rawPassword != null && !rawPassword.isBlank()) ? rawPassword : rollNumber;
            student.setPassword(passwordEncoder.encode(password));
            student = userRepository.save(student);
        } else if (student.getRollNumber() == null || student.getRollNumber().isBlank()) {
            // Student self-registered via signup earlier; fill in the roll number now.
            student.setRollNumber(rollNumber);
            student = userRepository.save(student);
        }

        if (!enrollmentRepository.existsByClassRoomAndStudent(classRoom, student)) {
            enrollmentRepository.save(new Enrollment(classRoom, student));
        }
        return student;
    }

    public void saveFaceEnrollment(Long studentId, String descriptorJson, String photoBase64) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        student.setFaceDescriptor(descriptorJson);
        student.setPhotoBase64(photoBase64);
        userRepository.save(student);
    }

    // ---------------------------------------------------------------
    // Join requests: a student submits a class code + optional roll number;
    // an admin reviews and approves/rejects. Approval creates the Enrollment.
    // ---------------------------------------------------------------

    public JoinRequest requestToJoin(User student, String classCode, String rollNumber) {
        if (classCode == null || classCode.isBlank()) {
            throw new IllegalArgumentException("Please enter a class code.");
        }
        ClassRoom classRoom = classRoomRepository.findByClassCode(classCode.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("No class found with that code. Double-check with your teacher."));

        if (enrollmentRepository.existsByClassRoomAndStudent(classRoom, student)) {
            throw new IllegalArgumentException("You're already enrolled in this class.");
        }

        JoinRequest existing = joinRequestRepository.findByClassRoomAndStudent(classRoom, student).orElse(null);
        if (existing != null && existing.getStatus() == JoinRequestStatus.PENDING) {
            throw new IllegalArgumentException("You already have a pending request for this class.");
        }

        boolean studentNeedsRollNumber = student.getRollNumber() == null || student.getRollNumber().isBlank();
        String trimmedRoll = rollNumber == null ? null : rollNumber.trim();
        if (studentNeedsRollNumber) {
            if (trimmedRoll == null || trimmedRoll.isBlank()) {
                throw new IllegalArgumentException("Please enter your roll number.");
            }
            if (userRepository.existsByRollNumber(trimmedRoll)) {
                throw new IllegalArgumentException("That roll number is already registered to another student.");
            }
        }

        JoinRequest request = existing != null ? existing : new JoinRequest();
        request.setClassRoom(classRoom);
        request.setStudent(student);
        request.setRollNumber(studentNeedsRollNumber ? trimmedRoll : student.getRollNumber());
        request.setStatus(JoinRequestStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());
        request.setDecidedAt(null);
        JoinRequest saved = joinRequestRepository.save(request);

        // Notify class creator/admin via email
        try {
            User admin = classRoom.getCreatedBy();
            if (admin != null && admin.getEmail() != null) {
                new Thread(() -> {
                    try {
                        emailService.sendNewJoinRequestAlert(admin.getEmail(), admin.getFullName(), student.getFullName(), saved.getRollNumber(), classRoom.getClassName());
                    } catch (Exception ignored) {}
                }).start();
            }
        } catch (Exception ignored) {}

        return saved;
    }

    public List<JoinRequest> getPendingForClass(ClassRoom classRoom) {
        return joinRequestRepository.findByClassRoomAndStatus(classRoom, JoinRequestStatus.PENDING);
    }

    public List<JoinRequest> getPendingRequestsForAdmin(User admin) {
        return joinRequestRepository.findByClassRoomCreatedByAndStatusOrderByRequestedAtDesc(admin, JoinRequestStatus.PENDING);
    }

    public List<JoinRequest> getRequestsForStudent(User student) {
        return joinRequestRepository.findByStudentOrderByRequestedAtDesc(student);
    }

    public void approveJoinRequest(Long requestId) {
        JoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        request.setStatus(JoinRequestStatus.APPROVED);
        request.setDecidedAt(LocalDateTime.now());
        joinRequestRepository.save(request);

        User student = request.getStudent();
        if ((student.getRollNumber() == null || student.getRollNumber().isBlank())
                && request.getRollNumber() != null && !request.getRollNumber().isBlank()
                && !userRepository.existsByRollNumber(request.getRollNumber())) {
            student.setRollNumber(request.getRollNumber());
            userRepository.save(student);
        }

        if (!enrollmentRepository.existsByClassRoomAndStudent(request.getClassRoom(), student)) {
            enrollmentRepository.save(new Enrollment(request.getClassRoom(), student));
        }
    }

    public void rejectJoinRequest(Long requestId) {
        JoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));
        request.setStatus(JoinRequestStatus.REJECTED);
        request.setDecidedAt(LocalDateTime.now());
        joinRequestRepository.save(request);
    }
}
