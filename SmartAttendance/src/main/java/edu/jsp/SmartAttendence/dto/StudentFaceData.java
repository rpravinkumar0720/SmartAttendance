package edu.jsp.SmartAttendence.dto;

public class StudentFaceData {
    private Long studentId;
    private String fullName;
    private String rollNumber;
    private String descriptor; // JSON array string

    public StudentFaceData(Long studentId, String fullName, String rollNumber, String descriptor) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.rollNumber = rollNumber;
        this.descriptor = descriptor;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public String getDescriptor() {
        return descriptor;
    }
}
