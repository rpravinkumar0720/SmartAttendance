package edu.jsp.SmartAttendence.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendSignupOtpEmail(String toEmail, String fullName, String otp) {
        sendOtpEmail(toEmail, fullName, otp,
                "Verify your Smart Attendance account",
                "Thanks for signing up! Use the One-Time Password (OTP) below to verify your email and activate your account:");
    }

    public void sendPasswordResetOtpEmail(String toEmail, String fullName, String otp) {
        sendOtpEmail(toEmail, fullName, otp,
                "Your Smart Attendance password reset OTP",
                "We received a request to reset your account password. Use the One-Time Password (OTP) below to continue:");
    }

    public void sendLowAttendanceWarning(String toEmail, String fullName, String className, int attendancePercent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail, "Smart Attendance");
            helper.setTo(toEmail);
            helper.setSubject("⚠️ Attendance Warning: Below 75% in " + className);

            String html =
                "<div style=\"font-family:'Segoe UI',Arial,sans-serif;max-width:520px;margin:auto;" +
                "border:1px solid #fed7aa;border-radius:12px;overflow:hidden;background:#fff;\">" +
                "<div style=\"background:linear-gradient(135deg,#f97316,#ea580c);padding:24px;text-align:center;\">" +
                "<h1 style=\"color:#fff;margin:0;font-size:20px;\">⚠️ Low Attendance Alert</h1></div>" +
                "<div style=\"padding:28px;color:#333;line-height:1.6;\">" +
                "<p>Hi <b>" + escape(fullName) + "</b>,</p>" +
                "<p>Your current attendance for <b>" + escape(className) + "</b> has fallen to:</p>" +
                "<div style=\"text-align:center;margin:22px 0;\">" +
                "<span style=\"display:inline-block;background:#fff7ed;color:#ea580c;font-size:32px;" +
                "padding:12px 28px;border-radius:10px;font-weight:700;border:1px solid #fdba74;\">" + attendancePercent + "%</span>" +
                "</div>" +
                "<p style=\"color:#b45309;\">The minimum required attendance threshold is <b>75%</b>. Please attend upcoming sessions regularly to avoid academic penalties.</p>" +
                "<p style=\"margin-top:28px;color:#999;font-size:12px;\">&copy; Smart Attendance Portal</p>" +
                "</div></div>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            // Log & don't block session finalization
            System.err.println("Failed to send low attendance email: " + e.getMessage());
        }
    }

    public void sendNewJoinRequestAlert(String adminEmail, String adminName, String studentName, String rollNumber, String className) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail, "Smart Attendance");
            helper.setTo(adminEmail);
            helper.setSubject("🔔 New Join Request for " + className);

            String html =
                "<div style=\"font-family:'Segoe UI',Arial,sans-serif;max-width:520px;margin:auto;" +
                "border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;background:#fff;\">" +
                "<div style=\"background:linear-gradient(135deg,#6366f1,#818cf8);padding:24px;text-align:center;\">" +
                "<h1 style=\"color:#fff;margin:0;font-size:20px;\">🔔 New Class Join Request</h1></div>" +
                "<div style=\"padding:28px;color:#333;line-height:1.6;\">" +
                "<p>Hi <b>" + escape(adminName) + "</b>,</p>" +
                "<p>A student has submitted a request to join your class <b>" + escape(className) + "</b>:</p>" +
                "<ul style=\"background:#f8fafc;padding:16px 28px;border-radius:8px;border:1px solid #e2e8f0;\">" +
                "<li><b>Student:</b> " + escape(studentName) + "</li>" +
                "<li><b>Roll Number:</b> " + escape(rollNumber) + "</li>" +
                "</ul>" +
                "<p>Log in to your admin dashboard to review and approve or reject this request.</p>" +
                "<p style=\"margin-top:28px;color:#999;font-size:12px;\">&copy; Smart Attendance Portal</p>" +
                "</div></div>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send join request alert email: " + e.getMessage());
        }
    }

    private void sendOtpEmail(String toEmail, String fullName, String otp, String subject, String intro) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail, "Smart Attendance");
            helper.setTo(toEmail);
            helper.setSubject(subject);

            String html =
                "<div style=\"font-family:'Segoe UI',Arial,sans-serif;max-width:480px;margin:auto;" +
                "border:1px solid #eee;border-radius:12px;overflow:hidden;\">" +
                "<div style=\"background:linear-gradient(135deg,#667eea,#764ba2);padding:26px;text-align:center;\">" +
                "<h1 style=\"color:#fff;margin:0;font-size:22px;letter-spacing:0.5px;\">🎓 Smart Attendance</h1></div>" +
                "<div style=\"padding:28px;color:#333;line-height:1.6;\">" +
                "<p>Hi " + escape(fullName) + ",</p>" +
                "<p>" + intro + "</p>" +
                "<div style=\"text-align:center;margin:26px 0;\">" +
                "<span style=\"display:inline-block;background:#f4f4ff;color:#764ba2;font-size:30px;" +
                "letter-spacing:10px;padding:16px 22px;border-radius:10px;font-weight:700;\">" + otp + "</span>" +
                "</div>" +
                "<p>This OTP is valid for <b>5 minutes</b>. Please do not share it with anyone.</p>" +
                "<p>If you did not request this, you can safely ignore this email.</p>" +
                "<p style=\"margin-top:32px;color:#999;font-size:12px;\">&copy; Smart Attendance</p>" +
                "</div></div>";

            helper.setText(html, true);
            mailSender.send(message);

        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage(), e);
        }
    }

    private String escape(String input) {
        return input == null ? "" : input.replace("<", "&lt;").replace(">", "&gt;");
    }
}
