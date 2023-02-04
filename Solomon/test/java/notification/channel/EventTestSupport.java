package ru.yandex.solomon.alert.notification.channel;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.mute.domain.AffectingMute;
import ru.yandex.solomon.alert.mute.domain.MuteStatus;
import ru.yandex.solomon.alert.rule.AlertMuteStatus;
import ru.yandex.solomon.alert.rule.AlertProcessingState;
import ru.yandex.solomon.alert.rule.EvaluationState;

import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomActiveAlert;

/**
 * @author Vladimir Gordiychuk
 */
public final class EventTestSupport {
    private EventTestSupport() {
    }

    public static Event eval(EvaluationStatus status) {
        return eval(randomActiveAlert(), status);
    }

    public static Event eval(EvaluationStatus status, MuteStatus muteStatus) {
        return eval(randomActiveAlert(), status, muteStatus);
    }

    public static Event eval(Alert alert, EvaluationStatus.Code code) {
        return eval(alert, code.toStatus());
    }

    public static Event eval(Alert alert, EvaluationStatus status) {
        return eval(alert, status, Instant.now());
    }

    public static Event eval(Alert alert, EvaluationStatus.Code code, Instant now) {
        return eval(alert, code.toStatus(), now);
    }

    public static Event eval(Alert alert, EvaluationStatus status, Instant now) {
        EvaluationState state = EvaluationState.newBuilder(alert)
                .setSince(now)
                .setStatus(status)
                .build();

        return eval(alert, state);
    }

    public static Event eval(Alert alert, EvaluationStatus status, MuteStatus muteStatus) {
        return eval(alert, status, muteStatus, Instant.now());
    }

    public static Event eval(Alert alert, EvaluationStatus status, MuteStatus muteStatus, Instant now) {
        EvaluationState state = EvaluationState.newBuilder(alert)
                .setSince(now)
                .setStatus(status)
                .build();

        return eval(alert, state, muteStatus);
    }

    public static Event eval(Alert alert, EvaluationState state) {
        return eval(alert, new AlertProcessingState(
                state,
                new AlertMuteStatus(AlertMuteStatus.MuteStatusCode.NOT_MUTED, List.of()))
        );
    }

    public static Event eval(Alert alert, EvaluationState state, MuteStatus muteStatus) {
        return eval(alert, new AlertProcessingState(
                state,
                new AlertMuteStatus(
                        muteStatus == MuteStatus.ACTIVE
                                ? AlertMuteStatus.MuteStatusCode.MUTED
                                : AlertMuteStatus.MuteStatusCode.NOT_MUTED,
                        muteStatus == MuteStatus.UNKNOWN
                                ? List.of()
                                : List.of(new AffectingMute("someId", muteStatus)
                )))
        );
    }

    private static Event eval(Alert alert, AlertProcessingState alertProcessingState) {
        return new Event(alert, alertProcessingState);
    }

    public static Event nextEval(Event message, EvaluationStatus.Code code) {
        return nextEval(message, code.toStatus());
    }

    public static Event nextEval(Event message, EvaluationStatus status) {
        EvaluationState prevStatus = message.getState();
        Instant nextTime = prevStatus.getLatestEval().plus(1, ChronoUnit.MINUTES);
        EvaluationState nextStatus = prevStatus.nextStatus(status, nextTime);
        return eval(message.getAlert(), nextStatus);
    }

    public static Event nextEval(Event message, EvaluationStatus status, AlertMuteStatus alertMuteStatus) {
        EvaluationState prevStatus = message.getState();
        Instant nextTime = prevStatus.getLatestEval().plus(1, ChronoUnit.MINUTES);
        EvaluationState nextStatus = prevStatus.nextStatus(status, nextTime);
        return eval(message.getAlert(), new AlertProcessingState(nextStatus, alertMuteStatus));
    }

}
