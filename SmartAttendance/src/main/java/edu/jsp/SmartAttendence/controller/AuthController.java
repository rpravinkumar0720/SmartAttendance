package edu.jsp.SmartAttendence.controller;

import edu.jsp.SmartAttendence.entity.User;
import edu.jsp.SmartAttendence.service.AuthService;
import edu.jsp.SmartAttendence.service.PendingSignup;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.Optional;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // =====================================================
    // STUDENT SELF-SIGNUP
    // =====================================================

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String fullName,
                          @RequestParam String email,
                          @RequestParam String password,
                          @RequestParam String confirmPassword,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {

        String cleanEmail = email == null ? "" : email.trim().toLowerCase();
        String cleanName = fullName == null ? "" : fullName.trim();

        if (cleanName.isEmpty() || cleanEmail.isEmpty() || password == null || password.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "All fields are required.");
            return "redirect:/signup";
        }
        if (!cleanEmail.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            redirectAttributes.addFlashAttribute("error", "Please enter a valid email address.");
            return "redirect:/signup";
        }
        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters long.");
            return "redirect:/signup";
        }
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/signup";
        }
        if (authService.emailExists(cleanEmail)) {
            redirectAttributes.addFlashAttribute("error", "An account with this email already exists.");
            return "redirect:/signup";
        }

        // Nothing is written to the database yet. The submitted details are
        // held in the session as a PendingSignup until the OTP is verified.
        PendingSignup pending;
        try {
            pending = authService.startStudentSignup(cleanName, cleanEmail, password);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Could not send the verification email. Please check the mail server configuration and try again.");
            return "redirect:/signup";
        }

        session.setAttribute("pendingSignup", pending);
        redirectAttributes.addFlashAttribute("success", "Enter the OTP sent to your email to activate your account.");
        return "redirect:/verify-signup-otp";
    }

    @GetMapping("/verify-signup-otp")
    public String verifySignupOtpPage(HttpSession session, Model model) {
        PendingSignup pending = (PendingSignup) session.getAttribute("pendingSignup");
        if (pending == null) {
            return "redirect:/signup";
        }
        model.addAttribute("email", maskEmail(pending.getEmail()));
        model.addAttribute("mode", "signup");
        return "verify-otp";
    }

    @PostMapping("/verify-signup-otp")
    public String verifySignupOtp(@RequestParam String otp,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        PendingSignup pending = (PendingSignup) session.getAttribute("pendingSignup");
        if (pending == null) {
            return "redirect:/signup";
        }

        if (!authService.verifyPendingOtp(pending, otp == null ? "" : otp.trim())) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired OTP. Please try again.");
            return "redirect:/verify-signup-otp";
        }

        // OTP confirmed - only now is the account actually created.
        if (authService.emailExists(pending.getEmail())) {
            // Extremely unlikely race (e.g. two tabs signing up at once), but
            // guard against a duplicate-email DB error either way.
            session.removeAttribute("pendingSignup");
            redirectAttributes.addFlashAttribute("error", "An account with this email already exists. Please sign in.");
            return "redirect:/login";
        }

        authService.completeStudentSignup(pending);
        session.removeAttribute("pendingSignup");
        redirectAttributes.addFlashAttribute("success", "Email verified! You can now sign in.");
        return "redirect:/login";
    }

    @PostMapping("/resend-signup-otp")
    public String resendSignupOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        PendingSignup pending = (PendingSignup) session.getAttribute("pendingSignup");
        if (pending == null) {
            return "redirect:/signup";
        }
        authService.resendSignupOtp(pending);
        redirectAttributes.addFlashAttribute("success", "A new OTP has been sent to your email.");
        return "redirect:/verify-signup-otp";
    }

    // =====================================================
    // FORGOT PASSWORD - request OTP
    // =====================================================

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {

        String cleanEmail = email == null ? "" : email.trim().toLowerCase();
        Optional<User> userOpt = authService.findByEmail(cleanEmail);

        if (!userOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "No account found with this email address.");
            return "redirect:/forgot-password";
        }

        try {
            authService.generateAndSendOtp(userOpt.get(), false);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Could not send OTP email. Please check the mail server configuration and try again.");
            return "redirect:/forgot-password";
        }

        session.setAttribute("resetEmail", cleanEmail);
        redirectAttributes.addFlashAttribute("success", "An OTP has been sent to your email address.");
        return "redirect:/verify-otp";
    }

    // =====================================================
    // VERIFY OTP (password reset)
    // =====================================================

    @GetMapping("/verify-otp")
    public String verifyOtpPage(HttpSession session, Model model) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) {
            return "redirect:/forgot-password";
        }
        model.addAttribute("email", maskEmail(email));
        model.addAttribute("mode", "reset");
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String otp,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        String email = (String) session.getAttribute("resetEmail");
        if (email == null) {
            return "redirect:/forgot-password";
        }

        Optional<User> userOpt = authService.findByEmail(email);
        if (!userOpt.isPresent() || !authService.verifyOtp(userOpt.get(), otp == null ? "" : otp.trim())) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired OTP. Please try again.");
            return "redirect:/verify-otp";
        }

        session.setAttribute("otpVerified", true);
        return "redirect:/reset-password";
    }

    @PostMapping("/resend-otp")
    public String resendOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute("resetEmail");
        if (email == null) {
            return "redirect:/forgot-password";
        }
        authService.findByEmail(email).ifPresent(u -> authService.generateAndSendOtp(u, false));
        redirectAttributes.addFlashAttribute("success", "A new OTP has been sent to your email.");
        return "redirect:/verify-otp";
    }

    // =====================================================
    // RESET PASSWORD
    // =====================================================

    @GetMapping("/reset-password")
    public String resetPasswordPage(HttpSession session) {
        Boolean verified = (Boolean) session.getAttribute("otpVerified");
        String email = (String) session.getAttribute("resetEmail");
        if (verified == null || !verified || email == null) {
            return "redirect:/forgot-password";
        }
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String password,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        Boolean verified = (Boolean) session.getAttribute("otpVerified");
        String email = (String) session.getAttribute("resetEmail");
        if (verified == null || !verified || email == null) {
            return "redirect:/forgot-password";
        }

        if (password == null || password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters long.");
            return "redirect:/reset-password";
        }
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/reset-password";
        }

        Optional<User> userOpt = authService.findByEmail(email);
        if (!userOpt.isPresent()) {
            return "redirect:/forgot-password";
        }

        authService.resetPassword(userOpt.get(), password);

        session.removeAttribute("resetEmail");
        session.removeAttribute("otpVerified");

        redirectAttributes.addFlashAttribute("success", "Password reset successful! Please sign in.");
        return "redirect:/login";
    }

    // Utility: mask an email like jo***@gmail.com for display on the OTP page
    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        String name = email.substring(0, at);
        String domain = email.substring(at);
        String visible = name.substring(0, Math.min(2, name.length()));
        return visible + "***" + domain;
    }
}
