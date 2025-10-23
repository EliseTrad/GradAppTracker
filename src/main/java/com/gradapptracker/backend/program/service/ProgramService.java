package com.gradapptracker.backend.program.service;

import com.gradapptracker.backend.program.dto.ProgramCreateDTO;
import com.gradapptracker.backend.program.dto.ProgramDTO;
import com.gradapptracker.backend.program.dto.ProgramUpdateDTO;
import com.gradapptracker.backend.program.entity.Program;
import com.gradapptracker.backend.program.repository.ProgramRepository;
import com.gradapptracker.backend.user.entity.User;
import com.gradapptracker.backend.user.repository.UserRepository;
import com.gradapptracker.backend.exception.ForbiddenException;
import com.gradapptracker.backend.exception.NotFoundException;
import com.gradapptracker.backend.exception.ValidationException;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public ProgramService(ProgramRepository programRepository, UserRepository userRepository) {
        this.programRepository = programRepository;
        this.userRepository = userRepository;
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
        dto.setStatus(p.getStatus());
        dto.setTuition(p.getTuition());
        dto.setRequirements(p.getRequirements());
        dto.setNotes(p.getNotes());
        return dto;
    }

    /**
     * Create a new Program for the given user.
     *
     * @param userId id of the owner user; must exist
     * @param dto    create payload; must contain a non-blank universityName
     * @return the newly created Program as a ProgramDTO
     * @throws ValidationException when dto is null or required fields are missing
     * @throws NotFoundException   when the provided userId does not exist
     */
    @Transactional
    public ProgramDTO createProgram(Integer userId, ProgramCreateDTO dto) {
        if (userId == null) {
            throw new com.gradapptracker.backend.exception.UnauthorizedException("missing or invalid token");
        }
        if (dto == null)
            throw new ValidationException("request body is required");
        if (dto.getUniversityName() == null || dto.getUniversityName().isBlank())
            throw new ValidationException("universityName is required");

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
        p.setStatus(dto.getStatus());
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
            throw new com.gradapptracker.backend.exception.UnauthorizedException("missing or invalid token");
        }
        // verify user exists
        Optional<User> uOpt = userRepository.findById(userId);
        if (uOpt.isEmpty()) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        List<Program> list = programRepository.findAllByUserUserId(userId);
        return list.stream().map(this::toDto).collect(Collectors.toList());
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
            throw new com.gradapptracker.backend.exception.UnauthorizedException("missing or invalid token");
        }
        Optional<Program> pOpt = programRepository.findByProgramIdAndUserUserId(programId, userId);
        if (pOpt.isEmpty()) {
            throw new NotFoundException("Program not found with id: " + programId);
        }
        return toDto(pOpt.get());
    }

    /**
     * Smart filter using Specifications. Accepts a map where keys are field
     * names and values are filter values. String fields are matched
     * case-insensitively using LIKE %value% semantics. Deadline accepts
     * ISO date strings (yyyy-MM-dd) or LocalDate instances for exact match.
     */
    @Transactional(readOnly = true)
    public List<ProgramDTO> filterPrograms(Integer userId, Map<String, Object> filters) {
        if (userId == null) {
            throw new com.gradapptracker.backend.exception.UnauthorizedException("missing or invalid token");
        }
        // verify user exists
        Optional<User> uOpt = userRepository.findById(userId);
        if (uOpt.isEmpty()) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        Specification<Program> spec = buildSpecification(userId, filters);
        List<Program> result = programRepository.findAll(spec);
        return result.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Build a JPA Specification used by filterPrograms.
     * The resulting specification includes an ownership predicate (user.userId =
     * userId)
     * and additional predicates based on the provided filters map.
     *
     * Behavior changes:
     * - For string fields (universityName, fieldOfStudy, focusArea, status,
     * portal, website, tuition, requirements) a row will only match when the
     * corresponding column IS NOT NULL and the column (lowercased) contains the
     * provided filter value (case-insensitive LIKE %%value%%). This ensures rows
     * with NULL values for that column are not returned when searching for a
     * non-empty value.
     * - For deadline the filter requires an exact ISO date string in yyyy-MM-dd
     * format; the predicate uses exact LocalDate equality. If the provided
     * deadline cannot be parsed as yyyy-MM-dd a ValidationException is thrown.
     *
     * Unknown keys are ignored.
     *
     * @param userId  owner id to restrict results to
     * @param filters map of filterName -> value
     * @return JPA Specification representing the composed filters
     */
    private Specification<Program> buildSpecification(Integer userId, Map<String, Object> filters) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            // ownership predicate
            preds.add(cb.equal(root.get("user").get("userId"), userId));

            if (filters == null || filters.isEmpty()) {
                return cb.and(preds.toArray(new Predicate[0]));
            }

            CriteriaBuilder cbuilder = cb;

            for (Map.Entry<String, Object> e : filters.entrySet()) {
                String key = e.getKey();
                Object raw = e.getValue();
                if (raw == null)
                    continue;

                // skip blank string filters
                if (raw instanceof CharSequence && raw.toString().isBlank())
                    continue;

                switch (key) {
                    case "universityName":
                    case "fieldOfStudy":
                    case "focusArea":
                    case "status":
                    case "portal":
                    case "website":
                    case "tuition":
                    case "requirements": {
                        String s = raw.toString().toLowerCase();
                        Path<String> path = root.get(camelToFieldName(key));
                        // ensure column is not NULL and then perform case-insensitive LIKE
                        preds.add(cbuilder.isNotNull(path));
                        preds.add(cbuilder.like(cbuilder.lower(path), "%" + escapeLike(s) + "%"));
                        break;
                    }
                    case "deadline": {
                        LocalDate d;
                        if (raw instanceof LocalDate) {
                            d = (LocalDate) raw;
                        } else {
                            try {
                                // enforce yyyy-MM-dd parsing (ISO_LOCAL_DATE)
                                d = LocalDate.parse(raw.toString());
                            } catch (DateTimeParseException ex) {
                                throw new ValidationException("deadline must be a date in yyyy-MM-dd format");
                            }
                        }
                        // require exact equality; implicit null-check (NULL != d)
                        preds.add(cbuilder.equal(root.get("deadline"), d));
                        break;
                    }
                    default:
                        // ignore unknown keys
                        break;
                }
            }

            return cb.and(preds.toArray(new Predicate[0]));
        };
    }

    /**
     * Convert a camelCase filter key to the matching entity field name.
     * Currently performs a direct mapping since entity fields use the same names.
     *
     * @param key camelCase filter key
     * @return entity field name to use in criteria paths
     */
    private static String camelToFieldName(String key) {
        // our DTO keys use camelCase and entity fields match those names
        return key;
    }

    /**
     * Escape characters that have special meaning in SQL LIKE patterns.
     *
     * @param s input string
     * @return escaped string safe to use inside a LIKE '%...%' predicate
     */
    private static String escapeLike(String s) {
        // basic escape for SQL like wildcard characters
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * Update a Program that belongs to the given user.
     *
     * @param userId    owner id used for ownership enforcement
     * @param programId id of the program to update
     * @param dto       update payload; must include universityName
     * @return the updated Program as ProgramDTO
     * @throws ValidationException when dto is null or required fields are missing
     * @throws NotFoundException   when the program is not found or is not owned by
     *                             the user
     */
    @Transactional
    public ProgramDTO updateProgram(Integer userId, Integer programId, ProgramUpdateDTO dto) {
        if (userId == null) {
            throw new com.gradapptracker.backend.exception.UnauthorizedException("missing or invalid token");
        }
        if (dto == null)
            throw new ValidationException("request body is required");
        if (dto.getUniversityName() == null || dto.getUniversityName().isBlank())
            throw new ValidationException("universityName is required");

        Optional<Program> pOpt = programRepository.findByProgramIdAndUserUserId(programId, userId);
        if (pOpt.isEmpty()) {
            throw new NotFoundException("Program not found with id: " + programId);
        }

        Program p = pOpt.get();
        p.setUniversityName(dto.getUniversityName());
        p.setFieldOfStudy(dto.getFieldOfStudy());
        p.setFocusArea(dto.getFocusArea());
        p.setPortal(dto.getPortal());
        p.setWebsite(dto.getWebsite());
        p.setDeadline(dto.getDeadline());
        p.setStatus(dto.getStatus());
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
            throw new com.gradapptracker.backend.exception.UnauthorizedException("missing or invalid token");
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
}

