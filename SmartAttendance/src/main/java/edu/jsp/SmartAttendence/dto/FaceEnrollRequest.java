package edu.jsp.SmartAttendence.dto;

public class FaceEnrollRequest {
    private Long studentId;
    private String descriptor;   // JSON array string of 128 floats from face-api.js
    private String photoBase64;  // captured snapshot, for the profile thumbnail

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public String getPhotoBase64() {
        return photoBase64;
    }

    public void setPhotoBase64(String photoBase64) {
        this.photoBase64 = photoBase64;
    }
}
