package edu.jsp.SmartAttendence.dto;

public class RecognizedStudent {
    private Long studentId;
    private double distance;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}
