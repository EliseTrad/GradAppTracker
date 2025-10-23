package com.gradapptracker.ui.services;

import com.gradapptracker.ui.utils.UserSession;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
}
