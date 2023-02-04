package ru.yandex.qe.dispenser.ws.logic;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuota;
import ru.yandex.qe.dispenser.api.v1.DiQuotaMaxUpdate;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.domain.dao.quota.QuotaDao;
import ru.yandex.qe.dispenser.ws.quota.QuotaMaxAggregationJob;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QuotaMaxAggregationJobTest extends BusinessLogicTestBase {
    @Autowired
    private QuotaDao quotaDao;

    private QuotaMaxAggregationJob quotaMaxAggregationJob;

    @BeforeAll
    public void beforeClass() {
        quotaMaxAggregationJob = new QuotaMaxAggregationJob(quotaDao, hierarchy, true);
    }

    @Test
    public void quotaMaxValuesShouldBeAggregatedCorrectly() {
        dispenser().service(MDS).updateMax().quotas()
                .updateMax(DiQuotaMaxUpdate.forResource(HDD).forProject(INFRA).withOwnMax(DiAmount.of(10, DiUnit.GIBIBYTE)).build())
                .updateMax(DiQuotaMaxUpdate.forResource(HDD).forProject(INFRA_SPECIAL).withOwnMax(DiAmount.of(10, DiUnit.GIBIBYTE)).build())
                .updateMax(DiQuotaMaxUpdate.forResource(HDD).forProject(SEARCH).withOwnMax(DiAmount.of(5, DiUnit.GIBIBYTE)).build())
                .updateMax(DiQuotaMaxUpdate.forResource(HDD).forProject(YANDEX).withOwnMax(DiAmount.of(5, DiUnit.GIBIBYTE)).build())
                .performBy(WHISTLER);

        quotaMaxAggregationJob.aggregateMaxes();
        updateHierarchy();

        final DiQuotaGetResponse quotas = dispenser().quotas().get().inService(MDS).forResource(HDD).perform();
        assertEquals(15, findQuota(quotas, SEARCH).getMax(DiUnit.GIBIBYTE));
        assertEquals(30, findQuota(quotas, YANDEX).getMax(DiUnit.GIBIBYTE));
    }

    private static DiQuota findQuota(final DiQuotaGetResponse quotas, final String projectKey) {
        return quotas.stream()
                .filter(quota -> quota.getProject().getKey().equals(projectKey))
                .findFirst()
                .get();
    }
}
