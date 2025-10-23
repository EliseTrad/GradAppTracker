package com.gradapptracker.backend.program.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.gradapptracker.backend.program.entity.Program;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Integer>, JpaSpecificationExecutor<Program> {
    List<Program> findAllByUserUserId(Integer userId);

    Optional<Program> findByProgramIdAndUserUserId(Integer programId, Integer userId);
}

