package ru.yandex.solomon.alert.cluster.broker.mute.search;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Ordering;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.mute.domain.Mute;
import ru.yandex.solomon.alert.mute.domain.SelectorsMute;
import ru.yandex.solomon.alert.protobuf.EOrderDirection;
import ru.yandex.solomon.alert.protobuf.ListMutesRequest;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class MuteSortsTest {

    private List<Mute> mutes;

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
        Instant now = Instant.now();

        mutes = new ArrayList<>();

        mutes.add(randomMuteBuilder()
                        .setName("a")
                        .setFrom(now)
                        .setTo(now.plusSeconds(600))
                        .build());

        mutes.add(randomMuteBuilder()
                .setName("b")
                .setFrom(now)
                .setTo(now.plusSeconds(600))
                .build());

        mutes.add(randomMuteBuilder()
                .setName("c")
                .setFrom(now.minusSeconds(1800))
                .setTo(now.minusSeconds(600))
                .build());

        mutes.add(randomMuteBuilder()
                .setName("d")
                .setFrom(now.minusSeconds(1700))
                .setTo(now.minusSeconds(800))
                .build());

        mutes.add(randomMuteBuilder()
                .setName("e")
                .setFrom(now.minusSeconds(3 * 86400))
                .setTo(now.minusSeconds(2 * 86400))
                .build());

        mutes.add(randomMuteBuilder()
                .setName("f")
                .setFrom(now.plusSeconds(86400))
                .setTo(now.plusSeconds(2 * 86400))
                .build());
    }

    @Test
    public void orderByNameIsDefault() {
        mutes.sort(MuteSorts.orderBy(ListMutesRequest.getDefaultInstance()));
        var names = mutes.stream().map(Mute::getName).collect(Collectors.toUnmodifiableList());
        Assert.assertTrue(Ordering.natural().isOrdered(names));
    }

    @Test
    public void orderByName() {
        mutes.sort(MuteSorts.orderBy(ListMutesRequest.newBuilder()
                        .setOrderByName(EOrderDirection.ASC)
                        .build()));
        var names = mutes.stream().map(Mute::getName).collect(Collectors.toUnmodifiableList());
        Assert.assertTrue(Ordering.natural().isOrdered(names));
    }

    @Test
    public void orderByNameDesc() {
        mutes.sort(MuteSorts.orderBy(ListMutesRequest.newBuilder()
                .setOrderByName(EOrderDirection.DESC)
                .build()));
        var names = mutes.stream().map(Mute::getName).collect(Collectors.toUnmodifiableList());
        Assert.assertTrue(Ordering.natural().reverse().isOrdered(names));
    }

    @Test
    public void orderByState() {
        Instant now = Instant.now();
        mutes.sort(MuteSorts.orderBy(ListMutesRequest.newBuilder()
                .setOrderByState(EOrderDirection.ASC)
                .build()));
        var statuses = mutes.stream()
                .map(mute -> mute.getStatusAt(now))
                .collect(Collectors.toUnmodifiableList());
        Assert.assertTrue(Ordering.natural().isOrdered(statuses));

        mutes.stream()
                .collect(Collectors.groupingBy(mute -> mute.getStatusAt(now)))
                .forEach((status, mutes) -> Assert.assertTrue("For status " + status + " mutes are not sorted by name",
                        Ordering.natural().isOrdered(mutes.stream().map(Mute::getName).collect(Collectors.toList()))));
    }

    @Test
    public void orderByStateDesc() {
        Instant now = Instant.now();
        mutes.sort(MuteSorts.orderBy(ListMutesRequest.newBuilder()
                .setOrderByState(EOrderDirection.DESC)
                .build()));
        var statuses = mutes.stream()
                .map(mute -> mute.getStatusAt(now))
                .collect(Collectors.toUnmodifiableList());
        Assert.assertTrue(Ordering.natural().reverse().isOrdered(statuses));

        mutes.stream()
                .collect(Collectors.groupingBy(mute -> mute.getStatusAt(now)))
                .forEach((status, mutes) -> Assert.assertTrue("For status " + status + " mutes are not sorted by name",
                        Ordering.natural().isOrdered(mutes.stream().map(Mute::getName).collect(Collectors.toList()))));
    }
}
