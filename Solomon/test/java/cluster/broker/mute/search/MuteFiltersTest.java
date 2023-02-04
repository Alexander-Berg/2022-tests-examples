package ru.yandex.solomon.alert.cluster.broker.mute.search;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.mute.domain.Mute;
import ru.yandex.solomon.alert.mute.domain.SelectorsMute;
import ru.yandex.solomon.alert.protobuf.ListMutesRequest;
import ru.yandex.solomon.alert.protobuf.MuteStatus;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class MuteFiltersTest {

    private List<Mute> mutes;
    private Instant now;

    private static Mute.MuteBuilder<?, ?> randomMuteBuilder() {
        return SelectorsMute.newBuilder()
                .setProjectId("junk")
                .setId(UUID.randomUUID().toString())
                .setCreatedAt(Instant.now())
                .setUpdatedAt(Instant.now())
                .setTtlBase(Instant.now());
    }

    @Before
    public void setUp() {
        now = Instant.now();

        mutes = new ArrayList<>();

        mutes.add(randomMuteBuilder()
                .setName("aaba")
                .setFrom(now)
                .setTo(now.plusSeconds(600))
                .build());

        mutes.add(randomMuteBuilder()
                .setName("baba")
                .setFrom(now)
                .setTo(now.plusSeconds(600))
                .build());

        mutes.add(randomMuteBuilder()
                .setName("cabac")
                .setFrom(now.minusSeconds(1800))
                .setTo(now.minusSeconds(600))
                .build());

        mutes.add(randomMuteBuilder()
                .setName("daaab")
                .setFrom(now.minusSeconds(1700))
                .setTo(now.minusSeconds(800))
                .build());

        mutes.add(randomMuteBuilder()
                .setName("edede")
                .setFrom(now.minusSeconds(3 * 86400))
                .setTo(now.minusSeconds(2 * 86400))
                .build());

        mutes.add(randomMuteBuilder()
                .setName("fee")
                .setFrom(now.plusSeconds(86400))
                .setTo(now.plusSeconds(2 * 86400))
                .build());
    }

    @Test
    public void filterByName() {
        Predicate<Mute> filter = MuteFilters.filterBy(ListMutesRequest.newBuilder()
                .setFilterByName("aBa")
                .build(),
                now);

        var filteredNames = mutes.stream()
                .filter(filter)
                .map(Mute::getName)
                .collect(Collectors.toUnmodifiableSet());

        Assert.assertEquals(Set.of("aaba", "cabac", "baba"), filteredNames);
    }

    @Test
    public void filterByState() {
        Predicate<Mute> filter = MuteFilters.filterBy(ListMutesRequest.newBuilder()
                .addFilterByStates(MuteStatus.ACTIVE)
                .addFilterByStates(MuteStatus.ARCHIVED)
                .build(),
                now);

        var filteredNames = mutes.stream()
                .filter(filter)
                .map(Mute::getName)
                .collect(Collectors.toUnmodifiableSet());

        Assert.assertEquals(Set.of("aaba", "edede", "baba"), filteredNames);
    }

    @Test
    public void filterByStateAndName() {
        Predicate<Mute> filter = MuteFilters.filterBy(ListMutesRequest.newBuilder()
                .addFilterByStates(MuteStatus.ACTIVE)
                .addFilterByStates(MuteStatus.ARCHIVED)
                .setFilterByName("aba")
                .build(),
                now);

        var filteredNames = mutes.stream()
                .filter(filter)
                .map(Mute::getName)
                .collect(Collectors.toUnmodifiableSet());

        Assert.assertEquals(Set.of("aaba", "baba"), filteredNames);
    }
}
