package com.gradapptracker.program.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramCreateDTO {

    // userId is provided to associate the program with a user
    private Integer userId;

    @NotBlank(message = "universityName is required")
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
