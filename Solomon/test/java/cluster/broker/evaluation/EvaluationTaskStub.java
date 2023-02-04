package ru.yandex.solomon.alert.cluster.broker.evaluation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

import javax.annotation.Nullable;

import io.grpc.Status;

import ru.yandex.solomon.alert.cluster.project.ProjectAssignment;
import ru.yandex.solomon.alert.domain.AlertKey;
import ru.yandex.solomon.alert.rule.AlertEvalStateTestSupport;
import ru.yandex.solomon.alert.rule.AlertProcessingState;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.balancer.AssignmentSeqNo;
import ru.yandex.solomon.util.host.HostUtils;

import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlert;

/**
 * @author Vladimir Gordiychuk
 */
public class EvaluationTaskStub implements EvaluationObserver {
    public final Task task;

    public volatile CountDownLatch sync;
    public volatile EvaluationState latestEvaluation;
    public volatile boolean complete;
    public volatile Status latestStatus;

    public EvaluationTaskStub() {
        var alert = randomAlert();
        var assignment = new ProjectAssignment(
                alert.getProjectId(),
                HostUtils.getFqdn(),
                new AssignmentSeqNo(42, 100));

        task = new Task(assignment, alert, this);
        sync = new CountDownLatch(1);
    }

    @Nullable
    @Override
    public EvaluationState getLatestEvaluation() {
        return latestEvaluation;
    }

    @Nullable
    @Override
    public AlertProcessingState getProcessingState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(EvaluationState item) {
        latestEvaluation = item;
        latestStatus = Status.OK;
        onEvent();
    }

    @Override
    public void onError(Throwable throwable) {
        latestStatus = Status.fromThrowable(throwable);
        onEvent();
    }

    @Override
    public void onComplete() {
        complete = true;
        onEvent();
    }

    void onEvent() {
        var copy = sync;
        sync = new CountDownLatch(1);
        copy.countDown();
    }

    public AlertKey alertKey() {
        return task.alert.getKey();
    }

    public EvaluationState nextEval() {
        if (latestEvaluation == null) {
            return AlertEvalStateTestSupport.randomState(task.alert);
        } else {
            return AlertEvalStateTestSupport.next(latestEvaluation, AlertEvalStateTestSupport.randomEvalStatus());
        }
    }
}

