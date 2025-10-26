package com.gradapptracker.ui.services;

import com.gradapptracker.ui.utils.UserSession;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;

/**
 * Base helper for HTTP calls from the JavaFX UI to the Spring backend.
 * <p>
 * Provides convenience wrappers around java.net.http.HttpClient for common
 * HTTP verbs and adds Authorization header when requested.
 */
public class ApiClient {

    protected final String baseUrl = "http://localhost:8080/api";
    protected final HttpClient client;

    protected ApiClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Build a request builder for the given path (concatenated with baseUrl).
     *
     * @param path relative path, must start with '/'
     * @return HttpRequest.Builder initialized with the target URI
     */
    protected HttpRequest.Builder request(String path) {
        String uri = baseUrl + path;
        return HttpRequest.newBuilder(URI.create(uri));
    }

    protected HttpResponse<String> GET(String path, boolean auth) {
        try {
            HttpRequest.Builder b = request(path).GET();
            if (auth) {
                String token = UserSession.getInstance().getJwt();
                if (token != null) {
                    b.header("Authorization", "Bearer " + token);
                }
            }
            HttpRequest req = b.build();
            return client.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    protected HttpResponse<String> POST(String path, String jsonBody, boolean auth) {
        try {
            HttpRequest.Builder b = request(path)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("Content-Type", "application/json");
            if (auth) {
                String token = UserSession.getInstance().getJwt();
                if (token != null) {
                    b.header("Authorization", "Bearer " + token);
                }
            }
            HttpRequest req = b.build();
            return client.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    protected HttpResponse<String> PUT(String path, String jsonBody, boolean auth) {
        try {
            HttpRequest.Builder b = request(path)
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("Content-Type", "application/json");
            if (auth) {
                String token = UserSession.getInstance().getJwt();
                if (token != null) {
                    b.header("Authorization", "Bearer " + token);
                }
            }
            HttpRequest req = b.build();
            return client.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    protected HttpResponse<String> DELETE(String path, boolean auth) {
        try {
            HttpRequest.Builder b = request(path).DELETE();
            if (auth) {
                String token = UserSession.getInstance().getJwt();
                if (token != null) {
                    b.header("Authorization", "Bearer " + token);
                }
            }
            HttpRequest req = b.build();
            return client.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    protected HttpResponse<String> POST_MULTIPART(String path, java.io.File file, String docType, String notes,
            boolean auth) {
        try {
            String boundary = "----FormBoundary" + System.currentTimeMillis();

            // Use proper multipart body with binary file handling
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder(boundary);
            bodyBuilder.addFilePart("file", file);
            bodyBuilder.addTextPart("docType", docType);
            if (notes != null && !notes.trim().isEmpty()) {
                bodyBuilder.addTextPart("notes", notes);
            }

            HttpRequest.Builder b = request(path)
                    .POST(bodyBuilder.build())
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary);
            if (auth) {
                String token = UserSession.getInstance().getJwt();
                if (token != null) {
                    b.header("Authorization", "Bearer " + token);
                }
            }
            HttpRequest req = b.build();
            return client.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    protected HttpResponse<String> PUT_MULTIPART(String path, java.io.File file, boolean auth) {
        try {
            String boundary = "----FormBoundary" + System.currentTimeMillis();

            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder(boundary);
            bodyBuilder.addFilePart("file", file);

            HttpRequest.Builder b = request(path)
                    .PUT(bodyBuilder.build())
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary);
            if (auth) {
                String token = UserSession.getInstance().getJwt();
                if (token != null) {
                    b.header("Authorization", "Bearer " + token);
                }
            }
            HttpRequest req = b.build();
            return client.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    protected HttpResponse<byte[]> GET_BYTES(String path, boolean auth) {
        try {
            HttpRequest.Builder b = request(path).GET();
            if (auth) {
                String token = UserSession.getInstance().getJwt();
                if (token != null) {
                    b.header("Authorization", "Bearer " + token);
                }
            }
            HttpRequest req = b.build();
            return client.send(req, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper class to build proper multipart request bodies that preserve binary
     * data
     */
    private static class MultipartBodyBuilder {
        private final String boundary;
        private final java.io.ByteArrayOutputStream outputStream;
        private final String CRLF = "\r\n";

        public MultipartBodyBuilder(String boundary) {
            this.boundary = boundary;
            this.outputStream = new java.io.ByteArrayOutputStream();
        }

        public void addTextPart(String name, String value) throws IOException {
            outputStream.write(("--" + boundary + CRLF).getBytes());
            outputStream.write(("Content-Disposition: form-data; name=\"" + name + "\"" + CRLF).getBytes());
            outputStream.write((CRLF).getBytes());
            outputStream.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            outputStream.write((CRLF).getBytes());
        }

        public void addFilePart(String name, java.io.File file) throws IOException {
            outputStream.write(("--" + boundary + CRLF).getBytes());
            outputStream.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + file.getName()
                    + "\"" + CRLF).getBytes());
            outputStream.write(("Content-Type: application/octet-stream" + CRLF).getBytes());
            outputStream.write((CRLF).getBytes());

            // Write binary file data directly without string conversion
            Files.copy(file.toPath(), outputStream);

            outputStream.write((CRLF).getBytes());
        }

        public HttpRequest.BodyPublisher build() throws IOException {
            outputStream.write(("--" + boundary + "--" + CRLF).getBytes());
            return HttpRequest.BodyPublishers.ofByteArray(outputStream.toByteArray());
        }
    }
}
