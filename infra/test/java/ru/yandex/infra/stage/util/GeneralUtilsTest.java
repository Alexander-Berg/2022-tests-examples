package ru.yandex.infra.stage.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class GeneralUtilsTest {

    public static final int GET_JITTER_SCENARIO_PROBS = 100;

    @Test
    void getJitterMillisMoreThanExpectedMidTest() {
        long nextBackoffMillis = 2500;
        List<Long> results = getJitterMillisScenario(nextBackoffMillis);

        long expectedBackOffMiddle = nextBackoffMillis +
                (nextBackoffMillis - GeneralUtils.DEFAULT_MIN_MILLISECONDS) / 2;
        long normalBackOffCount = results.stream()
                .map(nextBackoffWithJitter -> nextBackoffWithJitter > expectedBackOffMiddle)
                .count();
        // check that normalBackOffCount at least 25% count
        // we could check >= 50%, but the test will fail rather often
        assertThat(normalBackOffCount * 100L >= results.size() * 25L, equalTo(true));
    }

    @Test
    void getJitterMillisDoesNotIncreaseIn2TimesTest() {
        long nextBackoffMillis = 50;
        List<Long> results = getJitterMillisScenario(nextBackoffMillis);

        boolean noIncrementsMoreThanBackOff = results.stream()
                .noneMatch(nextBackoffWithJitter -> nextBackoffWithJitter > nextBackoffMillis * 2);
        assertThat(noIncrementsMoreThanBackOff, equalTo(true));
    }

    private static List<Long> getJitterMillisScenario(long nextBackoffMillis) {
        return IntStream.range(0, GET_JITTER_SCENARIO_PROBS)
                .mapToObj(i -> GeneralUtils.getNextBackoffWithJitter(nextBackoffMillis))
                .collect(Collectors.toList());
    }
}
