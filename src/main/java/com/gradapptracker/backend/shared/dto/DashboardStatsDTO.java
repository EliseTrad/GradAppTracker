package com.gradapptracker.backend.shared.dto;

import java.util.Map;

/**
 * DTO for dashboard statistics.
 * Contains aggregated data for 3D visualization.
 */
public class DashboardStatsDTO {

    private int totalPrograms;
    private int totalDocuments;
    private Map<String, Integer> statusCounts;

    public DashboardStatsDTO() {
    }

    public DashboardStatsDTO(int totalPrograms, int totalDocuments, Map<String, Integer> statusCounts) {
        this.totalPrograms = totalPrograms;
        this.totalDocuments = totalDocuments;
        this.statusCounts = statusCounts;
    }

    public int getTotalPrograms() {
        return totalPrograms;
    }

    public void setTotalPrograms(int totalPrograms) {
        this.totalPrograms = totalPrograms;
    }

    public int getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(int totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public Map<String, Integer> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<String, Integer> statusCounts) {
        this.statusCounts = statusCounts;
    }
}
