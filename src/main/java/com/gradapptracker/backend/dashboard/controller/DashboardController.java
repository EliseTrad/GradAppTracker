package com.gradapptracker.backend.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gradapptracker.backend.program.service.ProgramService;
import com.gradapptracker.backend.shared.dto.DashboardStatsDTO;
import com.gradapptracker.backend.security.JwtUtils;
import com.gradapptracker.backend.exception.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * REST Controller for dashboard-related endpoints.
 * Provides aggregated statistics for 3D visualization.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ProgramService programService;
    private final JwtUtils jwtUtils;

    public DashboardController(ProgramService programService, JwtUtils jwtUtils) {
        this.programService = programService;
        this.jwtUtils = jwtUtils;
    }

    private Integer extractUserId(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : auth;
        return jwtUtils.getUserIdFromToken(token);
    }

    /**
     * Get dashboard statistics for the authenticated user.
     * Returns aggregated data including total programs, documents, active
     * applications,
     * and program status counts for 3D visualization.
     * 
     * @return DashboardStatsDTO containing aggregated statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats(HttpServletRequest req) {
        Integer userId = extractUserId(req);
        if (userId == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        DashboardStatsDTO stats = programService.getDashboardStats(userId);
        return ResponseEntity.ok(stats);
    }
}
