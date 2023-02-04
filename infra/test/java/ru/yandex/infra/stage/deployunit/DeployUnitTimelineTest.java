package ru.yandex.infra.stage.deployunit;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DeployUnitTimelineTest {
    private static final long ZERO_SECONDS = 0L;
    private static final long ONE_SECOND = 1000L;
    private static final long TWO_SECONDS = 2000L;

    private static final Instant ZERO_INSTANT = Instant.ofEpochMilli(ZERO_SECONDS);
    private static final Instant ONE_INSTANT = Instant.ofEpochMilli(ONE_SECOND);
    private static final Instant TWO_INSTANT = Instant.ofEpochMilli(TWO_SECONDS);

    private static Stream<Arguments> getDeployingTimeSource() {
        return Stream.of(
                Arguments.of(ZERO_INSTANT, Optional.of(ZERO_INSTANT), Optional.of(ZERO_SECONDS)),
                Arguments.of(ZERO_INSTANT, Optional.of(ONE_INSTANT), Optional.of(ONE_SECOND)),
                Arguments.of(ZERO_INSTANT, Optional.of(TWO_INSTANT), Optional.of(TWO_SECONDS)),
                Arguments.of(ONE_INSTANT, Optional.of(TWO_INSTANT), Optional.of(ONE_SECOND)),
                Arguments.of(ZERO_INSTANT, Optional.empty(), Optional.empty())
        );
    }

    @ParameterizedTest
    @MethodSource("getDeployingTimeSource")
    public void getDeployingTimeTest(Instant startTimestamp, Optional<Instant> finishTimestamp, Optional<Long> deployingTime) {
        DeployUnitTimeline deployUnitTimeline = new DeployUnitTimeline(
                -1,
                startTimestamp,
                finishTimestamp,
                null,
                null,
                -1
        );

        Assertions.assertEquals(deployingTime, deployUnitTimeline.getDeployingTime());
    }
}
