package ru.yandex.qe.dispenser.ws.logic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.monlib.metrics.primitives.GaugeInt64;
import ru.yandex.monlib.metrics.registry.MetricId;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.solomon.SolomonHolder;
import ru.yandex.qe.dispenser.ws.ServicesOverCommitValueMetrics;
import ru.yandex.qe.dispenser.ws.api.ApiTestBase;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ServicesOverCommitValueMetricsTest extends ApiTestBase {

    @Autowired
    private SolomonHolder solomonHolder;

    @Test
    public void verifyOverCommitCalculatedRight() {
        final ServicesOverCommitValueMetrics servicesOverCommitValueMetrics
                = new ServicesOverCommitValueMetrics(hierarchy, solomonHolder);

        final String serviceKey = NIRVANA;
        final String projectKey = INFRA;
        final String resourceKey = YT_CPU;
        final DiUnit resourceType = DiUnit.PERMILLE;
        final String specKey = YT_CPU;

        // Изменяем квоту проекта. Ставим actual и max в 0.
        dispenser().service(serviceKey).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(resourceKey).forProject(projectKey)
                        .withOwnMax(DiAmount.of(0, resourceType))
                        .withMax(DiAmount.of(0, resourceType))
                        .forKey(specKey)
                        .build())
                .performBy(AMOSOV_F);

        dispenser().service(serviceKey).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(resourceKey).forProject(projectKey)
                        .withActual(DiAmount.of(0, resourceType))
                        .forKey(specKey)
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));
        updateHierarchy();

        // Обновляем метрики
        servicesOverCommitValueMetrics.countOverCommit();

        // Получаем Counter метрику для ресурса нашего сервиса
        GaugeInt64 metricOverCommitCounter = (GaugeInt64) solomonHolder.getRootRegistry().getMetric(
                new MetricId("quotas.over_commit", Labels.of("provider_key", serviceKey,
                        "resource_key", resourceKey)));
        assertNotNull(metricOverCommitCounter);
        Assertions.assertEquals(0, metricOverCommitCounter.get());

        // Изменяем квоту проекта. Ставим actual = 700 > max.
        dispenser().service(serviceKey).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(resourceKey).forProject(projectKey)
                        .withActual(DiAmount.of(700, resourceType))
                        .forKey(specKey)
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));
        updateHierarchy();

        // Обновляем метрики
        servicesOverCommitValueMetrics.countOverCommit();

        // Проверяем, что изменилось значение оверкоммита
        Assertions.assertEquals(700, metricOverCommitCounter.get());

        // Изменяем квоту проекта. Ставим actual = 0 = max.
        dispenser().service(serviceKey).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(resourceKey).forProject(projectKey)
                        .withActual(DiAmount.of(0, resourceType))
                        .forKey(specKey)
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));
        updateHierarchy();

        // Обновляем метрики
        servicesOverCommitValueMetrics.countOverCommit();

        // Проверяем, что оверкоммит исчез (  значение оверкоммита для тестируемого ресурса сервиса - равно 0 )
        Assertions.assertEquals(0, metricOverCommitCounter.get());
    }
}
