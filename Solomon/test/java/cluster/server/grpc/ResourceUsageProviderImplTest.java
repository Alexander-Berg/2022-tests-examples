package ru.yandex.solomon.alert.cluster.server.grpc;

import java.util.concurrent.ForkJoinPool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.consume.AlertRuleResultConsumerStub;
import ru.yandex.solomon.alert.evaluation.EvaluationServiceStub;
import ru.yandex.solomon.alert.protobuf.EvaluationServerStatusRequest;
import ru.yandex.solomon.ut.ManualClock;
import ru.yandex.solomon.ut.ManualScheduledExecutorService;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.alert.domain.AlertTestSupport.randomAlert;

/**
 * @author Vladimir Gordiychuk
 */
public class ResourceUsageProviderImplTest {
    private ManualClock clock;
    private ManualScheduledExecutorService timer;
    private EvaluationServiceStub evaluationService;
    private EvaluationServerStatusHandler handler;

    @Before
    public void setUp() throws Exception {
        clock = new ManualClock();
        timer = new ManualScheduledExecutorService(1, clock);
        evaluationService = new EvaluationServiceStub(clock, timer);
        handler = new EvaluationServerStatusHandler(evaluationService, timer, ForkJoinPool.commonPool());
    }

    @After
    public void tearDown() throws Exception {
        if (handler != null) {
            handler.close();
        }

        if (timer != null) {
            timer.shutdownNow();
        }
    }

    @Test
    public void empty() {
        handler.forceAct().join();
        var usage = handler.handle(EvaluationServerStatusRequest.getDefaultInstance());
        assertEquals(0, usage.getAssignmentsCount());
        assertEquals(0.0, usage.getEvaluationRate(), 0.01);
    }

    @Test
    public void actualAssignmentsCount() {
        for (int index = 0; index < 10; index++) {
            evaluationService.assign(randomAlert(), null, new AlertRuleResultConsumerStub());
        }

        handler.forceAct().join();
        var usage = handler.handle(EvaluationServerStatusRequest.getDefaultInstance());
        assertEquals(10, usage.getAssignmentsCount());
    }
}
