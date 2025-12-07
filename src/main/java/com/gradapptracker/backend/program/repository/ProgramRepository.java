package com.gradapptracker.backend.program.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.gradapptracker.backend.program.entity.Program;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Program entity.
 * <p>
 * Provides CRUD operations and custom query methods for program management.
 * Extends JpaSpecificationExecutor to support dynamic filtering with
 * Specifications.
 * All queries are scoped to specific users for data isolation.
 */
@Repository
public interface ProgramRepository extends JpaRepository<Program, Integer>, JpaSpecificationExecutor<Program> {
    List<Program> findAllByUserUserId(Integer userId);

    Page<Program> findAllByUserUserId(Integer userId, Pageable pageable);

    Optional<Program> findByProgramIdAndUserUserId(Integer programId, Integer userId);
}
