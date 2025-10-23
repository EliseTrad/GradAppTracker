package com.gradapptracker.backend.program.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.gradapptracker.backend.user.entity.User;

import java.time.LocalDate;

@Entity
@Table(name = "programs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "program_id")
    private Integer programId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "university_name", nullable = false)
    private String universityName;

    @Column(name = "field_of_study")
    private String fieldOfStudy;

    @Column(name = "focus_area")
    private String focusArea;

    @Column
    private String portal;

    @Column
    private String website;

    @Column
    private LocalDate deadline;

    @Column
    private String status;

    @Column
    private String tuition;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
