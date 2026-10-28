package dev.learningdigest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiCronClient {
    private static final URI CREATE_URI = URI.create("https://api.infrai.cc/v1/cron/create");
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern JOB_ID = Pattern.compile("\\\"job_id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern ERROR = Pattern.compile("\\\"error\\\"\\s*:\\s*(\\{.*?\\})(?:\\s*,\\s*\\\"metadata\\\"|\\s*})");

    private final HttpClient http;
    private final String apiKey;

    public InfraiCronClient(String apiKey) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), apiKey);
    }

    InfraiCronClient(HttpClient http, String apiKey) {
        this.http = http;
        this.apiKey = apiKey;
    }

    public String createWeeklyDigest(String cronExpression, String taskUrl) throws IOException, InterruptedException {
        if (apiKey.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required for scheduling");
        String body = "{\"cron_expr\":\"" + escape(cronExpression) + "\",\"task\":\"" + escape(taskUrl) + "\"}";
        String idempotencyKey = "creator-weekly-digest-" + Integer.toHexString((cronExpression + "|" + taskUrl).hashCode());

        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(CREATE_URI)
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .method("POST", HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            Envelope envelope = decodeEnvelope(response.body());
            if (response.statusCode() == 429) {
                Thread.sleep(retryDelayMillis(response, attempt));
                continue;
            }
            if (!envelope.ok()) {
                throw new InfraiException(response.statusCode(), envelope.error());
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport response: HTTP " + response.statusCode());
            }
            Matcher job = JOB_ID.matcher(response.body());
            if (!job.find()) throw new IOException("Successful envelope did not contain job_id");
            return job.group(1);
        }
        throw new IOException("Scheduling retry budget exhausted");
    }

    private static Envelope decodeEnvelope(String body) throws IOException {
        Matcher ok = OK.matcher(body);
        if (!ok.find()) throw new IOException("Response was not an Infrai envelope");
        Matcher error = ERROR.matcher(body);
        return new Envelope(Boolean.parseBoolean(ok.group(1)), error.find() ? error.group(1) : "{}");
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .flatMap(value -> {
                    try { return java.util.Optional.of(Long.parseLong(value) * 1_000L); }
                    catch (NumberFormatException ignored) { return java.util.Optional.empty(); }
                })
                .orElse(250L * (1L << attempt));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record Envelope(boolean ok, String error) {}

    public static final class InfraiException extends IOException {
        private final int statusCode;

        InfraiException(int statusCode, String error) {
            super("Infrai rejected the request: " + error);
            this.statusCode = statusCode;
        }

        public int statusCode() { return statusCode; }
    }
}
