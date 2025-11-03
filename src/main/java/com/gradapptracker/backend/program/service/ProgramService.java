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
import com.gradapptracker.backend.exception.UnauthorizedException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Filter programs using simple in-memory filtering. Much simpler than JPA
     * Specifications.
     * Accepts a map where keys are field names and values are filter values.
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

        // Get all programs for the user first
        List<Program> allPrograms = programRepository.findAllByUserUserId(userId);

        // If no filters, return all programs
        if (filters == null || filters.isEmpty()) {
            List<ProgramDTO> dtoList = new ArrayList<>();
            for (Program p : allPrograms) {
                dtoList.add(toDto(p));
            }
            return dtoList;
        }

        // Apply filters in memory
        List<Program> filteredPrograms = new ArrayList<>();
        for (Program p : allPrograms) {
            if (matchesFilters(p, filters)) {
                filteredPrograms.add(p);
            }
        }

        // Convert filtered programs to DTOs
        List<ProgramDTO> dtoList = new ArrayList<>();
        for (Program p : filteredPrograms) {
            dtoList.add(toDto(p));
        }

        return dtoList;
    }

    /**
     * Check if a program matches the given filters.
     */
    private boolean matchesFilters(Program program, Map<String, Object> filters) {
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object filterValue = entry.getValue();

            if (filterValue == null || filterValue.toString().isBlank()) {
                continue; // Skip empty filters
            }

            if (!matchesFilter(program, key, filterValue.toString())) {
                return false; // If any filter doesn't match, exclude this program
            }
        }
        return true; // All filters matched
    }

    /**
     * Check if a program field matches a specific filter value.
     */
    private boolean matchesFilter(Program program, String fieldName, String filterValue) {
        String lowerFilterValue = filterValue.toLowerCase();

        switch (fieldName) {
            case "universityName":
                return program.getUniversityName() != null &&
                        program.getUniversityName().toLowerCase().contains(lowerFilterValue);
            case "fieldOfStudy":
                return program.getFieldOfStudy() != null &&
                        program.getFieldOfStudy().toLowerCase().contains(lowerFilterValue);
            case "focusArea":
                return program.getFocusArea() != null &&
                        program.getFocusArea().toLowerCase().contains(lowerFilterValue);
            case "status":
                return program.getStatus() != null &&
                        program.getStatus().toLowerCase().contains(lowerFilterValue);
            case "portal":
                return program.getPortal() != null &&
                        program.getPortal().toLowerCase().contains(lowerFilterValue);
            case "website":
                return program.getWebsite() != null &&
                        program.getWebsite().toLowerCase().contains(lowerFilterValue);
            case "tuition":
                return program.getTuition() != null &&
                        program.getTuition().toLowerCase().contains(lowerFilterValue);
            case "requirements":
                return program.getRequirements() != null &&
                        program.getRequirements().toLowerCase().contains(lowerFilterValue);
            case "deadline":
                try {
                    LocalDate filterDate = LocalDate.parse(filterValue);
                    return filterDate.equals(program.getDeadline());
                } catch (DateTimeParseException e) {
                    return false; // Invalid date format, no match
                }
            default:
                return true; // Unknown field, don't filter
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
}
