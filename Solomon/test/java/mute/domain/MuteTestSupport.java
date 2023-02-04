package ru.yandex.solomon.alert.mute.domain;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.solomon.labels.query.SelectorsFormat;

import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * @author Ivan Tsybulin
 */
public class MuteTestSupport {
    public static Mute randomMute() {
        return randomMute(ThreadLocalRandom.current());
    }

    public static Mute randomMute(ThreadLocalRandom random) {
        List<MuteType> types = Arrays.stream(MuteType.values())
            .collect(Collectors.toList());
        MuteType type = types.get(random.nextInt(types.size()));
        return randomMute(random, type);
    }

    public static Mute randomMute(MuteType type) {
        return randomMute(ThreadLocalRandom.current(), type);
    }

    public static Mute randomMute(ThreadLocalRandom random, MuteType type) {
        return randomDerivedMute(random, type)
                .setId(UUID.randomUUID().toString())
                .setProjectId("junk")
                .setFolderId("myfolder")
                .setName("Release by " + randomUser(random))
                .setDescription("Release mute #" + random.nextInt(10000))
                .setTicketId("DRILLS-" + random.nextInt(1000))
                .setFrom(Instant.now().truncatedTo(MILLIS).plus(random.nextLong(0, 1_000_000), MILLIS))
                .setTo(Instant.now().truncatedTo(MILLIS).plus(random.nextLong(1_000_000, 2_000_000), MILLIS))
                .setTtlBase(Instant.now().truncatedTo(MILLIS).plus(random.nextLong(2_000_000, 3_000_000), MILLIS))
                .setVersion(random.nextInt(0, 1000))
                .setCreatedBy(randomUser(random))
                .setUpdatedBy(randomUser(random))
                .setCreatedAt(Instant.now().truncatedTo(MILLIS).plus(random.nextLong(0, 1_000_000), MILLIS))
                .setUpdatedAt(Instant.now().truncatedTo(MILLIS).plus(random.nextLong(1_000_000, 2_000_000), MILLIS))
                .build();
    }

    private static Mute.MuteBuilder<?, ?> randomDerivedMute(ThreadLocalRandom random, MuteType type) {
        return switch (type) {
            case BY_SELECTORS -> randomSelectorsMute(random);
        };
    }

    private static SelectorsMute.Builder randomSelectorsMute(ThreadLocalRandom random) {
        var builder = SelectorsMute.newBuilder();
        switch (random.nextInt(0, 3)) {
            case 0 -> builder.setAlertSelector(SelectorsFormat.parseSelector("alert=*"));
            case 1 -> builder.setAlertSelector(SelectorsFormat.parseSelector("alert=test"));
            case 2 -> builder.setAlertSelector(SelectorsFormat.parseSelector("alert=test1|test2"));
        }
        switch (random.nextInt(0, 3)) {
            case 0 -> builder.setLabelSelectors(SelectorsFormat.parse("{}"));
            case 1 -> builder.setLabelSelectors(SelectorsFormat.parse("{host=solomon-04}"));
            case 2 -> builder.setLabelSelectors(SelectorsFormat.parse("{host=*-vla-*, service=coremon}"));
        }
        return builder;
    }


    private static String randomUser(ThreadLocalRandom random) {
        return switch (random.nextInt(10)) {
            case 0 -> "";
            case 1 -> "Anne Hathaway";
            case 2 -> "Marilyn Monroe";
            case 3 -> "Leonardo da Vinci";
            case 4 -> "Kenny Chesney";
            case 5 -> "Chief Sitting Bull";
            case 6 -> "Ellen DeGeneres";
            case 7 -> "Fidel Castro";
            default -> RandomStringUtils.randomAlphanumeric(10);
        };
    }
}
