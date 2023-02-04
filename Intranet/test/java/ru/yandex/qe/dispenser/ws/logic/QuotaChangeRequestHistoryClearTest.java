package ru.yandex.qe.dispenser.ws.logic;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiQuotaRequestHistoryEvent;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.response.DiListRelativePageResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.dao.history.request.QuotaChangeRequestHistoryDao;
import ru.yandex.qe.dispenser.domain.history.QuotaChangeRequestHistoryClearTask;
import ru.yandex.qe.dispenser.ws.param.RelativePageParam;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QuotaChangeRequestHistoryClearTest extends BaseQuotaRequestTest {
    @Autowired
    private QuotaChangeRequestHistoryDao quotaChangeRequestHistoryDao;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        quotaChangeRequestHistoryDao.clear();
        createDefaultCampaign();
    }

    private DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history(final DiPerson person,
                                                                           final Map<String, Object> params) {
        final WebClient client = createAuthorizedLocalClient(person)
                .path("/v1/history/quota-requests/");

        params.forEach(client::query);
        return client.get(QuotaChangeRequestHistoryTest.RESPONSE_TYPE);
    }

    private DiQuotaChangeRequest createResourcePreorderRequest() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        return dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(project.getPublicKey())
                        .changes(NIRVANA, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F)
                .getFirst();
    }

    @Test
    public void taskClearHistory() {
        final int eventCount = 2;

        prepareCampaignResources();
        for (int i = 0; i < eventCount; i++) {
            createResourcePreorderRequest();
        }

        final ImmutableMap<String, Object> params = ImmutableMap.of(RelativePageParam.PAGE_ORDER, "ASC");
        DiListRelativePageResponse<DiQuotaRequestHistoryEvent> history = history(SLONNN, params);
        final int expectedHistoryEventCount = eventCount * 2; // CREATE + ISSUE_KEY_UPDATE events for each quota request
        assertEquals(expectedHistoryEventCount, history.size());

        QuotaChangeRequestHistoryClearTask clearTask = new QuotaChangeRequestHistoryClearTask(quotaChangeRequestHistoryDao, 1000000, true);
        clearTask.clear();

        history = history(SLONNN, params);
        assertEquals(expectedHistoryEventCount, history.size());

        clearTask = new QuotaChangeRequestHistoryClearTask(quotaChangeRequestHistoryDao, 1, true);
        clearTask.clear();

        history = history(SLONNN, params);
        assertEquals(0, history.size());
    }
}
