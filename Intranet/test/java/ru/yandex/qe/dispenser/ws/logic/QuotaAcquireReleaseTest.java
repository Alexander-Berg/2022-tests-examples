package ru.yandex.qe.dispenser.ws.logic;

import java.util.Optional;

import javax.ws.rs.BadRequestException;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuota;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.request.DiResourceAmount;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaChangeResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.ws.BatchQuotaServiceImpl;
import ru.yandex.qe.dispenser.ws.QuotaReadUpdateService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class QuotaAcquireReleaseTest extends BusinessLogicTestBase {
    /**
     * {@link QuotaReadUpdateService#read}
     */
    @Test
    public void quotaMaxValueCanAcquireButMaxPlusOneCant() {
        final DiQuotaGetResponse canAcquire = dispenser().quotas().get()
                .inService(NIRVANA)
                .byEntitySpecification(YT_FILE)
                .ofProject(INFRA)
                .perform();

        assertEquals(1, canAcquire.size());
        assertTrue(canAcquire.canAcquire(STORAGE, DiAmount.of(100, DiUnit.BYTE)));

        final DiQuotaGetResponse cantAcquire = dispenser().quotas().get()
                .inService(NIRVANA)
                .byEntitySpecification(YT_FILE)
                .ofProject(INFRA)
                .perform();

        assertEquals(1, cantAcquire.size());
        assertFalse(cantAcquire.canAcquire(STORAGE, DiAmount.of(101, DiUnit.BYTE)));
    }

    /**
     * {@link QuotaReadUpdateService#read}
     */
    @Test
    public void canAcquireMayBeAppliedToEntity() {
        final DiQuotaGetResponse canAcquire = dispenser().quotas().get()
                .inService(NIRVANA)
                .byEntitySpecification(YT_FILE)
                .ofProject(INFRA)
                .perform();

        final DiEntity pool = DiEntity.withKey("pool")
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.of(100, DiUnit.BYTE))
                .build();
        assertTrue(canAcquire.canAcquire(pool));
    }

    /**
     * {@link BatchQuotaServiceImpl#changeQuotas}
     */
    @Test
    public void afterAcquireReleaseUserCanAcquire() {
        final DiEntity pool = DiEntity.withKey("pool").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(100, DiUnit.BYTE)).build();
        dispenser().quotas().changeInService(NIRVANA).createEntity(pool, LYADZHIN.chooses(INFRA)).perform();
        dispenser().quotas().changeInService(NIRVANA).releaseEntity(pool).perform();

        quotaMaxValueCanAcquireButMaxPlusOneCant();
    }

    /**
     * {@link BatchQuotaServiceImpl#changeQuotas}
     */
    @Disabled
    @Test
    public void quotaAfterReleaseShouldNotBeBelowZero() {
        dispenser().quotas()
                .changeInService(SCRAPER)
                .releaseResource(DiResourceAmount.ofResource(DOWNLOADS).withAmount(100, DiUnit.COUNT).build(), LYADZHIN.chooses(INFRA))
                .perform();

        updateHierarchy();

        final DiQuotaGetResponse quotas = dispenser().quotas().get().perform();
        quotas.forEach(q -> assertEquals(0, q.getActual().getValue()));
    }

    @Test
    public void canAcquireAnyAmountShouldReturnTrueIfQuotaIsNotFull() {
        final DiQuotaGetResponse response = dispenser().quotas().get()
                .ofProject(INFRA)
                .inService(NIRVANA)
                .byEntitySpecification(YT_FILE)
                .perform();
        assertThat(response.canAcquireAllResources(), is(true));
    }

    @Test
    public void canAcquireAnyAmountShouldReturnFalseIfQuotaIsFull() {
        final DiEntity pool = DiEntity.withKey("pool").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(100, DiUnit.BYTE)).build();
        final DiQuotaChangeResponse acquireResponse = dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(pool, LYADZHIN.chooses(INFRA))
                .perform();
        assertThat(acquireResponse.isSuccess(), is(true));

        updateHierarchy();

        final DiQuotaGetResponse infraQuotas = dispenser().quotas().get()
                .inService(NIRVANA)
                .byEntitySpecification(YT_FILE)
                .ofProject(INFRA)
                .perform();
        assertThat(infraQuotas.canAcquireAllResources(), is(false));
    }

    @Test
    public void newExistingPersonShouldBeAbleToAcquireQuotaInDefaultProject() {
        final DiQuotaChangeResponse resp = dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(UNIT_ENTITY, DiPerson.login("bendyna").chooses(DEFAULT))
                .perform();

        updateHierarchy();

        assertTrue(resp.isSuccess());
        assertActualEquals(dispenser().quotas().get().inLeafs().perform(), STORAGE, DEFAULT, UNIT_ENTITY.getSize(STORAGE).getValue());
    }

    /**
     * DISPENSER-337: Учитывать ответственных за проект как участников
     */
    @Test
    public void projectResponsibleIsProjectMember() {
        final DiQuotaChangeResponse resp = dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(UNIT_ENTITY, WHISTLER.chooses(INFRA))
                .perform();

        assertTrue(resp.isSuccess());
    }

    @Test
    public void acquireOfResourceAmountShouldWorkCorrectly() {
        final DiResourceAmount resourceAmount = DiResourceAmount.ofResource(DOWNLOADS).withAmount(10, DiUnit.COUNT).build();
        dispenser().quotas()
                .changeInService(SCRAPER)
                .acquireResource(resourceAmount, LYADZHIN.chooses(VERTICALI))
                .perform();

        updateHierarchy();

        final DiQuotaGetResponse quotas = dispenser().quotas().get()
                .inService(SCRAPER)
                .forResource(DOWNLOADS)
                .ofProject(VERTICALI)
                .perform();
        quotas.forEach(q -> assertEquals(q.getActual().humanize(), DiAmount.of(10, DiUnit.COUNT)));
    }

    @Test
    public void negativeResourceAmountCannotBeAcquired() {
        assertThrows(BadRequestException.class, () -> {
            final DiResourceAmount resourceAmount = DiResourceAmount.ofResource(DOWNLOADS)
                    .withAmount(DiAmountWithNegativeValueAllowed.of(-10, DiUnit.COUNT))
                    .build();
            dispenser().quotas()
                    .changeInService(SCRAPER)
                    .acquireResource(resourceAmount, LYADZHIN.chooses(VERTICALI))
                    .perform();
        });
    }

    @Test
    public void acquireOfSegmentResourceShouldWorkCorrectly() {
        final DiQuotaGetResponse canAcquire = dispenser().quotas().get()
                .inService(YP)
                .forResource(SEGMENT_CPU)
                .ofProject(INFRA)
                .perform();

        assertTrue(canAcquire.canAcquire(DiResourceAmount.ofResource(SEGMENT_CPU)
                .withAmount(20, DiUnit.CORES)
                .withSegments(Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1))
                .build()));

        assertFalse(canAcquire.canAcquire(DiResourceAmount.ofResource(SEGMENT_CPU)
                .withAmount(20, DiUnit.CORES)
                .withSegments(Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1))
                .build()));
    }

    @Test
    public void acquireOfSegmentResourceShouldAffectAggregationSegments() {
        final DiResourceAmount resourceAmount = DiResourceAmount.ofResource(SEGMENT_CPU)
                .withAmount(20, DiUnit.CORES)
                .withSegments(Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1))
                .build();

        dispenser().quotas()
                .changeInService(YP)
                .acquireResource(resourceAmount, LYADZHIN.chooses(INFRA))
                .perform();

        updateHierarchy();

        final DiQuotaGetResponse canAcquire = dispenser().quotas().get()
                .inService(YP)
                .forResource(SEGMENT_CPU)
                .ofProject(INFRA)
                .perform();

        assertFalse(canAcquire.canAcquire(resourceAmount));

        final Optional<DiQuota> totalSegment = canAcquire.stream()
                .filter(q -> q.getSegmentKeys().isEmpty())
                .findFirst();

        assertEquals(20000, totalSegment.get().getActual().getValue());
    }
}
