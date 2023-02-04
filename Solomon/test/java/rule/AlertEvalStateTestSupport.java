package ru.yandex.solomon.alert.rule;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.base.Throwables;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertKey;

/**
 * @author Vladimir Gordiychuk
 */
public final class AlertEvalStateTestSupport {
    private AlertEvalStateTestSupport() {
    }

    public static EvaluationState randomState() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return randomState(randomKey(), random.nextInt());
    }

    public static EvaluationState randomState(Alert alert) {
        return randomState(alert.getKey(), alert.getVersion());
    }

    public static EvaluationState randomState(AlertKey key, int version) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Instant latestEval = Instant.ofEpochMilli(System.currentTimeMillis()).plusSeconds(randomSecondsNearYear(random));
        Instant since = latestEval.minusSeconds(randomSecondsNearYear(random));

        return EvaluationState.newBuilder()
                .setAlertKey(key)
                .setAlertVersion(version)
                .setSince(since)
                .setStatus(randomEvalStatus(random))
                .setLatestEval(latestEval)
                .setPreviousStatus(randomEvalStatus(random))
                .build();
    }

    private static AlertKey randomKey() {
        String projectId = UUID.randomUUID().toString();
        String alertId = UUID.randomUUID().toString();
        return new AlertKey(projectId, "", alertId);
    }

    public static EvaluationStatus randomEvalStatus(ThreadLocalRandom random) {
        EvaluationStatus.Code[] source = EvaluationStatus.Code.values();
        EvaluationStatus.Code code = source[random.nextInt(0, source.length)];
        if (code == EvaluationStatus.Code.ERROR) {
            return code.toStatus(Throwables.getStackTraceAsString(new RuntimeException("expected rnd: "+random.nextInt())));
        }
        return code.toStatus(random.nextBoolean() ? "rnd: " + random.nextInt() : "");
    }

    public static EvaluationStatus randomEvalStatus() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return randomEvalStatus(random);
    }

    private static long randomSecondsNearYear(ThreadLocalRandom random) {
        return random.nextLong(0, ChronoUnit.YEARS.getDuration().getSeconds());
    }

    public static EvaluationState next(EvaluationState state, EvaluationStatus status) {
        Instant nextTime = state.getLatestEval().plus(1, ChronoUnit.MINUTES);
        return state.nextStatus(status, nextTime);
    }
}
