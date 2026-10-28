package dev.learningdigest;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public final class CreatorDigestService {
    public record DigitalAsset(String title, String deliveryUrl, boolean processed, Instant publishedAt) {}
    public record SubscriberUpdate(String email, boolean subscribed, Instant changedAt) {}
    public record Digest(String subject, List<String> recipients, List<DigitalAsset> lessons) {}

    public Digest prepareWeeklyDigest(
            List<DigitalAsset> assets,
            List<SubscriberUpdate> updates,
            Instant windowStart) {
        List<String> recipients = updates.stream()
                .filter(SubscriberUpdate::subscribed)
                .filter(update -> !update.changedAt().isBefore(windowStart))
                .map(SubscriberUpdate::email)
                .distinct()
                .sorted()
                .toList();
        List<DigitalAsset> lessons = assets.stream()
                .filter(DigitalAsset::processed)
                .filter(asset -> !asset.publishedAt().isBefore(windowStart))
                .sorted(Comparator.comparing(DigitalAsset::publishedAt))
                .toList();
        return new Digest("This week's creator classroom", recipients, lessons);
    }
}
