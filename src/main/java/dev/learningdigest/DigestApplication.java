package dev.learningdigest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class DigestApplication {
    private DigestApplication() {}

    public static void main(String[] args) throws Exception {
        DigestConfiguration config = DigestConfiguration.load(args);
        if (List.of(args).contains("--schedule")) {
            String jobId = new InfraiCronClient(config.apiKey())
                    .createWeeklyDigest(config.cronExpression(), config.taskUrl());
            System.out.println("Weekly creator digest scheduled: " + jobId);
            return;
        }

        Instant now = Instant.parse("2026-09-10T09:00:00Z");
        CreatorDigestService.Digest digest = new CreatorDigestService().prepareWeeklyDigest(
                List.of(
                        new CreatorDigestService.DigitalAsset("Lighting a product lesson", "https://creator.example/lessons/lighting", true, now.minus(2, ChronoUnit.DAYS)),
                        new CreatorDigestService.DigitalAsset("Raw camera notes", "https://creator.example/notes/camera", false, now.minus(1, ChronoUnit.DAYS))),
                List.of(
                        new CreatorDigestService.SubscriberUpdate("learner@example.com", true, now.minus(3, ChronoUnit.DAYS)),
                        new CreatorDigestService.SubscriberUpdate("paused@example.com", false, now.minus(2, ChronoUnit.DAYS))),
                now.minus(7, ChronoUnit.DAYS));
        System.out.printf("%s%nrecipients=%s%nlessons=%s%n", digest.subject(), digest.recipients(),
                digest.lessons().stream().map(CreatorDigestService.DigitalAsset::title).toList());
    }
}
