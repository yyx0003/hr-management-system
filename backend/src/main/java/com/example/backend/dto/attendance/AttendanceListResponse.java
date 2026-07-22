package com.example.backend.dto.attendance;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AttendanceListResponse {

    private String targetMonth;

    private List<AttendanceListItem> attendanceList;
}