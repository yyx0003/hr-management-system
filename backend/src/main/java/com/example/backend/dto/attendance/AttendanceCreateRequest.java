package com.example.backend.dto.attendance;

import lombok.Data;

@Data
public class AttendanceCreateRequest {

    private String workDate;

    private String attendanceTime;

    private String leavingTime;

    private String workType;
}