package com.gradapptracker.backend.program.controller;

import com.gradapptracker.backend.program.dto.ProgramCreateDTO;
import com.gradapptracker.backend.program.dto.ProgramDTO;
import com.gradapptracker.backend.program.dto.ProgramUpdateDTO;
import com.gradapptracker.backend.program.service.ProgramService;
import com.gradapptracker.backend.security.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Program endpoints (base path: /api/programs).
 *
 * All endpoints require a Bearer JWT in the Authorization header. The
 * controller extracts the authenticated user's id from the token and passes it
 * to the service. Business logic and error handling remain in the service and
 * global exception handler respectively.
 */
@RestController
@RequestMapping("/api/programs")
@Validated
public class ProgramController {

    private final ProgramService programService;
    private final JwtUtils jwtUtils;

    public ProgramController(ProgramService programService, JwtUtils jwtUtils) {
        this.programService = programService;
        this.jwtUtils = jwtUtils;
    }

    private Integer extractUserId(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : auth;
        return jwtUtils.getUserIdFromToken(token);
    }

    /**
     * Create a program for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<ProgramDTO> createProgram(HttpServletRequest req, @RequestBody @Valid ProgramCreateDTO dto) {
        Integer userId = extractUserId(req);
        ProgramDTO created = programService.createProgram(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get all programs for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ProgramDTO>> getAllPrograms(HttpServletRequest req) {
        Integer userId = extractUserId(req);
        List<ProgramDTO> list = programService.getAllProgramsByUser(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * Get a single program by id for the authenticated user.
     */
    @GetMapping("/{programId}")
    public ResponseEntity<ProgramDTO> getProgram(HttpServletRequest req, @PathVariable Integer programId) {
        Integer userId = extractUserId(req);
        ProgramDTO dto = programService.getProgramById(userId, programId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Filter programs for the authenticated user. Query parameters are forwarded
     * directly to the service as filter keys.
     */
    @GetMapping("/filter")
    public ResponseEntity<List<ProgramDTO>> filterPrograms(HttpServletRequest req,
            @RequestParam Map<String, String> allParams) {
        Integer userId = extractUserId(req);
        Map<String, Object> filters = new HashMap<>();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            filters.put(entry.getKey(), entry.getValue());
        }
        List<ProgramDTO> result = programService.filterPrograms(userId, filters);
        return ResponseEntity.ok(result);
    }

    /**
     * Update a program for the authenticated user.
     */
    @PutMapping("/{programId}")
    public ResponseEntity<ProgramDTO> updateProgram(HttpServletRequest req, @PathVariable Integer programId,
            @RequestBody @Valid ProgramUpdateDTO dto) {
        Integer userId = extractUserId(req);
        ProgramDTO updated = programService.updateProgram(userId, programId, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a program for the authenticated user.
     */
    @DeleteMapping("/{programId}")
    public ResponseEntity<Void> deleteProgram(HttpServletRequest req, @PathVariable Integer programId) {
        Integer userId = extractUserId(req);
        programService.deleteProgram(userId, programId);
        return ResponseEntity.noContent().build();
    }

}
