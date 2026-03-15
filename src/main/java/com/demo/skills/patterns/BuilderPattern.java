package com.demo.skills.patterns;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builder pattern for constructing HttpRequest objects with a fluent API.
 *
 * The builder validates required fields and produces an immutable request object.
 */
public class BuilderPattern {

    // --- Immutable HttpRequest ---

    enum HttpMethod { GET, POST, PUT, DELETE, PATCH }

    static final class HttpRequest {
        private final HttpMethod method;
        private final String url;
        private final Map<String, String> headers;
        private final String body;
        private final int timeoutMs;
        private final boolean followRedirects;

        private HttpRequest(Builder builder) {
            this.method = builder.method;
            this.url = builder.url;
            this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers));
            this.body = builder.body;
            this.timeoutMs = builder.timeoutMs;
            this.followRedirects = builder.followRedirects;
        }

        // Accessors
        public HttpMethod method()         { return method; }
        public String url()                { return url; }
        public Map<String, String> headers() { return headers; }
        public String body()               { return body; }
        public int timeoutMs()             { return timeoutMs; }
        public boolean followRedirects()   { return followRedirects; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("  %s %s%n", method, url));
            headers.forEach((k, v) -> sb.append(String.format("  %s: %s%n", k, v)));
            sb.append(String.format("  Timeout: %dms | Follow redirects: %s%n", timeoutMs, followRedirects));
            if (body != null) {
                sb.append(String.format("  Body: %s%n", body));
            }
            return sb.toString();
        }

        // --- Builder ---

        static Builder builder(HttpMethod method, String url) {
            return new Builder(method, url);
        }

        static Builder get(String url)    { return builder(HttpMethod.GET, url); }
        static Builder post(String url)   { return builder(HttpMethod.POST, url); }
        static Builder put(String url)    { return builder(HttpMethod.PUT, url); }
        static Builder delete(String url) { return builder(HttpMethod.DELETE, url); }

        static final class Builder {
            private final HttpMethod method;
            private final String url;
            private final Map<String, String> headers = new LinkedHashMap<>();
            private String body;
            private int timeoutMs = 30_000; // default 30s
            private boolean followRedirects = true;

            private Builder(HttpMethod method, String url) {
                this.method = Objects.requireNonNull(method, "method is required");
                this.url = Objects.requireNonNull(url, "url is required");
            }

            public Builder header(String name, String value) {
                headers.put(
                        Objects.requireNonNull(name, "header name"),
                        Objects.requireNonNull(value, "header value"));
                return this;
            }

            public Builder contentType(String type) {
                return header("Content-Type", type);
            }

            public Builder authorization(String token) {
                return header("Authorization", "Bearer " + token);
            }

            public Builder body(String body) {
                this.body = body;
                return this;
            }

            public Builder timeout(int millis) {
                if (millis <= 0) throw new IllegalArgumentException("Timeout must be positive");
                this.timeoutMs = millis;
                return this;
            }

            public Builder followRedirects(boolean follow) {
                this.followRedirects = follow;
                return this;
            }

            public HttpRequest build() {
                if (url.isBlank()) {
                    throw new IllegalStateException("URL cannot be blank");
                }
                if ((method == HttpMethod.POST || method == HttpMethod.PUT) && body == null) {
                    System.out.println("  [WARN] " + method + " request without body");
                }
                return new HttpRequest(this);
            }
        }
    }

    // --- Demo ---

    public static void main(String[] args) {
        System.out.println("=== Builder Pattern (HttpRequest) Demo ===\n");

        // Simple GET
        System.out.println("1) Simple GET request:");
        HttpRequest get = HttpRequest.get("https://api.example.com/users")
                .header("Accept", "application/json")
                .authorization("sk-test-token-123")
                .timeout(5000)
                .build();
        System.out.println(get);

        // POST with JSON body
        System.out.println("2) POST request with JSON body:");
        HttpRequest post = HttpRequest.post("https://api.example.com/users")
                .contentType("application/json")
                .authorization("sk-test-token-123")
                .body("{\"name\": \"Alice\", \"email\": \"alice@example.com\"}")
                .timeout(10_000)
                .followRedirects(false)
                .build();
        System.out.println(post);

        // DELETE
        System.out.println("3) DELETE request:");
        HttpRequest delete = HttpRequest.delete("https://api.example.com/users/42")
                .authorization("sk-admin-token")
                .build();
        System.out.println(delete);

        System.out.println("Done.");
    }
}
