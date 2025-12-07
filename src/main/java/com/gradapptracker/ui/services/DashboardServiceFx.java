package com.gradapptracker.ui.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradapptracker.backend.shared.dto.DashboardStatsDTO;

import java.net.http.HttpResponse;

/**
 * Frontend service for dashboard-related API calls.
 * Fetches aggregated statistics for 3D visualization.
 */
public class DashboardServiceFx extends ApiClient {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Retrieve dashboard statistics for the authenticated user.
     * 
     * @return DashboardStatsDTO containing program statistics
     * @throws Exception if the request fails or response cannot be parsed
     */
    public DashboardStatsDTO getDashboardStats() throws Exception {
        HttpResponse<String> response = GET("/dashboard/stats", true);

        if (response.statusCode() == 200) {
            return mapper.readValue(response.body(), DashboardStatsDTO.class);
        } else {
            throw new RuntimeException(
                    "Failed to fetch dashboard stats: " + response.statusCode() + " - " + response.body());
        }
    }
}
