package com.gradapptracker.backend.program.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramDTO {
    private Integer programId;
    private Integer userId;
    private String universityName;
    private String fieldOfStudy;
    private String focusArea;
    private String portal;
    private String website;
    private LocalDate deadline;
    private String status;
    private String tuition;
    private String requirements;
    private String notes;
}

