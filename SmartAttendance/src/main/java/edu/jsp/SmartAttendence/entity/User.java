package edu.jsp.SmartAttendence.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // BCrypt encoded

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Only relevant for STUDENT role
    @Column(unique = true)
    private String rollNumber;

    // Face descriptor captured during enrollment, stored as a JSON array string
    // e.g. "[0.12,-0.03,...]" (128 floats produced by face-api.js)
    @Lob
    @Column(columnDefinition = "TEXT")
    private String faceDescriptor;

    // Base64 snapshot of the enrollment photo, shown in UI as a profile thumbnail
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String photoBase64;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ---- Login-security fields (ported from securelogin-app) ----
    // Used both for signup email verification and forgot-password OTP resets.
    @Column(length = 6)
    private String otp;

    @Column
    private LocalDateTime otpExpiry;

    // True once a self-registered student has verified their email via OTP.
    // Accounts created directly by an admin, or that existed before this
    // column was added, are treated as verified (null == verified).
    @Column
    private Boolean verified = Boolean.TRUE;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getFaceDescriptor() {
        return faceDescriptor;
    }

    public void setFaceDescriptor(String faceDescriptor) {
        this.faceDescriptor = faceDescriptor;
    }

    public String getPhotoBase64() {
        return photoBase64;
    }

    public void setPhotoBase64(String photoBase64) {
        this.photoBase64 = photoBase64;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isFaceEnrolled() {
        return faceDescriptor != null && !faceDescriptor.isBlank();
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

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }
}
