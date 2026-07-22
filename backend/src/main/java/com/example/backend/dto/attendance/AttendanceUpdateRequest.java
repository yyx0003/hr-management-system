package com.example.backend.dto.attendance;

import lombok.Data;

@Data
public class AttendanceUpdateRequest {

    private String attendanceTime;

    private String leavingTime;

    private String workType;
}