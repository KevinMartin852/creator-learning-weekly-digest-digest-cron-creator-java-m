package dev.learningdigest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class CreatorDigestServiceTest {
    public static void main(String[] args) {
        Instant now = Instant.parse("2026-09-10T09:00:00Z");
        CreatorDigestService.Digest digest = new CreatorDigestService().prepareWeeklyDigest(
                List.of(
                        new CreatorDigestService.DigitalAsset("Published lesson", "https://creator.example/a", true, now.minus(1, ChronoUnit.DAYS)),
                        new CreatorDigestService.DigitalAsset("Unprocessed lesson", "https://creator.example/b", false, now.minus(1, ChronoUnit.DAYS)),
                        new CreatorDigestService.DigitalAsset("Old lesson", "https://creator.example/c", true, now.minus(8, ChronoUnit.DAYS))),
                List.of(
                        new CreatorDigestService.SubscriberUpdate("active@example.com", true, now.minus(2, ChronoUnit.DAYS)),
                        new CreatorDigestService.SubscriberUpdate("left@example.com", false, now.minus(2, ChronoUnit.DAYS))),
                now.minus(7, ChronoUnit.DAYS));

        require(digest.recipients().equals(List.of("active@example.com")), "only current subscribers receive the digest");
        require(digest.lessons().stream().map(CreatorDigestService.DigitalAsset::title).toList()
                .equals(List.of("Published lesson")), "only processed assets from the weekly window are taught");
        System.out.println("PASS: one current learner receives one processed weekly lesson");
    }

    private static void require(boolean decision, String message) {
        if (!decision) throw new AssertionError(message);
    }
}
