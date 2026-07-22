package com.example.backend.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceListItem {

    private String workDate;

    private String attendanceTime;

    private String leavingTime;

    private String workType;

    private String holidayType;

    private String holidayName;
}