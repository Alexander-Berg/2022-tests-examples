package ru.yandex.qe.dispenser.ws.logic;

import javax.ws.rs.ForbiddenException;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuota;
import ru.yandex.qe.dispenser.api.v1.DiQuotaMaxDeltaUpdate;
import ru.yandex.qe.dispenser.api.v1.DiQuotaMaxUpdate;
import ru.yandex.qe.dispenser.api.v1.DiSignedAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.client.v1.builder.QuotaMaxUpdateRequestBuilder;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.domain.util.CollectionUtils;
import ru.yandex.qe.dispenser.standalone.MockTrackerManager;
import ru.yandex.qe.dispenser.ws.ServiceService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class QuotaMaxUpdateTest extends BusinessLogicTestBase {

    @Autowired
    private MockTrackerManager trackerManager;

    @BeforeEach
    public void init() {
        dispenser().properties()
                .setProperty(ServiceService.OLD_STYLE_QUOTA_MAX_CHANGE_ENTITY, ServiceService.FORBIDDEN_SERVICES_PROPERTY, NIRVANA)
                .performBy(AMOSOV_F);
        updateHierarchy();
        trackerManager.clearIssues();
    }

    @Test
    public void updateWithoutMaxAndOwnMaxFieldsWillCauseException() {
        assertThrowsWithMessage(() -> dispenser().service(NIRVANA).updateMax().quotas()
                .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(YANDEX).build())
                .performBy(SANCHO), "Incorrect update for quota! Both 'max' and 'ownMax' fields are absent");
    }

    @Test
    public void emptyUpdateWontCreateTicket() {
        dispenser().service(NIRVANA).updateMax().quotas()
                .performBy(SANCHO);
        assertTrue(trackerManager.getIssues().isEmpty());
    }

    @Test
    public void serviceAdminCanUpdateMaxValuesOfServiceQuotas() {
        final DiQuotaGetResponse response = dispenser().service(NIRVANA).updateMax().quotas()
                .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(YANDEX).withMax(DiAmount.of(100, DiUnit.GIBIBYTE)).build())
                .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(DEFAULT).withMax(DiAmount.of(90, DiUnit.GIBIBYTE)).build())
                .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(INFRA).withMax(DiAmount.of(10, DiUnit.GIBIBYTE)).build())
                .performBy(SANCHO);
        assertEquals(3, response.size());
        updateHierarchy();
        assertEquals(getQuotaMax(NIRVANA, STORAGE, YANDEX), DiAmount.of(100, DiUnit.GIBIBYTE));
        assertEquals(getQuotaMax(NIRVANA, STORAGE, DEFAULT), DiAmount.of(90, DiUnit.GIBIBYTE));
        assertEquals(getQuotaMax(NIRVANA, STORAGE, INFRA), DiAmount.of(10, DiUnit.GIBIBYTE));
    }

    @Test
    public void ownMaxUpdateIsPossible() {
        final DiQuotaGetResponse response = dispenser().service(NIRVANA).updateMax().quotas()
                .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(YANDEX).withOwnMax(DiAmount.of(100, DiUnit.BYTE)).build())
                .performBy(SANCHO);
        assertEquals(1, response.size());
        updateHierarchy();
        assertEquals(getQuotaOwnMax(NIRVANA, STORAGE, YANDEX), DiAmount.of(100, DiUnit.BYTE));
    }

    @Test
    public void projectResponsibleCanUpdateMaxValuesOfSubprojectQuotas() {
        final DiQuotaGetResponse response = dispenser().service(NIRVANA).updateMax().quotas()
                .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(DEFAULT).withMax(DiAmount.of(100, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(INFRA).withMax(DiAmount.of(50, DiUnit.BYTE)).build())
                .performBy(WHISTLER);
        assertEquals(2, response.size());
        updateHierarchy();
        assertEquals(getQuotaMax(NIRVANA, STORAGE, DEFAULT), DiAmount.of(100, DiUnit.BYTE));
        assertEquals(getQuotaMax(NIRVANA, STORAGE, INFRA), DiAmount.of(50, DiUnit.BYTE));
    }

    @Test
    public void projectResponsibleCantUpdateMaxValuesOfHisProjectQuotas() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser().service(NIRVANA).updateMax().quotas()
                    .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(YANDEX).withMax(DiAmount.of(100, DiUnit.GIBIBYTE)).build())
                    .performBy(WHISTLER);
        });
    }

    @Test
    public void projectMemberCantUpdateQuotaMaxValues() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser().service(NIRVANA).updateMax().quotas()
                    .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(DEFAULT).withMax(DiAmount.of(100, DiUnit.BYTE)).build())
                    .performBy(LYADZHIN);
        });
    }

    @Test
    public void whenUpdatingMaxValueCorrectQuotaStateShouldBeReturned() {
        dispenser().service(NIRVANA).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU).forProject(DEFAULT).withActual(DiAmount.of(10, DiUnit.COUNT)).build())
                .perform();
        updateHierarchy();
        assertEquals(getQuotaActual(NIRVANA, YT_CPU, DEFAULT), DiAmount.of(10, DiUnit.COUNT));
        final DiQuotaGetResponse response = dispenser().service(NIRVANA).updateMax().quotas()
                .updateMax(DiQuotaMaxUpdate.forResource(YT_CPU).forProject(YANDEX).withMax(DiAmount.of(100, DiUnit.COUNT)).build())
                .performBy(SANCHO);
        final DiQuota changedQuota = CollectionUtils.first(response);
        assertEquals(100, changedQuota.getMax(DiUnit.COUNT));
        assertEquals(10, changedQuota.getActual(DiUnit.COUNT));
    }

    @Test
    public void updatingMaxByDeltasShouldWorkCorrectly() {
        dispenser().service(NIRVANA).updateMax().quotas()
                .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(YANDEX).withMax(DiAmount.of(200, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(DEFAULT).withMax(DiAmount.of(100, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(INFRA).withMax(DiAmount.of(50, DiUnit.BYTE)).build())
                .performBy(SANCHO);

        final DiQuotaGetResponse response = dispenser().service(NIRVANA).updateMax().deltas()
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(YANDEX).withOwnMaxDelta(DiSignedAmount.positive(30, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(YANDEX).withOwnMaxDelta(DiSignedAmount.negative(10, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(DEFAULT).withMaxDelta(DiSignedAmount.positive(10, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(DEFAULT).withMaxDelta(DiSignedAmount.negative(20, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(DEFAULT).withMaxDelta(DiSignedAmount.positive(50, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(INFRA).withMaxDelta(DiSignedAmount.negative(50, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(INFRA).withMaxDelta(DiSignedAmount.positive(10, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(INFRA).withMaxDelta(DiSignedAmount.positive(30, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(INFRA).withMaxDelta(DiSignedAmount.positive(1, DiUnit.KIBIBYTE)).build())
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(INFRA).withMaxDelta(DiSignedAmount.negative(1024, DiUnit.BYTE)).build())
                .performBy(SANCHO);

        assertEquals(3, response.size());
        response.forEach(q -> {
            final String projectKey = q.getProject().getKey();
            if (projectKey.equals(YANDEX)) {
                assertEquals(200, q.getMax(DiUnit.BYTE));
                assertEquals(20, q.getOwnMax(DiUnit.BYTE));
            } else if (projectKey.equals(DEFAULT)) {
                assertEquals(140, q.getMax(DiUnit.BYTE));
            } else if (projectKey.equals(INFRA)) {
                assertEquals(40, q.getMax(DiUnit.BYTE));
            }
        });

        updateHierarchy();

        assertEquals(getQuotaMax(NIRVANA, STORAGE, YANDEX), DiAmount.of(200, DiUnit.BYTE));
        assertEquals(getQuotaOwnMax(NIRVANA, STORAGE, YANDEX), DiAmount.of(20, DiUnit.BYTE));
        assertEquals(getQuotaMax(NIRVANA, STORAGE, DEFAULT), DiAmount.of(140, DiUnit.BYTE));
        assertEquals(getQuotaMax(NIRVANA, STORAGE, INFRA), DiAmount.of(40, DiUnit.BYTE));
    }

    @Test
    public void updatingByDeltasMayBeSetToZero() {
        dispenser().service(NIRVANA).updateMax().quotas()
                .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(YANDEX).withMax(DiAmount.of(2000, DiUnit.BYTE))
                        .withOwnMax(DiAmount.of(300, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(DEFAULT).withMax(DiAmount.of(1000, DiUnit.BYTE))
                        .withOwnMax(DiAmount.of(200, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(INFRA).withMax(DiAmount.of(50, DiUnit.BYTE))
                        .withOwnMax(DiAmount.of(10, DiUnit.BYTE)).build())
                .performBy(SANCHO);

        final DiQuotaGetResponse response = dispenser().service(NIRVANA).updateMax().deltas()
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(INFRA).withMaxDelta(DiSignedAmount.positive(25, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(INFRA).withMaxDelta(DiSignedAmount.positive(25, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(INFRA).withMaxDelta(DiSignedAmount.positive(1, DiUnit.KIBIBYTE)).build())
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(INFRA).withMaxDelta(DiSignedAmount.negative(1024, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(INFRA).withMaxDelta(DiSignedAmount.negative(100, DiUnit.BYTE)).build())
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(INFRA).withOwnMaxDelta(DiSignedAmount.negative(10, DiUnit.BYTE)).build())
                .performBy(SANCHO);

        assertEquals(1, response.size());
        response.forEach(q -> {
            final String projectKey = q.getProject().getKey();
            if (projectKey.equals(INFRA)) {
                assertEquals(0, q.getMax(DiUnit.BYTE));
                assertEquals(0, q.getOwnMax(DiUnit.BYTE));
            }
        });

        updateHierarchy();

        assertEquals(getQuotaMax(NIRVANA, STORAGE, INFRA), DiAmount.of(0, DiUnit.BYTE));
        assertEquals(getQuotaOwnMax(NIRVANA, STORAGE, INFRA), DiAmount.of(0, DiUnit.BYTE));
    }

    @Test
    public void updatingMaxByDeltasShouldBeIdempotent() {
        dispenser().service(NIRVANA).updateMax().quotas()
                .updateMax(DiQuotaMaxUpdate.forResource(STORAGE).forProject(YANDEX).withMax(DiAmount.of(100, DiUnit.BYTE)).build())
                .performBy(SANCHO);

        final QuotaMaxUpdateRequestBuilder<DiQuotaMaxDeltaUpdate> requestBuilder = dispenser().service(NIRVANA).updateMax().deltas()
                .updateMax(DiQuotaMaxDeltaUpdate.forResource(STORAGE).forProject(YANDEX).withMaxDelta(DiSignedAmount.positive(100, DiUnit.BYTE)).build())
                .withReqId("req123");

        final DiQuotaGetResponse response1 = requestBuilder.performBy(SANCHO);
        final DiQuotaGetResponse response2 = requestBuilder.performBy(SANCHO);

        assertIterableEquals(response1, response2);

        updateHierarchy();

        assertEquals(getQuotaMax(NIRVANA, STORAGE, YANDEX), DiAmount.of(200, DiUnit.BYTE));
    }

    @NotNull
    private DiQuota getQuota(@NotNull final String serviceKey, @NotNull final String resourceKey, @NotNull final String projectKey,
                             @NotNull final String... segments) {
        return dispenser().quotas().get()
                .inService(serviceKey)
                .forResource(resourceKey)
                .ofProject(projectKey)
                .withSegments(segments)
                .perform()
                .getFirst();
    }

    @NotNull
    private DiAmount getQuotaMax(@NotNull final String serviceKey, @NotNull final String resourceKey, @NotNull final String projectKey,
                                 @NotNull final String... segments) {
        return getQuota(serviceKey, resourceKey, projectKey, segments).getMax().humanize();
    }

    @NotNull
    private DiAmount getQuotaOwnMax(@NotNull final String serviceKey, @NotNull final String resourceKey, @NotNull final String projectKey,
                                    @NotNull final String... segments) {
        return getQuota(serviceKey, resourceKey, projectKey, segments).getOwnMax().humanize();
    }


    @NotNull
    private DiAmount getQuotaActual(@NotNull final String serviceKey,
                                    @NotNull final String resourceKey,
                                    @NotNull final String projectKey) {
        return getQuota(serviceKey, resourceKey, projectKey).getActual().humanize();
    }

    private String serviceName(@NotNull final String serviceKey) {
        return Hierarchy.get().getServiceReader().read(serviceKey).getName();
    }
}
