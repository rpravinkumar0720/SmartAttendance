package edu.jsp.SmartAttendence.dto;

import java.util.List;

public class FinalizeAttendanceRequest {
    private Long sessionId;
    private List<RecognizedStudent> recognized;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public List<RecognizedStudent> getRecognized() {
        return recognized;
    }

    public void setRecognized(List<RecognizedStudent> recognized) {
        this.recognized = recognized;
    }
}
