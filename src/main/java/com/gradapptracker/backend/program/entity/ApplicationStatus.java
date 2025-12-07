package com.gradapptracker.backend.program.entity;

/**
 * Enumeration for program application status.
 * Provides type-safe status values and ensures consistency across the
 * application.
 */
public enum ApplicationStatus {
    ACCEPTED("Accepted"),
    APPLIED("Applied"),
    IN_PROGRESS("In Progress"),
    REJECTED("Rejected"),
    OTHER("Other");

    private final String displayName;

    ApplicationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse a string value to ApplicationStatus enum.
     * Case-insensitive and handles display names.
     * 
     * @param value the string value to parse
     * @return matching ApplicationStatus or OTHER if not found
     */
    public static ApplicationStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return OTHER;
        }

        String normalized = value.trim().toLowerCase();

        for (ApplicationStatus status : ApplicationStatus.values()) {
            if (status.name().toLowerCase().equals(normalized) ||
                    status.displayName.toLowerCase().equals(normalized)) {
                return status;
            }
        }

        return OTHER;
    }
}
