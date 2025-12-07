package com.gradapptracker.backend.program.service;

import com.gradapptracker.backend.program.dto.ProgramCreateDTO;
import com.gradapptracker.backend.program.dto.ProgramDTO;
import com.gradapptracker.backend.program.dto.ProgramUpdateDTO;
import com.gradapptracker.backend.program.entity.ApplicationStatus;
import com.gradapptracker.backend.program.entity.Program;
import com.gradapptracker.backend.program.repository.ProgramRepository;
import com.gradapptracker.backend.user.entity.User;
import com.gradapptracker.backend.user.repository.UserRepository;
import com.gradapptracker.backend.document.repository.DocumentRepository;
import com.gradapptracker.backend.shared.dto.DashboardStatsDTO;
import com.gradapptracker.backend.exception.ForbiddenException;
import com.gradapptracker.backend.exception.NotFoundException;
import com.gradapptracker.backend.exception.UnauthorizedException;
import com.gradapptracker.backend.exception.ValidationException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProgramService {

    /**
     * Service layer for Program entity operations.
     *
     * Responsibilities:
     * - Create, read, update and delete Program records that belong to a specific
     * user.
     * - Provide database-side smart filtering using JPA Specifications.
     * - Enforce ownership checks: most methods accept a userId and ensure the
     * targeted Program belongs to that user before performing mutations.
     */

    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    public ProgramService(ProgramRepository programRepository, UserRepository userRepository,
            DocumentRepository documentRepository) {
        this.programRepository = programRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * Map Program entity to ProgramDTO.
     */
    private ProgramDTO toDto(Program p) {
        ProgramDTO dto = new ProgramDTO();
        dto.setProgramId(p.getProgramId());
        dto.setUserId(p.getUser() != null ? p.getUser().getUserId() : null);
        dto.setUniversityName(p.getUniversityName());
        dto.setFieldOfStudy(p.getFieldOfStudy());
        dto.setFocusArea(p.getFocusArea());
        dto.setPortal(p.getPortal());
        dto.setWebsite(p.getWebsite());
        dto.setDeadline(p.getDeadline());
        dto.setStatus(p.getStatus() != null ? p.getStatus().getDisplayName() : null);
        dto.setTuition(p.getTuition());
        dto.setRequirements(p.getRequirements());
        dto.setNotes(p.getNotes());
        return dto;
    }

    /**
     * Create a new Program for the given user.
     *
     * Basic field validation is handled by DTO annotations via @Valid in
     * controller.
     * This method enforces business rules like user existence.
     *
     * @param userId id of the owner user; must exist
     * @param dto    create payload; validated by @Valid in controller
     * @return the newly created Program as a ProgramDTO
     * @throws NotFoundException when the provided userId does not exist
     */
    @Transactional
    public ProgramDTO createProgram(Integer userId, ProgramCreateDTO dto) {
        if (userId == null) {
            throw new UnauthorizedException("missing or invalid token");
        }

        Optional<User> uOpt = userRepository.findById(userId);
        if (uOpt.isEmpty()) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        User user = uOpt.get();
        Program p = new Program();
        p.setUser(user);
        p.setUniversityName(dto.getUniversityName());
        p.setFieldOfStudy(dto.getFieldOfStudy());
        p.setFocusArea(dto.getFocusArea());
        p.setPortal(dto.getPortal());
        p.setWebsite(dto.getWebsite());
        p.setDeadline(dto.getDeadline());
        p.setStatus(ApplicationStatus.fromString(dto.getStatus()));
        p.setTuition(dto.getTuition());
        p.setRequirements(dto.getRequirements());
        p.setNotes(dto.getNotes());

        Program saved = programRepository.save(p);
        return toDto(saved);
    }

    /**
     * Retrieve all programs that belong to a given user.
     *
     * @param userId owner id whose programs will be returned
     * @return list of ProgramDTO for the given user
     * @throws NotFoundException when the userId does not exist
     */
    @Transactional(readOnly = true)
    public List<ProgramDTO> getAllProgramsByUser(Integer userId) {
        if (userId == null) {
            throw new UnauthorizedException("missing or invalid token");
        }
        // verify user exists
        Optional<User> uOpt = userRepository.findById(userId);
        if (uOpt.isEmpty()) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        List<Program> list = programRepository.findAllByUserUserId(userId);
        List<ProgramDTO> result = new ArrayList<>();

        for (Program p : list) {
            result.add(toDto(p));
        }

        return result;
    }

    /**
     * Retrieve programs that belong to a given user with pagination support.
     *
     * @param userId   owner id whose programs will be returned
     * @param pageable pagination parameters (page number, size, sorting)
     * @return Page of ProgramDTO for the given user
     * @throws NotFoundException when the userId does not exist
     */
    @Transactional(readOnly = true)
    public Page<ProgramDTO> getAllProgramsByUser(Integer userId, Pageable pageable) {
        if (userId == null) {
            throw new UnauthorizedException("missing or invalid token");
        }
        // verify user exists
        Optional<User> uOpt = userRepository.findById(userId);
        if (uOpt.isEmpty()) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        Page<Program> page = programRepository.findAllByUserUserId(userId, pageable);
        return page.map(this::toDto);
    }

    /**
     * Retrieve a single Program for a user, enforcing ownership.
     *
     * @param userId    owner id used for ownership verification
     * @param programId id of the program to return
     * @return ProgramDTO for the requested program
     * @throws NotFoundException when the program does not exist or is not owned by
     *                           the user
     */
    @Transactional(readOnly = true)
    public ProgramDTO getProgramById(Integer userId, Integer programId) {
        if (userId == null) {
            throw new UnauthorizedException("missing or invalid token");
        }
        Optional<Program> prog = programRepository.findByProgramIdAndUserUserId(programId, userId);
        if (prog.isEmpty()) {
            throw new NotFoundException("Program not found with id: " + programId);
        }
        return toDto(prog.get());
    }

    /**
     * Filter programs using JPA Specifications for database-level filtering.
     * This is more efficient for large datasets as filtering occurs in the
     * database.
     * Accepts a map where keys are field names and values are filter values.
     *
     * @param userId  owner id whose programs will be filtered
     * @param filters map of field names to filter values (case-insensitive contains
     *                for strings, exact match for dates)
     * @return list of ProgramDTO matching the filters
     * @throws UnauthorizedException when userId is null
     * @throws NotFoundException     when the userId does not exist
     */
    @Transactional(readOnly = true)
    public List<ProgramDTO> filterPrograms(Integer userId, Map<String, Object> filters) {
        if (userId == null) {
            throw new UnauthorizedException("missing or invalid token");
        }
        // verify user exists
        Optional<User> userTemp = userRepository.findById(userId);
        if (userTemp.isEmpty()) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        // Build specification from filters
        Specification<Program> spec = buildFilterSpecification(userId, filters);

        // Execute query with specification
        List<Program> filteredPrograms = programRepository.findAll(spec);

        // Convert to DTOs
        List<ProgramDTO> dtoList = new ArrayList<>();
        for (Program p : filteredPrograms) {
            dtoList.add(toDto(p));
        }

        return dtoList;
    }

    /**
     * Build a JPA Specification from the given filters.
     * All filters are combined with AND logic.
     *
     * @param userId  owner id to filter by
     * @param filters map of field names to filter values
     * @return Specification combining all filters
     */
    private Specification<Program> buildFilterSpecification(Integer userId, Map<String, Object> filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by userId
            predicates.add(criteriaBuilder.equal(root.get("user").get("userId"), userId));

            // Apply additional filters if provided
            if (filters != null && !filters.isEmpty()) {
                for (Map.Entry<String, Object> entry : filters.entrySet()) {
                    String fieldName = entry.getKey();
                    Object filterValue = entry.getValue();

                    // Skip null or blank filters
                    if (filterValue == null || filterValue.toString().isBlank()) {
                        continue;
                    }

                    Predicate predicate = createPredicate(root, criteriaBuilder, fieldName, filterValue.toString());
                    if (predicate != null) {
                        predicates.add(predicate);
                    }
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Create a predicate for a specific field and filter value.
     * String fields use case-insensitive LIKE matching.
     * Date fields use exact equality matching.
     *
     * @param root            the root entity
     * @param criteriaBuilder the criteria builder
     * @param fieldName       the field to filter on
     * @param filterValue     the value to filter by
     * @return a Predicate for the field, or null if the field is unknown or invalid
     */
    private Predicate createPredicate(jakarta.persistence.criteria.Root<Program> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            String fieldName, String filterValue) {
        switch (fieldName) {
            case "universityName":
            case "fieldOfStudy":
            case "focusArea":
            case "status":
            case "portal":
            case "website":
            case "tuition":
            case "requirements":
                // Case-insensitive LIKE match for string fields
                return criteriaBuilder.like(
                        criteriaBuilder.lower(root.get(fieldName)),
                        "%" + filterValue.toLowerCase() + "%");
            case "deadline":
                // Exact match for date fields
                try {
                    LocalDate filterDate = LocalDate.parse(filterValue);
                    return criteriaBuilder.equal(root.get("deadline"), filterDate);
                } catch (DateTimeParseException e) {
                    // Invalid date format, skip this filter
                    return null;
                }
            default:
                // Unknown field, skip
                return null;
        }
    }

    /**
     * Update a Program that belongs to the given user.
     *
     * Basic field validation is handled by DTO annotations via @Valid in
     * controller.
     * This method enforces business rules like ownership.
     *
     * @param userId    owner id used for ownership enforcement
     * @param programId id of the program to update
     * @param dto       update payload; validated by @Valid in controller
     * @return the updated Program as ProgramDTO
     * @throws NotFoundException when the program is not found or is not owned by
     *                           the user
     */
    @Transactional
    public ProgramDTO updateProgram(Integer userId, Integer programId, ProgramUpdateDTO dto) {
        if (userId == null) {
            throw new UnauthorizedException("missing or invalid token");
        }

        Optional<Program> prog = programRepository.findByProgramIdAndUserUserId(programId, userId);
        if (prog.isEmpty()) {
            throw new NotFoundException("Program not found with id: " + programId);
        }

        Program p = prog.get();
        p.setUniversityName(dto.getUniversityName());
        p.setFieldOfStudy(dto.getFieldOfStudy());
        p.setFocusArea(dto.getFocusArea());
        p.setPortal(dto.getPortal());
        p.setWebsite(dto.getWebsite());
        p.setDeadline(dto.getDeadline());
        p.setStatus(ApplicationStatus.fromString(dto.getStatus()));
        p.setTuition(dto.getTuition());
        p.setRequirements(dto.getRequirements());
        p.setNotes(dto.getNotes());

        Program saved = programRepository.save(p);
        return toDto(saved);
    }

    /**
     * Delete a program that belongs to the given user.
     *
     * @param userId    owner id used for ownership enforcement
     * @param programId id of the program to delete
     * @throws NotFoundException  when the program is not found
     * @throws ForbiddenException when the program exists but is not owned by the
     *                            user
     */
    @Transactional
    public void deleteProgram(Integer userId, Integer programId) {
        if (userId == null) {
            throw new UnauthorizedException("missing or invalid token");
        }
        // First verify the program exists
        Optional<Program> existing = programRepository.findById(programId);
        if (existing.isEmpty()) {
            throw new NotFoundException("Program not found with id: " + programId);
        }

        Program p = existing.get();
        // Ownership check: if the program exists but belongs to another user =>
        // Forbidden
        if (p.getUser() == null || p.getUser().getUserId() == null || !p.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("not authorized to delete this program");
        }

        programRepository.deleteById(programId);
    }

    /**
     * Get aggregated dashboard statistics for a user's programs.
     * 
     * @param userId the ID of the user
     * @return DashboardStatsDTO containing program statistics
     */
    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(Integer userId) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        // Get all programs for the user
        List<Program> programs = programRepository.findAllByUserUserId(userId);

        // Calculate total programs
        int totalPrograms = programs.size();

        // Calculate total documents for the user
        long totalDocuments = documentRepository.findByUserUserId(userId).size();

        // Count programs by status
        Map<String, Integer> statusCounts = new HashMap<>();
        for (Program program : programs) {
            String status = program.getStatus() != null ? program.getStatus().getDisplayName() : "Other";
            statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
        }

        return new DashboardStatsDTO(totalPrograms, (int) totalDocuments, statusCounts);
    }
}
