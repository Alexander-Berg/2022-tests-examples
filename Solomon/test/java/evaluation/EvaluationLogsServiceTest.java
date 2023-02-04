package ru.yandex.solomon.alert.evaluation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.monlib.metrics.registry.MetricRegistry;
import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.dao.ProjectEvaluationLogsDao;
import ru.yandex.solomon.alert.dao.memory.InMemoryEvaluationLogsDao;
import ru.yandex.solomon.alert.domain.AlertKey;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.ut.ManualClock;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.alert.rule.AlertEvalStateTestSupport.randomState;
import static ru.yandex.solomon.alert.util.CommonMatchers.reflectionEqualTo;

/**
 * @author Vladimir Gordiychuk
 */
public class EvaluationLogsServiceTest {

    private ManualClock clock;
    private ExecutorService executorService;
    private ProjectEvaluationLogsDao dao;
    private EvaluationLogsService service;

    @Before
    public void setUp() throws Exception {
        clock = new ManualClock();
        executorService = Executors.newFixedThreadPool(2);
        dao = new InMemoryEvaluationLogsDao().forProject("junk");
        service = new EvaluationLogsServiceImpl("junk", dao, new MetricRegistry(), executorService, clock);
    }

    @After
    public void tearDown() throws Exception {
        executorService.shutdownNow();
    }

    @Test
    public void saveRestore() {
        AlertKey key = new AlertKey("junk", "", "test");
        EvaluationState state = randomState(key, 1).nextStatus(EvaluationStatus.ERROR, clock.instant());
        service.save(state);
        service.actSync().join();

        List<EvaluationState> restored = restore(state.getLatestEvalTruncated().toEpochMilli() - 10_000);
        assertEquals(1, restored.size());
        assertThat(restored.get(0), reflectionEqualTo(state));
    }

    @Test
    public void concurrentSave() {
        long now = clock.millis();
        List<EvaluationState> states = IntStream.range(0, 1000)
                .parallel()
                .mapToObj(index -> {
                    AlertKey key = new AlertKey("junk", "", "test" + index);
                    return randomState(key, 1).nextStatus(EvaluationStatus.ERROR, Instant.ofEpochMilli(now));
                })
                .peek(state -> service.save(state))
                .collect(toList());
        service.actSync().join();

        List<EvaluationState> restored = restore(now - (now % 60_000));
        assertEquals(states.size(), restored.size());
    }

    @Test
    public void restoreNone() {
        service.actSync().join();
        List<EvaluationState> list = restore(clock.millis() - 60_000);
        assertEquals(Collections.emptyList(), list);
    }

    @Test
    public void deleteOldLogs() {
        long now = clock.millis();

        AlertKey key = new AlertKey("junk", "", "test");
        EvaluationState prev = randomState(key, 1);

        for (int index = 0; index < 10; index++) {
            clock.passedTime(1, TimeUnit.DAYS);
            prev = prev.nextStatus(EvaluationStatus.ERROR, clock.instant());
            service.save(prev);
            service.actSync().join();
        }

        List<EvaluationState> restored = restore(now);
        assertEquals(8, restored.size());
    }

    private List<EvaluationState> restore(long fromTimeMillis) {
        List<EvaluationState> result = new ArrayList<>();
        service.restore(fromTimeMillis, result::add).join();
        return result;
    }
}
