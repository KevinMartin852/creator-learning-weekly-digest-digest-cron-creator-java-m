package dev.learningdigest;

import java.util.HashMap;
import java.util.Map;

public record DigestConfiguration(String apiKey, String taskUrl, String cronExpression) {
    public static DigestConfiguration load(String[] args) {
        Map<String, String> cli = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] pair = arg.substring(2).split("=", 2);
                cli.put(pair[0], pair[1]);
            }
        }
        Map<String, String> env = System.getenv();
        return new DigestConfiguration(
                cli.getOrDefault("api-key", env.getOrDefault("INFRAI_API_KEY", "")),
                cli.getOrDefault("task-url", env.getOrDefault("DIGEST_TASK_URL", "http://localhost:8080/digests/weekly")),
                cli.getOrDefault("cron", env.getOrDefault("DIGEST_CRON", "0 9 * * 1")));
    }
}
