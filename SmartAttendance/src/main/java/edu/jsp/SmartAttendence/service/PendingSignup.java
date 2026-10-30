package edu.jsp.SmartAttendence.service;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Holds a not-yet-created student account while the signup OTP is pending.
 * Nothing is written to the users table until the OTP is verified, so an
 * account is only ever created once the email address is confirmed.
 */
public class PendingSignup implements Serializable {

    private final String fullName;
    private final String email;
    private final String encodedPassword;

    private String otp;
    private LocalDateTime otpExpiry;

    public PendingSignup(String fullName, String email, String encodedPassword) {
        this.fullName = fullName;
        this.email = email;
        this.encodedPassword = encodedPassword;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public LocalDateTime getOtpExpiry() {
        return otpExpiry;
    }

    public void setOtpExpiry(LocalDateTime otpExpiry) {
        this.otpExpiry = otpExpiry;
    }
}
