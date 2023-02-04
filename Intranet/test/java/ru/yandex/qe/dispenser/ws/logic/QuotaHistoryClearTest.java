package ru.yandex.qe.dispenser.ws.logic;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiQuotaHistoryEvent;
import ru.yandex.qe.dispenser.api.v1.DiQuotaMaxDeltaUpdate;
import ru.yandex.qe.dispenser.api.v1.DiSignedAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.response.DiListRelativePageResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.dao.history.quota.QuotaHistoryDao;
import ru.yandex.qe.dispenser.domain.history.QuotaHistoryClearTask;
import ru.yandex.qe.dispenser.ws.param.RelativePageParam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.qe.dispenser.ws.logic.QuotaHistoryTest.RESPONSE_TYPE;

public class QuotaHistoryClearTest extends BusinessLogicTestBase {
    @Autowired
    private QuotaHistoryDao quotaHistoryDao;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        quotaHistoryDao.clear();
    }

    private DiListRelativePageResponse<DiQuotaHistoryEvent> history(final DiPerson person,
                                                                    final Map<String, Object> params) {
        final WebClient client = createAuthorizedLocalClient(person)
                .path("/v1/history/quotas/");

        params.forEach(client::query);
        return client.get(RESPONSE_TYPE);
    }

    private void changeDeltaMax(final String service,
                                final String project,
                                final String resource,
                                final String quotaSpec,
                                final Collection<String> segments,
                                final DiSignedAmount maxDelta,
                                final DiSignedAmount ownMaxDelta,
                                final DiPerson person) {
        dispenser().service(service)
                .updateMax()
                .deltas()
                .updateMax(DiQuotaMaxDeltaUpdate
                        .forResource(resource)
                        .forProject(project)
                        .withQuotaSpecKey(quotaSpec)
                        .withSegmentKeys(segments)
                        .withMaxDelta(maxDelta)
                        .withOwnMaxDelta(ownMaxDelta)
                        .build())
                .performBy(person);
    }

    @Test
    public void taskClearHistory() {
        final int eventCount = 3;
        for (int i = 0; i < eventCount; i++) {
            changeDeltaMax(YP, YANDEX, SEGMENT_CPU, SEGMENT_CPU, Collections.emptySet(), DiSignedAmount.positive(1, DiUnit.CORES),
                    DiSignedAmount.positive(0, DiUnit.CORES), SLONNN);
        }
        final ImmutableMap<String, Object> params = ImmutableMap.of(RelativePageParam.PAGE_ORDER, "ASC");
        DiListRelativePageResponse<DiQuotaHistoryEvent> history = history(SLONNN, params);
        assertEquals(eventCount, history.size());

        QuotaHistoryClearTask clearTask = new QuotaHistoryClearTask(quotaHistoryDao, 1000000, true);
        clearTask.clear();

        history = history(SLONNN, params);
        assertEquals(eventCount, history.size());

        clearTask = new QuotaHistoryClearTask(quotaHistoryDao, 1, true);
        clearTask.clear();

        history = history(SLONNN, params);
        assertEquals(0, history.size());
    }
}
