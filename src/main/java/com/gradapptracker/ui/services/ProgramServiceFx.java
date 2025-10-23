package com.gradapptracker.ui.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ObjectMapper mapper = new ObjectMapper();
    private final String basePath = "/programs";

    public ProgramServiceFx() {
        super();
    }

    public ProgramDTO createProgram(ProgramCreateDTO dto) {
        try {
            String json = mapper.writeValueAsString(dto);
            HttpResponse<String> resp = POST(basePath, json, true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), ProgramDTO.class);
            }
            throw new RuntimeException("Create failed: " + resp.body());
        } catch (Exception e) {
            throw new RuntimeException("Create program error", e);
        }
    }

    public List<ProgramDTO> getAllPrograms() {
        try {
            HttpResponse<String> resp = GET(basePath, true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), new TypeReference<List<ProgramDTO>>() {});
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
            HttpResponse<String> resp = PUT(basePath + "/" + programId, json, true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), ProgramDTO.class);
            }
            throw new RuntimeException("Update failed: " + resp.body());
        } catch (Exception e) {
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
            String json = mapper.writeValueAsString(filters);
            HttpResponse<String> resp = POST(basePath + "/filter", json, true);
            if (resp.statusCode() / 100 == 2) {
                return mapper.readValue(resp.body(), new TypeReference<List<ProgramDTO>>() {});
            }
            return Collections.emptyList(); // safer than throwing for empty filter
        } catch (Exception e) {
            throw new RuntimeException("Filter programs error", e);
        }
    }
}
