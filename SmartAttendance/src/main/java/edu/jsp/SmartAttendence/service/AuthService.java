package edu.jsp.SmartAttendence.service;

import edu.jsp.SmartAttendence.entity.Role;
import edu.jsp.SmartAttendence.entity.User;
import edu.jsp.SmartAttendence.repository.UserRepository;
import edu.jsp.SmartAttendence.util.OtpUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Handles the student-facing login-security flows ported from the
 * securelogin-app: self-signup with email OTP verification, forgot
 * password, and OTP-based password reset.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Starts the student self-signup flow WITHOUT touching the database.
     * The submitted details are packaged into a PendingSignup (to be kept
     * in the session) with a freshly generated OTP, and the OTP is emailed.
     * The User row is only created once verifySignupOtp() succeeds.
     */
    public PendingSignup startStudentSignup(String fullName, String email, String rawPassword) {
        PendingSignup pending = new PendingSignup(fullName, email, passwordEncoder.encode(rawPassword));
        issueOtp(pending);
        emailService.sendSignupOtpEmail(pending.getEmail(), pending.getFullName(), pending.getOtp());
        return pending;
    }

    /** Generates a fresh 6-digit OTP (valid 5 minutes) and stores it on the pending signup. */
    public void issueOtp(PendingSignup pending) {
        pending.setOtp(OtpUtil.generateOtp());
        pending.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
    }

    /** Resends a new OTP for a pending (not-yet-created) signup. */
    public void resendSignupOtp(PendingSignup pending) {
        issueOtp(pending);
        emailService.sendSignupOtpEmail(pending.getEmail(), pending.getFullName(), pending.getOtp());
    }

    public boolean verifyPendingOtp(PendingSignup pending, String otp) {
        if (pending.getOtp() == null || pending.getOtpExpiry() == null) {
            return false;
        }
        if (!pending.getOtp().equals(otp)) {
            return false;
        }
        return LocalDateTime.now().isBefore(pending.getOtpExpiry());
    }

    /**
     * Creates the verified STUDENT account in the database. Only ever
     * called after the signup OTP has been successfully verified.
     */
    public User completeStudentSignup(PendingSignup pending) {
        User user = new User();
        user.setFullName(pending.getFullName());
        user.setEmail(pending.getEmail());
        user.setPassword(pending.getEncodedPassword());
        user.setRole(Role.STUDENT);
        user.setVerified(true);
        return userRepository.save(user);
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    /**
     * Generates a fresh 6-digit OTP, valid for 5 minutes, and emails it.
     *
     * @param forSignup true to send the "verify your account" wording,
     *                   false to send the "reset your password" wording.
     */
    public void generateAndSendOtp(User user, boolean forSignup) {
        String otp = OtpUtil.generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);
        if (forSignup) {
            emailService.sendSignupOtpEmail(user.getEmail(), user.getFullName(), otp);
        } else {
            emailService.sendPasswordResetOtpEmail(user.getEmail(), user.getFullName(), otp);
        }
    }

    public boolean verifyOtp(User user, String otp) {
        if (user.getOtp() == null || user.getOtpExpiry() == null) {
            return false;
        }
        if (!user.getOtp().equals(otp)) {
            return false;
        }
        return LocalDateTime.now().isBefore(user.getOtpExpiry());
    }

    /** Marks a self-registered student's email as verified after a successful signup OTP check. */
    public void markVerified(User user) {
        user.setVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
    }

    public void resetPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
    }
}
