package com.gradapptracker.program.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramSummaryDTO {
    private Integer programId;
    private Integer userId;
    private String universityName;
}
