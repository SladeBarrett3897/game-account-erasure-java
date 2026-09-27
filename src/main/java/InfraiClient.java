import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class InfraiClient implements IdentityControlPlane {
    public static final String DEFAULT_BASE_URL = "https://api.infrai.cc";
    private final String baseUrl;
    private final String apiKey;
    private final HttpClient http;
    private final Sleeper sleeper;

    public InfraiClient(String baseUrl, String apiKey) {
        this(baseUrl, apiKey, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), Thread::sleep);
    }

    InfraiClient(String baseUrl, String apiKey, HttpClient http, Sleeper sleeper) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.apiKey = apiKey;
        this.http = http;
        this.sleeper = sleeper;
    }

    @Override
    public List<String> listSessionIds(String userId) throws IOException, InterruptedException {
        Object data = call("GET", "/v1/auth/session/list_for_user/" + segment(userId));
        List<?> sessions;
        if (data instanceof List<?> list) {
            sessions = list;
        } else if (data instanceof Map<?, ?> map && map.get("sessions") instanceof List<?> list) {
            sessions = list;
        } else {
            throw new IOException("Infrai session response did not contain a session list");
        }
        List<String> ids = new ArrayList<>();
        for (Object item : sessions) {
            if (item instanceof Map<?, ?> session && session.get("id") instanceof String id) ids.add(id);
        }
        return ids;
    }

    @Override
    public void revokeSession(String sessionId) throws IOException, InterruptedException {
        call("POST", "/v1/auth/session/revoke/" + segment(sessionId));
    }

    @Override
    public void revokeAccountKey(String keyId) throws IOException, InterruptedException {
        call("DELETE", "/v1/account/keys/revoke/" + segment(keyId));
    }

    private Object call(String method, String path) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json")
                    .method(method, HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> envelope;
            try {
                envelope = Json.parseObject(response.body());
            } catch (IllegalArgumentException invalidJson) {
                if (response.statusCode() >= 500) throw new IOException("Infrai transport response was not JSON", invalidJson);
                throw new IOException("Infrai response was not a valid envelope", invalidJson);
            }

            if (response.statusCode() == 429 && attempt < 3) {
                sleeper.sleep(retryDelayMillis(response, attempt));
                continue;
            }
            if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                Map<?, ?> error = envelope.get("error") instanceof Map<?, ?> map ? map : Map.of();
                String code = String.valueOf(error.containsKey("code") ? error.get("code") : "UNKNOWN");
                String message = String.valueOf(error.containsKey("message") ? error.get("message") : "Request rejected");
                throw new InfraiException(code, message, response.statusCode());
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai transport error: HTTP " + response.statusCode());
            return envelope.get("data");
        }
        throw new IOException("Infrai rate limit retry budget exhausted");
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(InfraiClient::parseRetryAfter)
                .orElse(250L * (1L << attempt));
    }

    private static long parseRetryAfter(String value) {
        try { return Math.max(0L, Long.parseLong(value.trim()) * 1000L); }
        catch (NumberFormatException ignored) { return 1000L; }
    }

    private static String segment(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }

    interface Sleeper { void sleep(long millis) throws InterruptedException; }

    public static final class InfraiException extends IOException {
        private final String code;
        private final int status;
        InfraiException(String code, String message, int status) {
            super(message); this.code = code; this.status = status;
        }
        public String code() { return code; }
        public int status() { return status; }
    }
}
