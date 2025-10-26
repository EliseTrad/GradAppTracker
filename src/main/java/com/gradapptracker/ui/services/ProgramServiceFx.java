package com.gradapptracker.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gradapptracker.backend.program.dto.ProgramCreateDTO;
import com.gradapptracker.backend.program.dto.ProgramDTO;
import com.gradapptracker.backend.program.dto.ProgramUpdateDTO;

import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * UI-facing service for working with Programs via backend API.
 */
public class ProgramServiceFx extends ApiClient {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final String basePath = "/programs";

    public ProgramServiceFx() {
        super();
    }

    public ProgramDTO createProgram(ProgramCreateDTO dto) {
        try {
            String json = mapper.writeValueAsString(dto);
            String jwt = com.gradapptracker.ui.utils.UserSession.getInstance().getJwt();

            // DIAGNOSTIC LOGGING - CREATE PROGRAM
            System.out.println("=== CREATE PROGRAM REQUEST ===");
            System.out.println("Endpoint URL: " + baseUrl + basePath);
            System.out.println("HTTP Method: POST");
            System.out.println("Authorization Header: Bearer "
                    + (jwt != null ? jwt.substring(0, Math.min(jwt.length(), 20)) + "..." : "null"));
            System.out.println("JSON Body: " + json);

            HttpResponse<String> resp = POST(basePath, json, true);

            // DIAGNOSTIC LOGGING - CREATE PROGRAM RESPONSE
            System.out.println("=== CREATE PROGRAM RESPONSE ===");
            System.out.println("Status Code: " + resp.statusCode());
            System.out.println("Raw Response Body: " + resp.body());

            if (resp.statusCode() / 100 == 2) {
                System.out.println("SUCCESS: Program created successfully");
                return mapper.readValue(resp.body(), ProgramDTO.class);
            }

            System.out.println("ERROR: Create failed with status " + resp.statusCode());
            throw new RuntimeException("Create failed: " + resp.body());
        } catch (Exception e) {
            System.out.println("EXCEPTION in createProgram: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Create program error", e);
        }
    }

    public List<ProgramDTO> getAllPrograms() {
        try {
            HttpResponse<String> resp = GET(basePath, true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), new TypeReference<List<ProgramDTO>>() {
                });
            }
            throw new RuntimeException("Get all programs failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException("Get all programs error", e);
        }
    }

    public ProgramDTO getProgramById(int programId) {
        try {
            HttpResponse<String> resp = GET(basePath + "/" + programId, true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), ProgramDTO.class);
            }
            throw new RuntimeException("Get program failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException("Get program error", e);
        }
    }

    public ProgramDTO updateProgram(int programId, ProgramUpdateDTO dto) {
        try {
            String json = mapper.writeValueAsString(dto);
            String jwt = com.gradapptracker.ui.utils.UserSession.getInstance().getJwt();

            // DIAGNOSTIC LOGGING - UPDATE PROGRAM
            System.out.println("=== UPDATE PROGRAM REQUEST ===");
            System.out.println("Endpoint URL: " + baseUrl + basePath + "/" + programId);
            System.out.println("HTTP Method: PUT");
            System.out.println("Authorization Header: Bearer "
                    + (jwt != null ? jwt.substring(0, Math.min(jwt.length(), 20)) + "..." : "null"));
            System.out.println("JSON Body: " + json);

            HttpResponse<String> resp = PUT(basePath + "/" + programId, json, true);

            // DIAGNOSTIC LOGGING - UPDATE PROGRAM RESPONSE
            System.out.println("=== UPDATE PROGRAM RESPONSE ===");
            System.out.println("Status Code: " + resp.statusCode());
            System.out.println("Raw Response Body: " + resp.body());

            if (resp.statusCode() / 100 == 2) {
                System.out.println("SUCCESS: Program updated successfully");
                return mapper.readValue(resp.body(), ProgramDTO.class);
            }

            System.out.println("ERROR: Update failed with status " + resp.statusCode());
            throw new RuntimeException("Update failed: " + resp.body());
        } catch (Exception e) {
            System.out.println("EXCEPTION in updateProgram: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Update program error", e);
        }
    }

    public void deleteProgram(int programId) {
        try {
            HttpResponse<String> resp = DELETE(basePath + "/" + programId, true);
            if (resp.statusCode() / 100 != 2) {
                throw new RuntimeException("Delete failed: " + resp.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Delete program error", e);
        }
    }

    public List<ProgramDTO> filterPrograms(Map<String, Object> filters) {
        try {
            // Build query string from filters with proper URL encoding
            StringBuilder query = new StringBuilder(basePath).append("/filter");
            if (!filters.isEmpty()) {
                query.append("?");
                filters.forEach((key, value) -> {
                    try {
                        String encodedKey = java.net.URLEncoder.encode(key, "UTF-8");
                        String encodedValue = java.net.URLEncoder.encode(value.toString(), "UTF-8");
                        query.append(encodedKey).append("=").append(encodedValue).append("&");
                    } catch (java.io.UnsupportedEncodingException e) {
                        throw new RuntimeException("URL encoding error", e);
                    }
                });
                // Remove trailing &
                if (query.charAt(query.length() - 1) == '&') {
                    query.setLength(query.length() - 1);
                }
            }

            HttpResponse<String> resp = GET(query.toString(), true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), new TypeReference<List<ProgramDTO>>() {
                });
            }
            return Collections.emptyList(); // safer than throwing for empty filter
        } catch (Exception e) {
            throw new RuntimeException("Filter programs error", e);
        }
    }
}
