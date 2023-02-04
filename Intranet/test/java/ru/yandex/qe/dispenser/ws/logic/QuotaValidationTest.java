package ru.yandex.qe.dispenser.ws.logic;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiPerformer;
import ru.yandex.qe.dispenser.api.v1.DiPersonGroup;
import ru.yandex.qe.dispenser.api.v1.DiProject;
import ru.yandex.qe.dispenser.api.v1.DiQuota;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiActualQuotaUpdate;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.request.DiEntityUsage;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.api.v1.request.DiResourceAmount;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaChangeResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.QuotaSpec;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.ResourceSegmentation;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.quota.QuotaUtils;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.domain.support.EntityUsageDiff;
import ru.yandex.qe.dispenser.ws.BatchQuotaServiceImpl;
import ru.yandex.qe.dispenser.ws.QuotaReadUpdateService;
import ru.yandex.qe.dispenser.ws.ServiceService;
import ru.yandex.qe.dispenser.ws.aspect.AccessAspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class QuotaValidationTest extends BusinessLogicTestBase {

    @Autowired
    private ResourceSegmentationDao resourceSegmentationDao;
    @Autowired
    private PersonDao personDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;

    /**
     * {@link BatchQuotaServiceImpl#changeQuotas}
     */
    @Test
    public void canNotAcquireQuotaNotFromLeaf() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().quotas()
                    .changeInService(NIRVANA)
                    .createEntity(UNIT_ENTITY, LYADZHIN.chooses(YANDEX))
                    .perform();
        });
    }

    /**
     * {@link BatchQuotaServiceImpl#changeQuotas}
     */
    @Test
    public void canAcquireFromDefaultProjectIfPersonIsDismissed() {
        final DiQuotaChangeResponse response = dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(UNIT_ENTITY, DiPerson.login("welvet").chooses(DEFAULT))
                .perform();
        assertTrue(response.isSuccess());
    }

    /**
     * {@link BatchQuotaServiceImpl#changeQuotas}
     */
    @Test
    public void cantAcquireFromDefaultProjectIfPersonDoesNotExist() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().quotas()
                    .changeInService(NIRVANA)
                    .createEntity(UNIT_ENTITY, DiPerson.login("not-existing-person").chooses(DEFAULT))
                    .perform();
        });
    }


    /**
     * {@link QuotaReadUpdateService#update}
     */
    @Test
    public void canNotSetMaxOverParentMax() {
        // TODO: use dispenser() after DISPENSER-105
        final WebClient client = createLocalClient()
                .path("/v1/quotas/" + VERTICALI + "/" + NIRVANA + "/" + STORAGE + "/storage")
                .header("Content-Type", "application/json")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + WHISTLER);
        assertEquals(HttpStatus.SC_BAD_REQUEST, client.post(ImmutableMap.of("maxValue", 151, "unit", DiUnit.BYTE)).getStatus());
        assertEquals(HttpStatus.SC_OK, client.post(ImmutableMap.of("maxValue", 150, "unit", DiUnit.BYTE)).getStatus());
    }

    /**
     * {@link QuotaReadUpdateService#update}
     */
    @Test
    public void canNotSetMaxBelowSubProjectsMax() {
        // TODO: use dispenser() after DISPENSER-105
        final WebClient client = createLocalClient()
                .path("/v1/quotas/" + YANDEX + "/" + NIRVANA + "/" + STORAGE + "/storage")
                .header("Content-Type", "application/json")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + SANCHO);
        assertEquals(HttpStatus.SC_BAD_REQUEST, client.post(ImmutableMap.of("maxValue", 99, "unit", DiUnit.BYTE)).getStatus());
        assertEquals(HttpStatus.SC_OK, client.post(ImmutableMap.of("maxValue", 100, "unit", DiUnit.BYTE)).getStatus());
    }

    /**
     * {@link QuotaReadUpdateService#update}
     */
    @Test
    public void canNotSetMaxToRootIfIsNotAdminOfService() {
        // TODO: use dispenser() after DISPENSER-105
        final WebClient client = createLocalClient()
                .path("/v1/quotas/" + YANDEX + "/" + NIRVANA + "/" + STORAGE + "/storage")
                .header("Content-Type", "application/json")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + WHISTLER);
        assertEquals(HttpStatus.SC_FORBIDDEN, client.post(ImmutableMap.of("maxValue", 250, "unit", DiUnit.BYTE)).getStatus());
    }

    /**
     * {@link QuotaReadUpdateService#update}
     */
    @Test
    public void canNotSetMaxToProjectIfIsNotResponsibleOfParent() {
        disableHierarchy();

        // TODO: use dispenser() after DISPENSER-105
        final DiProject infra = dispenser().project(INFRA).responsibles().attach(LYADZHIN.getLogin()).performBy(WHISTLER);
        Assertions.assertTrue(infra.getResponsibles().getPersons().contains(LYADZHIN.getLogin()));
        final WebClient client = createLocalClient()
                .path("/v1/quotas/" + INFRA + "/" + NIRVANA + "/" + STORAGE + "/storage")
                .header("Content-Type", "application/json")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + LYADZHIN);
        assertEquals(HttpStatus.SC_FORBIDDEN, client.post(ImmutableMap.of("maxValue", 100, "unit", DiUnit.BYTE)).getStatus());
    }

    @Test
    public void canNotAcquireResourceAmountIfEntitiesOnly() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().quotas()
                    .changeInService(NIRVANA)
                    .acquireResource(DiResourceAmount.ofResource(STORAGE).withAmount(1, DiUnit.BYTE).build(), LYADZHIN.chooses(INFRA))
                    .perform();
        });
    }

    @Test
    public void doubleEntityAcquireMustThrowException() {
        assertThrows(WebApplicationException.class, () -> {
            dispenser().quotas().changeInService(NIRVANA).createEntity(UNIT_ENTITY, LYADZHIN.chooses(INFRA)).perform();
            dispenser().quotas().changeInService(NIRVANA).createEntity(UNIT_ENTITY, LYADZHIN.chooses(INFRA)).perform();
        });
    }

    /**
     * DISPENSER-349: Не кидать NPE, если нет такого проекта
     */
    @Test
    public void dontThrowNPEifProjectNotExists() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().quotas()
                    .changeInService(NIRVANA)
                    .createEntity(UNIT_ENTITY, DiPerformer.login("kadymov").chooses("some_project"))
                    .perform();
        });
    }


    /**
     * DISPENSER-441: Разрешить удалять владения всем пользователям группы
     * <p>
     * {@link EntityUsageDiff#processOverUsages}
     */
    @Test
    public void releaseUsageForNoUsageMustFail() {
        assertThrows(BadRequestException.class, () -> {
            final long poolSize = 60;
            final DiEntity pool = DiEntity.withKey("pool")
                    .bySpecification(YT_FILE)
                    .occupies(STORAGE, DiAmount.of(poolSize, DiUnit.BYTE))
                    .build();
            dispenser().quotas()
                    .changeInService(NIRVANA)
                    .createEntity(pool, LYADZHIN.chooses(INFRA))
                    .perform();
            dispenser().quotas()
                    .changeInService(NIRVANA)
                    .releaseEntitySharing(DiEntityUsage.singleOf(pool), LYADZHIN.chooses(VERTICALI))
                    .perform();
        });
    }

    /**
     * DISPENSER-687: Режим ингорирования неизвестных entities при удалении использований
     * <p>
     * {@link EntityUsageDiff#processOverUsages}
     */
    @Test
    public void releaseTooMuchUsageMustFail() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().quotas()
                    .changeInService(NIRVANA)
                    .createEntity(UNIT_ENTITY, LYADZHIN.chooses(INFRA))
                    .releaseEntitySharing(DiEntityUsage.of(UNIT_ENTITY, 2), LYADZHIN.chooses(INFRA))
                    .perform();
        });
    }

    /**
     * DISPENSER-441: Разрешить удалять владения всем пользователям группы
     * <p>
     * {@link EntityUsageDiff#processOverUsages}
     */
    @Test
    public void releaseOfMoreUsagesThanPresentMustFail() {
        assertThrows(BadRequestException.class, () -> {
            final long poolSize = 60;
            final DiEntity pool = DiEntity.withKey("pool")
                    .bySpecification(YT_FILE)
                    .occupies(STORAGE, DiAmount.of(poolSize, DiUnit.BYTE))
                    .build();
            dispenser().quotas()
                    .changeInService(NIRVANA)
                    .createEntity(pool, LYADZHIN.chooses(INFRA))
                    .perform();
            dispenser().quotas()
                    .changeInService(NIRVANA)
                    .releaseEntitySharing(DiEntityUsage.of(pool, 2), LYADZHIN.chooses(INFRA))
                    .perform();
        });
    }

    /**
     * DISPENSER-516: Учитывать админов Dispenser в ответственных проекта
     * <p>
     * {@link AccessAspect#isMember}
     */
    @Test
    public void dispenserAdminIsNotMember() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser().quotas().changeInService(NIRVANA).createEntity(UNIT_ENTITY, AMOSOV_F.chooses(INFRA)).perform();
        });
    }

    /**
     * DISPENSER-610: Транзакционное обновление квот проектов
     * <p>
     * {@link ServiceService#syncQuotas}
     * {@link
     * QuotaUtils#checkValues}
     */
    @Test
    public void canNotReshareQuotaOverParentMax() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().service(NIRVANA).syncState().quotas()
                    .changeQuota(DiQuotaState.forResource(STORAGE).forProject(DEFAULT).withMax(DiAmount.of(200, DiUnit.BYTE)).build())
                    .changeQuota(DiQuotaState.forResource(STORAGE).forProject(INFRA).withMax(DiAmount.of(40, DiUnit.BYTE)).build())
                    .changeQuota(DiQuotaState.forResource(STORAGE).forProject(VERTICALI).withMax(DiAmount.of(20, DiUnit.BYTE)).build())
                    .performBy(WHISTLER);
        });
    }

    /**
     * DISPENSER-610: Транзакционное обновление квот проектов
     * <p>
     * {@link ServiceService#syncQuotas}
     * {@link ServiceService#checkCanUpdateMax}
     */
    @Test
    public void canNotReshareQuotaIfNotResponsibleOfAllParents() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser().service(NIRVANA).syncState().quotas()
                    .changeQuota(DiQuotaState.forResource(STORAGE).forProject(DEFAULT).withMax(DiAmount.of(200, DiUnit.BYTE)).build())
                    .changeQuota(DiQuotaState.forResource(STORAGE).forProject(INFRA).withMax(DiAmount.of(40, DiUnit.BYTE)).build())
                    .changeQuota(DiQuotaState.forResource(STORAGE).forProject(VERTICALI).withMax(DiAmount.of(10, DiUnit.BYTE)).build())
                    .performBy(LYADZHIN);
        });
    }

    /**
     * DISPENSER-1227: Ограничение на распределении квоты больше actual
     */
    @Test
    public void quotaMaxCannotBeSetBelowActualUsingSyncStateMethodForServiceWithSuchRestriction() {
        dispenser().service(YP).syncState().quotas()
                .changeQuota(DiQuotaState
                        .forResource(SEGMENT_STORAGE)
                        .forProject(VERTICALI)
                        .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                        .withActual(DiAmount.of(128, DiUnit.BYTE))
                        .build())
                .perform();

        updateHierarchy();

        final BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            dispenser().service(YP).syncState().quotas()
                    .changeQuota(DiQuotaState
                            .forResource(SEGMENT_STORAGE)
                            .forProject(VERTICALI)
                            .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                            .withMax(DiAmount.of(64, DiUnit.BYTE))
                            .build())
                    .performBy(SLONNN);
        });
        assertTrue(exception.getMessage().contains("is less than actual"));
    }

    /**
     * DISPENSER-1227: Ограничение на распределении квоты больше actual
     */
    @Test
    public void quotaMaxCannotBeSetBelowActualUsingUpdateQuotaMethodForServiceWithSuchRestriction() {
        final DiResourceAmount amount = DiResourceAmount.ofResource(SEGMENT_CPU)
                .withAmount(20, DiUnit.CORES)
                .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                .build();
        dispenser().quotas()
                .changeInService(YP)
                .acquireResource(amount, LYADZHIN.chooses(INFRA))
                .perform();

        updateHierarchy();

        final Response response = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/quotas/infra/yp/segment-cpu/segment-cpu")
                .post(ImmutableMap.of(
                        "maxValue", 10,
                        "unit", DiUnit.CORES,
                        "segments", Collections.singleton(DC_SEGMENT_1)
                ));
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        assertTrue(response.readEntity(String.class).contains("is less than actual"));
    }

    @Test
    public void overquotingIsAllowedForServiceWithSuchRestriction() {
        final HashSet<String> segments = Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1);

        dispenser().service(YP).syncState().quotas()
                .changeQuota(DiQuotaState
                        .forResource(SEGMENT_STORAGE)
                        .forProject(VERTICALI)
                        .withSegments(segments)
                        .withActual(DiAmount.of(512, DiUnit.BYTE))
                        .build())
                .perform();

        updateHierarchy();

        final DiQuota quota = dispenser().quotas().get()
                .inService(YP)
                .forResource(SEGMENT_STORAGE)
                .ofProject(VERTICALI)
                .withSegments(segments)
                .perform()
                .getFirst();

        assertEquals(256, quota.getMax(DiUnit.BYTE));
        assertEquals(512, quota.getActual(DiUnit.BYTE));
    }

    /**
     * DISPENSER-1227: Ограничение на распределении квоты больше actual
     */
    @Test
    public void quotaMaxCanBeSetBelowActualForServiceWithoutSuchRestriction() {
        final DiEntity entity = DiEntity.withKey("yt-file")
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.of(50, DiUnit.BYTE))
                .build();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(entity, LYADZHIN.chooses(INFRA))
                .perform();

        updateHierarchy();

        final long actual = dispenser().quotas().get()
                .inService(NIRVANA)
                .forResource(STORAGE)
                .ofProject(INFRA)
                .perform()
                .getFirst()
                .getActual(DiUnit.BYTE);

        assertEquals(50, actual);

        // check max can be updated using sync state method

        dispenser().service(NIRVANA).syncState().quotas()
                .changeQuota(DiQuotaState
                        .forResource(STORAGE)
                        .forProject(INFRA)
                        .withMax(DiAmount.of(40, DiUnit.BYTE))
                        .build())
                .performBy(WHISTLER);

        updateHierarchy();

        final long max = dispenser().quotas().get()
                .inService(NIRVANA)
                .forResource(STORAGE)
                .ofProject(INFRA)
                .perform()
                .getFirst()
                .getMax(DiUnit.BYTE);

        assertEquals(40, max);

        // check max can be updated using update quota method

        createAuthorizedLocalClient(WHISTLER)
                .path("/v1/quotas/infra/nirvana/storage/storage")
                .post(ImmutableMap.of(
                        "maxValue", 30,
                        "unit", DiUnit.BYTE
                ));

        updateHierarchy();

        final long max2 = dispenser().quotas().get()
                .inService(NIRVANA)
                .forResource(STORAGE)
                .ofProject(INFRA)
                .perform()
                .getFirst()
                .getMax(DiUnit.BYTE);

        assertEquals(30, max2);
    }

    @Test
    public void allOperationsAreSupportedForRemovedProjects() {
        dispenser().project(INFRA).delete().performBy(AMOSOV_F);
        updateHierarchy();

        final DiEntity diEntity = DiEntity.withKey("pool").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(10, DiUnit.BYTE)).build();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(diEntity, LYADZHIN.chooses(INFRA))
                .perform();
        updateHierarchy();
        final DiQuotaGetResponse createEntityQuotas = dispenser().quotas().get().inService(NIRVANA).perform();
        final DiQuota createEntityInfraQuota = createEntityQuotas.stream()
                .filter(q -> q.getProject().getKey().equals(INFRA) && q.getSpecification().getKey().equals(STORAGE))
                .findFirst().get();
        assertEquals(10, createEntityInfraQuota.getActual(DiUnit.BYTE));


        dispenser().quotas()
                .changeInService(NIRVANA)
                .shareEntity(DiEntityUsage.singleOf(diEntity), LYADZHIN.chooses(VERTICALI))
                .perform();
        updateHierarchy();
        final DiQuotaGetResponse shareEntityQuotas = dispenser().quotas().get().inService(NIRVANA).perform();
        final DiQuota shareEntityInfraQuota = shareEntityQuotas.stream()
                .filter(q -> q.getProject().getKey().equals(INFRA) && q.getSpecification().getKey().equals(STORAGE))
                .findFirst().get();
        assertEquals(5, shareEntityInfraQuota.getActual(DiUnit.BYTE));


        dispenser().quotas()
                .changeInService(NIRVANA)
                .releaseEntitySharing(DiEntityUsage.singleOf(diEntity), LYADZHIN.chooses(VERTICALI))
                .perform();
        updateHierarchy();
        final DiQuotaGetResponse releaseEntitySharingQuotas = dispenser().quotas().get().inService(NIRVANA).perform();
        final DiQuota releaseEntitySharingInfraQuota = releaseEntitySharingQuotas.stream()
                .filter(q -> q.getProject().getKey().equals(INFRA) && q.getSpecification().getKey().equals(STORAGE))
                .findFirst().get();
        assertEquals(10, releaseEntitySharingInfraQuota.getActual(DiUnit.BYTE));


        dispenser().quotas()
                .changeInService(NIRVANA)
                .releaseEntity(diEntity)
                .perform();
        updateHierarchy();
        final DiQuotaGetResponse releaseEntityQuotas = dispenser().quotas().get().inService(NIRVANA).perform();
        final DiQuota releaseEntityInfraQuota = releaseEntityQuotas.stream()
                .filter(q -> q.getProject().getKey().equals(INFRA) && q.getSpecification().getKey().equals(STORAGE))
                .findFirst().get();
        assertEquals(0, releaseEntityInfraQuota.getActual(DiUnit.BYTE));


        dispenser().quotas()
                .changeInService(YP)
                .acquireResource(DiResourceAmount.ofResource(SEGMENT_CPU)
                        .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                        .withAmount(10, DiUnit.CORES).build(), LYADZHIN.chooses(INFRA))
                .perform();
        updateHierarchy();
        final DiQuotaGetResponse acquireResourceQuotas = dispenser().quotas().get().inService(YP).forResource(SEGMENT_CPU).perform();
        final DiQuota acquireResourceInfraQuota = findQuota(acquireResourceQuotas, INFRA, Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1));
        assertEquals(10, acquireResourceInfraQuota.getActual(DiUnit.CORES));


        dispenser().quotas()
                .changeInService(YP)
                .releaseResource(DiResourceAmount.ofResource(SEGMENT_CPU)
                        .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                        .withAmount(7, DiUnit.CORES).build(), LYADZHIN.chooses(INFRA))
                .perform();
        updateHierarchy();
        final DiQuotaGetResponse releaseResourceQuotas = dispenser().quotas().get().inService(YP).forResource(SEGMENT_CPU).perform();
        final DiQuota releaseResourceInfraQuota = findQuota(releaseResourceQuotas, INFRA, Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1));
        assertEquals(3, releaseResourceInfraQuota.getActual(DiUnit.CORES));
    }

    @Test
    public void quotaActualValuesCanBeChangedForRemovedProjects() {
        dispenser().project(VERTICALI).delete().performBy(AMOSOV_F);
        updateHierarchy();

        final HashSet<String> segments = Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1);

        dispenser().service(YP).syncState().quotas()
                .changeQuota(DiQuotaState
                        .forResource(SEGMENT_STORAGE)
                        .forProject(VERTICALI)
                        .withSegments(segments)
                        .withActual(DiAmount.of(55, DiUnit.BYTE))
                        .build())
                .perform();

        updateHierarchy();

        final DiQuota quota = dispenser().quotas().get()
                .inService(YP)
                .forResource(SEGMENT_STORAGE)
                .withSegments(segments)
                .perform()
                .stream()
                .filter(q -> q.getProject().getKey().equals(VERTICALI)).findFirst().get();

        assertEquals(55, quota.getActual(DiUnit.BYTE));
    }

    @Test
    public void quotaActualValuesCanBeChangedForRemovedProjectsByDedicatedEndpoint() {
        dispenser().project(VERTICALI).delete().performBy(AMOSOV_F);
        updateHierarchy();

        final HashSet<String> segments = Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1);

        dispenser().service(YP).syncState().actualQuotas()
                .changeActualQuota(DiActualQuotaUpdate
                        .forResource(SEGMENT_STORAGE)
                        .project(VERTICALI)
                        .addSegments(segments)
                        .actual(DiAmount.of(55, DiUnit.BYTE))
                        .build())
                .perform();

        updateHierarchy();

        final DiQuota quota = dispenser().quotas().get()
                .inService(YP)
                .forResource(SEGMENT_STORAGE)
                .withSegments(segments)
                .perform()
                .stream()
                .filter(q -> q.getProject().getKey().equals(VERTICALI)).findFirst().get();

        assertEquals(55, quota.getActual(DiUnit.BYTE));
    }

    @Test
    public void quotaActualValuesCanBeChangedRawForRemovedProjects() {
        dispenser().project(VERTICALI).delete().performBy(AMOSOV_F);
        updateHierarchy();

        final HashSet<String> segments = Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1);

        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        dispenser().service(YP).syncState().rawQuotas()
                .changeRawQuota(DiQuotaState
                        .forResource(SEGMENT_STORAGE)
                        .forProject(VERTICALI)
                        .withSegments(segments)
                        .withActual(DiAmount.of(55, DiUnit.BYTE))
                        .build())
                .perform();

        updateHierarchy();

        final DiQuota quota = dispenser().quotas().get()
                .inService(YP)
                .forResource(SEGMENT_STORAGE)
                .withSegments(segments)
                .perform()
                .stream()
                .filter(q -> q.getProject().getKey().equals(VERTICALI)).findFirst().get();

        assertEquals(55, quota.getActual(DiUnit.BYTE));
    }

    @Test
    public void quotaMaxValuesCantBeChangedForRemovedProjects() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().project(VERTICALI).delete().performBy(AMOSOV_F);
            updateHierarchy();

            final HashSet<String> segments = Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1);

            dispenser().service(YP).syncState().quotas()
                    .changeQuota(DiQuotaState
                            .forResource(SEGMENT_STORAGE)
                            .forProject(VERTICALI)
                            .withSegments(segments)
                            .withMax(DiAmount.of(555, DiUnit.BYTE))
                            .build())
                    .perform();
        });
    }

    @Test
    public void quotaMaxValuesCantBeChangedRawForRemovedProjects() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().project(VERTICALI).delete().performBy(AMOSOV_F);
            updateHierarchy();

            final HashSet<String> segments = Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1);

            dispenser()
                    .properties()
                    .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                    .performBy(AMOSOV_F);
            updateHierarchy();
            dispenser().service(YP).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState
                            .forResource(SEGMENT_STORAGE)
                            .forProject(VERTICALI)
                            .withSegments(segments)
                            .withMax(DiAmount.of(555, DiUnit.BYTE))
                            .build())
                    .perform();
        });
    }

    @Test
    public void cantChangeMaxForRemovedProject() {
        dispenser().project(YANDEX).delete().performBy(AMOSOV_F);
        updateHierarchy();
        final WebClient client = createLocalClient()
                .path("/v1/quotas/" + YANDEX + "/" + NIRVANA + "/" + STORAGE + "/storage")
                .header("Content-Type", "application/json")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + SANCHO);
        assertEquals(HttpStatus.SC_NOT_FOUND, client.post(ImmutableMap.of("maxValue", 100, "unit", DiUnit.BYTE)).getStatus());
    }

    @Test
    public void quotaOwnMaxValuesCantBeChangedForRemovedProjects() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().project(VERTICALI).delete().performBy(AMOSOV_F);
            updateHierarchy();

            final HashSet<String> segments = Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1);

            dispenser().service(YP).syncState().quotas()
                    .changeQuota(DiQuotaState
                            .forResource(SEGMENT_STORAGE)
                            .forProject(VERTICALI)
                            .withSegments(segments)
                            .withOwnMax(DiAmount.of(555, DiUnit.BYTE))
                            .build())
                    .perform();
        });
    }

    @Test
    public void quotaOwnMaxValuesCantBeChangedRawForRemovedProjects() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().project(VERTICALI).delete().performBy(AMOSOV_F);
            updateHierarchy();

            final HashSet<String> segments = Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1);

            dispenser()
                    .properties()
                    .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                    .performBy(AMOSOV_F);
            updateHierarchy();
            dispenser().service(YP).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState
                            .forResource(SEGMENT_STORAGE)
                            .forProject(VERTICALI)
                            .withSegments(segments)
                            .withOwnMax(DiAmount.of(555, DiUnit.BYTE))
                            .build())
                    .perform();

        });
    }

    /**
     * DISPENSER-1257: Доработки ограничения на распределение квоты
     */
    @Test
    public void quotaActualValuesShouldBeAccountedWhenSettingMaxValuesForServicesWithSuchRestriction() {
        final HashSet<String> segments = Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1);

        dispenser().service(YP).syncState().quotas()
                .changeQuota(DiQuotaState
                        .forResource(SEGMENT_STORAGE)
                        .forProject(VERTICALI)
                        .withSegments(segments)
                        .withActual(DiAmount.of(512, DiUnit.BYTE))
                        .build())
                .perform();

        updateHierarchy();

        // State:
        //   yandex: max 768, actual 512
        //   verticali: max 256, actual 512
        //   infra: max 0, actual 0
        // Free quota accounting max values only: 512
        // Free quota accounting verticali actual value: 256

        // Can't set 512 for infra, only 256 is free
        final BadRequestException infraException = assertThrows(BadRequestException.class, () -> {
            dispenser().service(YP).syncState().quotas()
                    .changeQuota(DiQuotaState
                            .forResource(SEGMENT_STORAGE)
                            .forProject(INFRA)
                            .withSegments(segments)
                            .withMax(DiAmount.of(512, DiUnit.BYTE))
                            .build())
                    .performBy(SLONNN);
        });
        assertTrue(infraException.getMessage().contains("is less than subprojects usage"));
        assertTrue(infraException.getMessage().contains("Yandex"));

        // Can set 256 for infra
        dispenser().service(YP).syncState().quotas()
                .changeQuota(DiQuotaState
                        .forResource(SEGMENT_STORAGE)
                        .forProject(INFRA)
                        .withSegments(segments)
                        .withMax(DiAmount.of(256, DiUnit.BYTE))
                        .build())
                .performBy(SLONNN);

        // State:
        //   yandex: max 768, actual 512
        //   verticali: max 256, actual 512
        //   infra: max 256, actual 0
        // Free quota accounting max values only: 256
        // Free quota accounting verticali actual value: 0

        // Can't set less then 768 for yandex
        final BadRequestException yandexException = assertThrows(BadRequestException.class, () -> {
            dispenser().service(YP).syncState().quotas()
                    .changeQuota(DiQuotaState
                            .forResource(SEGMENT_STORAGE)
                            .forProject(YANDEX)
                            .withSegments(segments)
                            .withMax(DiAmount.of(767, DiUnit.BYTE))
                            .build())
                    .performBy(SLONNN);
        });
        assertTrue(yandexException.getMessage().contains("is less than subprojects usage"));
        assertTrue(yandexException.getMessage().contains("Yandex"));

        // Can set less then 768 for yandex along with decreasing verticali actual value
        // Note: overquoting is allowed for verticali in this case — check is applied only when max value is edited
        dispenser().service(YP).syncState().quotas()
                .changeQuota(DiQuotaState
                        .forResource(SEGMENT_STORAGE)
                        .forProject(YANDEX)
                        .withSegments(segments)
                        .withMax(DiAmount.of(640, DiUnit.BYTE))
                        .build())
                .changeQuota(DiQuotaState
                        .forResource(SEGMENT_STORAGE)
                        .forProject(VERTICALI)
                        .withSegments(segments)
                        .withActual(DiAmount.of(384, DiUnit.BYTE))
                        .build())
                .perform();

        // State:
        //   yandex: max 640, actual 384
        //   verticali: max 256, actual 384
        //   infra: max 256, actual 0
        // Free quota accounting max values only: 128
        // Free quota accounting verticali actual value: 0

        // Can't set max less then actual value,
        // in this case overquoting is detected for verticali
        final BadRequestException verticaliException = assertThrows(BadRequestException.class, () -> {
            dispenser().service(YP).syncState().quotas()
                    .changeQuota(DiQuotaState
                            .forResource(SEGMENT_STORAGE)
                            .forProject(VERTICALI)
                            .withSegments(segments)
                            .withMax(DiAmount.of(128, DiUnit.BYTE))
                            .build())
                    .perform();
        });
        assertTrue(verticaliException.getMessage().contains("is less than actual"));
        assertTrue(verticaliException.getMessage().contains("Verticali"));
    }

    public static Object[][] createAggregationSegments() {
        return new Object[][]{
                {Collections.emptySet()},
                {Collections.singleton(DC_SEGMENT_1)},
                {Collections.singleton(DC_SEGMENT_2)},
                {Collections.singleton(SEGMENT_SEGMENT_1)}
        };
    }

    @MethodSource("createAggregationSegments")
    @ParameterizedTest
    public void cannotSyncQuotaActualValuesForQuotasWithAggregationSegments(final Collection<String> segmentKeys) {
        final BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            dispenser().service(YP).syncState().quotas()
                    .changeQuota(DiQuotaState
                            .forResource(SEGMENT_STORAGE)
                            .forProject(VERTICALI)
                            .withSegments(segmentKeys)
                            .withActual(DiAmount.anyOf(DiUnit.BYTE))
                            .build())
                    .perform();
        });
        assertTrue(exception.getMessage().contains("Quota actual values cannot be synced for quotas with aggregation segments"));
    }

    @MethodSource("createAggregationSegments")
    @ParameterizedTest
    public void cannotSyncQuotaActualValuesForQuotasWithAggregationSegmentsByDedicatedEndpoint(final Collection<String> segmentKeys) {
        final BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            dispenser().service(YP).syncState().actualQuotas()
                    .changeActualQuota(DiActualQuotaUpdate
                            .forResource(SEGMENT_STORAGE)
                            .project(VERTICALI)
                            .addSegments(segmentKeys)
                            .actual(DiAmount.anyOf(DiUnit.BYTE))
                            .build())
                    .perform();
        });
        assertTrue(exception.getMessage().contains("Quota actual values can not be synced for quotas with aggregation segments."));
    }

    @MethodSource("createAggregationSegments")
    @ParameterizedTest
    public void cannotSyncRawQuotaActualValuesForQuotasWithAggregationSegments(final Collection<String> segmentKeys) {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            dispenser().service(YP).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState
                            .forResource(SEGMENT_STORAGE)
                            .forProject(VERTICALI)
                            .withSegments(segmentKeys)
                            .withActual(DiAmount.anyOf(DiUnit.BYTE))
                            .build())
                    .perform();
        });
        assertTrue(exception.getMessage().contains("Quota actual values can not be synced for quotas with aggregation segments"));
    }

    @MethodSource("createAggregationSegments")
    @ParameterizedTest
    public void cannotAcquireResourceAmountWithAggregationSegments(final Collection<String> segmentKeys) {
        final DiResourceAmount amount = DiResourceAmount.ofResource(SEGMENT_CPU)
                .withSegments(segmentKeys)
                .withAmount(DiAmount.anyOf(DiUnit.COUNT))
                .build();
        final BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            dispenser().quotas().changeInService(YP).acquireResource(amount, LYADZHIN.chooses(INFRA)).perform();
        });
        assertTrue(exception.getMessage().contains("Cannot acquire/release resource amount with aggregation segments"));
    }

    @MethodSource("createAggregationSegments")
    @ParameterizedTest
    public void cannotCreateEntityOccupyingResourceAmountWithAggregationSegments(final Collection<String> segmentKeys) {
        final DiResourceAmount resourceAmount = DiResourceAmount.ofResource(SEGMENT_HDD)
                .withSegments(segmentKeys)
                .withAmount(DiAmount.anyOf(DiUnit.BYTE))
                .build();
        final DiEntity entity = DiEntity.withKey("file")
                .bySpecification(YP_HDD_FILE)
                .occupies(resourceAmount)
                .build();
        final BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            dispenser().quotas().changeInService(YP).createEntity(entity, LYADZHIN.chooses(INFRA)).perform();
        });
        assertTrue(exception.getMessage().contains("Cannot create entity occupying resource amount with aggregation segments"));
    }

    public static Object[][] createInvalidSegmentCombinations() {
        return new Object[][]{
                {Arrays.asList(DC_SEGMENT_1, DC_SEGMENT_2)},
                {Arrays.asList(DC_SEGMENT_1, DC_SEGMENT_2, SEGMENT_SEGMENT_1)}
        };
    }

    @MethodSource("createInvalidSegmentCombinations")
    @ParameterizedTest
    public void cannotSyncActualValueForQuotaWithInvalidSegmentCombination(final Collection<String> segmentKeys) {
        final BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            dispenser().service(YP).syncState().quotas()
                    .changeQuota(DiQuotaState
                            .forResource(SEGMENT_STORAGE)
                            .forProject(VERTICALI)
                            .withSegments(segmentKeys)
                            .withActual(DiAmount.anyOf(DiUnit.BYTE))
                            .build())
                    .perform();
        });
        assertTrue(exception.getMessage().contains("Invalid segment combination"));
    }

    @MethodSource("createInvalidSegmentCombinations")
    @ParameterizedTest
    public void cannotSyncActualValueForQuotaWithInvalidSegmentCombinationByDedicatedEndpoint(final Collection<String> segmentKeys) {
        final BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            dispenser().service(YP).syncState().actualQuotas()
                    .changeActualQuota(DiActualQuotaUpdate
                            .forResource(SEGMENT_STORAGE)
                            .project(VERTICALI)
                            .addSegments(segmentKeys)
                            .actual(DiAmount.anyOf(DiUnit.BYTE))
                            .build())
                    .perform();
        });
        assertTrue(exception.getMessage().contains("Invalid segment combination"));
    }

    @MethodSource("createInvalidSegmentCombinations")
    @ParameterizedTest
    public void cannotRawSyncActualValueForQuotaWithInvalidSegmentCombination(final Collection<String> segmentKeys) {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            dispenser().service(YP).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState
                            .forResource(SEGMENT_STORAGE)
                            .forProject(VERTICALI)
                            .withSegments(segmentKeys)
                            .withActual(DiAmount.anyOf(DiUnit.BYTE))
                            .build())
                    .perform();
        });
        assertTrue(exception.getMessage().contains("Invalid segment combination"));
    }

    @MethodSource("createInvalidSegmentCombinations")
    @ParameterizedTest
    public void cannotSetMaxValueForQuotaWithInvalidSegmentCombination(final Collection<String> segmentKeys) {
        final BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            dispenser().service(YP).syncState().quotas()
                    .changeQuota(DiQuotaState
                            .forResource(SEGMENT_STORAGE)
                            .forProject(YANDEX)
                            .withSegments(segmentKeys)
                            .withMax(DiAmount.anyOf(DiUnit.BYTE))
                            .build())
                    .perform();
        });
        assertTrue(exception.getMessage().contains("Invalid segment combination"));
    }

    @MethodSource("createInvalidSegmentCombinations")
    @ParameterizedTest
    public void cannotRawSetMaxValueForQuotaWithInvalidSegmentCombination(final Collection<String> segmentKeys) {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        final BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            dispenser().service(YP).syncState().rawQuotas()
                    .changeRawQuota(DiQuotaState
                            .forResource(SEGMENT_STORAGE)
                            .forProject(YANDEX)
                            .withSegments(segmentKeys)
                            .withMax(DiAmount.anyOf(DiUnit.BYTE))
                            .build())
                    .perform();
        });
        assertTrue(exception.getMessage().contains("Invalid segment combination"));
    }

    @MethodSource("createInvalidSegmentCombinations")
    @ParameterizedTest
    public void cannotAcquireResourceAmountWithInvalidSegmentCombination(final Collection<String> segmentKeys) {
        final BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            final DiResourceAmount amount = DiResourceAmount.ofResource(SEGMENT_CPU)
                    .withAmount(DiAmount.anyOf(DiUnit.COUNT))
                    .withSegments(segmentKeys)
                    .build();
            dispenser().quotas().changeInService(YP)
                    .acquireResource(amount, LYADZHIN.chooses(INFRA))
                    .perform();
        });
        assertTrue(exception.getMessage().contains("Invalid segment combination"));
    }

    @MethodSource("createInvalidSegmentCombinations")
    @ParameterizedTest
    public void cannotCreateEntityOccupyingResourceAmountWithInvalidSegmentCombination(final Collection<String> segmentKeys) {
        final BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            final DiResourceAmount amount = DiResourceAmount.ofResource(SEGMENT_HDD)
                    .withAmount(DiAmount.anyOf(DiUnit.BYTE))
                    .withSegments(segmentKeys)
                    .build();
            final DiEntity entity = DiEntity.withKey("file")
                    .bySpecification(YP_HDD_FILE)
                    .occupies(amount)
                    .build();
            dispenser().quotas().changeInService(YP)
                    .createEntity(entity, LYADZHIN.chooses(INFRA))
                    .perform();
        });
        assertTrue(exception.getMessage().contains("Invalid segment combination"));
    }

    @MethodSource("createInvalidSegmentCombinations")
    @ParameterizedTest
    public void cannotUpdateQuotaWithInvalidSegmentCombination(final Collection<String> segmentKeys) {
        final Response response = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/quotas/infra/yp/segment-cpu/segment-cpu")
                .post(ImmutableMap.of(
                        "maxValue", 10,
                        "unit", DiUnit.COUNT,
                        "segments", segmentKeys
                ));
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        assertTrue(response.readEntity(String.class).contains("Invalid segment combination"));
    }

    @Test
    public void acquiredQuotaActualValuesShouldBeAggregatedCorrectly() {
        dispenser().quotas()
                .changeInService(YP)
                .acquireResource(DiResourceAmount.ofResource(SEGMENT_CPU).withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1).withAmount(10, DiUnit.CORES).build(), LYADZHIN.chooses(INFRA))
                .acquireResource(DiResourceAmount.ofResource(SEGMENT_CPU).withSegments(DC_SEGMENT_2, SEGMENT_SEGMENT_1).withAmount(20, DiUnit.CORES).build(), LYADZHIN.chooses(INFRA))
                .acquireResource(DiResourceAmount.ofResource(SEGMENT_CPU).withSegments(DC_SEGMENT_3, SEGMENT_SEGMENT_1).withAmount(20, DiUnit.CORES).build(), LYADZHIN.chooses(INFRA))
                .acquireResource(DiResourceAmount.ofResource(SEGMENT_CPU).withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_2).withAmount(30, DiUnit.CORES).build(), LYADZHIN.chooses(INFRA))
                .acquireResource(DiResourceAmount.ofResource(SEGMENT_CPU).withSegments(DC_SEGMENT_2, SEGMENT_SEGMENT_2).withAmount(40, DiUnit.CORES).build(), LYADZHIN.chooses(INFRA))
                .acquireResource(DiResourceAmount.ofResource(SEGMENT_CPU).withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1).withAmount(50, DiUnit.CORES).build(), LYADZHIN.chooses(VERTICALI))
                .acquireResource(DiResourceAmount.ofResource(SEGMENT_CPU).withSegments(DC_SEGMENT_2, SEGMENT_SEGMENT_1).withAmount(60, DiUnit.CORES).build(), LYADZHIN.chooses(VERTICALI))
                .acquireResource(DiResourceAmount.ofResource(SEGMENT_CPU).withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_2).withAmount(70, DiUnit.CORES).build(), LYADZHIN.chooses(VERTICALI))
                .acquireResource(DiResourceAmount.ofResource(SEGMENT_CPU).withSegments(DC_SEGMENT_2, SEGMENT_SEGMENT_2).withAmount(80, DiUnit.CORES).build(), LYADZHIN.chooses(VERTICALI))
                .perform();

        updateHierarchy();

        final DiQuotaGetResponse quotas = dispenser().quotas().get().inService(YP).forResource(SEGMENT_CPU).perform();

        final DiQuota yandexDc1S1Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1));
        final DiQuota yandexDc2S1Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1));
        final DiQuota yandexDc1S2Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_2));
        final DiQuota yandexDc2S2Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_2));
        final DiQuota yandexDc1TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(DC_SEGMENT_1));
        final DiQuota yandexDc2TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(DC_SEGMENT_2));
        final DiQuota yandexS1TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(SEGMENT_SEGMENT_1));
        final DiQuota yandexS2TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(SEGMENT_SEGMENT_2));
        final DiQuota yandexTotalQuota = findQuota(quotas, YANDEX, Collections.emptySet());

        final DiQuota infraDc1TotalQuota = findQuota(quotas, INFRA, Collections.singleton(DC_SEGMENT_1));
        final DiQuota infraDc2TotalQuota = findQuota(quotas, INFRA, Collections.singleton(DC_SEGMENT_2));
        final DiQuota infraS1TotalQuota = findQuota(quotas, INFRA, Collections.singleton(SEGMENT_SEGMENT_1));
        final DiQuota infraS2TotalQuota = findQuota(quotas, INFRA, Collections.singleton(SEGMENT_SEGMENT_2));
        final DiQuota infraTotalQuota = findQuota(quotas, INFRA, Collections.emptySet());

        final DiQuota verticaliDc1TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(DC_SEGMENT_1));
        final DiQuota verticaliDc2TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(DC_SEGMENT_2));
        final DiQuota verticaliS1TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(SEGMENT_SEGMENT_1));
        final DiQuota verticaliS2TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(SEGMENT_SEGMENT_2));
        final DiQuota verticaliTotalQuota = findQuota(quotas, VERTICALI, Collections.emptySet());

        assertEquals(60, yandexDc1S1Quota.getActual(DiUnit.CORES));
        assertEquals(80, yandexDc2S1Quota.getActual(DiUnit.CORES));
        assertEquals(100, yandexDc1S2Quota.getActual(DiUnit.CORES));
        assertEquals(120, yandexDc2S2Quota.getActual(DiUnit.CORES));
        assertEquals(160, yandexDc1TotalQuota.getActual(DiUnit.CORES));
        assertEquals(200, yandexDc2TotalQuota.getActual(DiUnit.CORES));
        assertEquals(160, yandexS1TotalQuota.getActual(DiUnit.CORES));
        assertEquals(220, yandexS2TotalQuota.getActual(DiUnit.CORES));
        assertEquals(380, yandexTotalQuota.getActual(DiUnit.CORES));

        assertEquals(40, infraDc1TotalQuota.getActual(DiUnit.CORES));
        assertEquals(60, infraDc2TotalQuota.getActual(DiUnit.CORES));
        assertEquals(50, infraS1TotalQuota.getActual(DiUnit.CORES));
        assertEquals(70, infraS2TotalQuota.getActual(DiUnit.CORES));
        assertEquals(120, infraTotalQuota.getActual(DiUnit.CORES));

        assertEquals(120, verticaliDc1TotalQuota.getActual(DiUnit.CORES));
        assertEquals(140, verticaliDc2TotalQuota.getActual(DiUnit.CORES));
        assertEquals(110, verticaliS1TotalQuota.getActual(DiUnit.CORES));
        assertEquals(150, verticaliS2TotalQuota.getActual(DiUnit.CORES));
        assertEquals(260, verticaliTotalQuota.getActual(DiUnit.CORES));
    }

    @Test
    public void syncedQuotaActualValuesShouldBeAggregatedCorrectly() {
        dispenser().service(YP).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1).withActual(DiAmount.of(10, DiUnit.TEBIBYTE)).forProject(INFRA).build())
                .changeQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_2, SEGMENT_SEGMENT_1).withActual(DiAmount.of(20, DiUnit.TEBIBYTE)).forProject(INFRA).build())
                .changeQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_3, SEGMENT_SEGMENT_1).withActual(DiAmount.of(30, DiUnit.TEBIBYTE)).forProject(INFRA).build())
                .changeQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_2, SEGMENT_SEGMENT_2).withActual(DiAmount.of(40, DiUnit.TEBIBYTE)).forProject(INFRA).build())
                .changeQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_2).withActual(DiAmount.of(50, DiUnit.TEBIBYTE)).forProject(VERTICALI).build())
                .changeQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_2, SEGMENT_SEGMENT_2).withActual(DiAmount.of(60, DiUnit.TEBIBYTE)).forProject(VERTICALI).build())
                .changeQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_3, SEGMENT_SEGMENT_2).withActual(DiAmount.of(70, DiUnit.TEBIBYTE)).forProject(VERTICALI).build())
                .changeQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_3, SEGMENT_SEGMENT_1).withActual(DiAmount.of(80, DiUnit.TEBIBYTE)).forProject(VERTICALI).build())
                .perform();

        updateHierarchy();

        final DiQuotaGetResponse quotas = dispenser().quotas().get().inService(YP).forResource(SEGMENT_STORAGE).perform();

        final DiQuota infraDc1TotalQuota = findQuota(quotas, INFRA, Collections.singleton(DC_SEGMENT_1));
        final DiQuota infraDc2TotalQuota = findQuota(quotas, INFRA, Collections.singleton(DC_SEGMENT_2));
        final DiQuota infraDc3TotalQuota = findQuota(quotas, INFRA, Collections.singleton(DC_SEGMENT_3));
        final DiQuota infraS1TotalQuota = findQuota(quotas, INFRA, Collections.singleton(SEGMENT_SEGMENT_1));
        final DiQuota infraS2TotalQuota = findQuota(quotas, INFRA, Collections.singleton(SEGMENT_SEGMENT_2));
        final DiQuota infraTotalQuota = findQuota(quotas, INFRA, Collections.emptySet());

        assertEquals(10, infraDc1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(60, infraDc2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(30, infraDc3TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(60, infraS1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(40, infraS2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(100, infraTotalQuota.getActual(DiUnit.TEBIBYTE));

        final DiQuota verticaliDc1TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(DC_SEGMENT_1));
        final DiQuota verticaliDc2TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(DC_SEGMENT_2));
        final DiQuota verticaliDc3TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(DC_SEGMENT_3));
        final DiQuota verticaliS1TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(SEGMENT_SEGMENT_1));
        final DiQuota verticaliS2TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(SEGMENT_SEGMENT_2));
        final DiQuota verticaliTotalQuota = findQuota(quotas, VERTICALI, Collections.emptySet());

        assertEquals(50, verticaliDc1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(60, verticaliDc2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(150, verticaliDc3TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(80, verticaliS1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(180, verticaliS2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(260, verticaliTotalQuota.getActual(DiUnit.TEBIBYTE));

        final DiQuota yandexDc1S1Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1));
        final DiQuota yandexDc2S1Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1));
        final DiQuota yandexDc3S1Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_3, SEGMENT_SEGMENT_1));
        final DiQuota yandexDc1S2Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_2));
        final DiQuota yandexDc2S2Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_2));
        final DiQuota yandexDc3S2Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_3, SEGMENT_SEGMENT_2));
        final DiQuota yandexDc1TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(DC_SEGMENT_1));
        final DiQuota yandexDc2TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(DC_SEGMENT_2));
        final DiQuota yandexDc3TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(DC_SEGMENT_3));
        final DiQuota yandexS1TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(SEGMENT_SEGMENT_1));
        final DiQuota yandexS2TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(SEGMENT_SEGMENT_2));
        final DiQuota yandexTotalQuota = findQuota(quotas, YANDEX, Collections.emptySet());

        assertEquals(10, yandexDc1S1Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(20, yandexDc2S1Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(110, yandexDc3S1Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(50, yandexDc1S2Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(100, yandexDc2S2Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(70, yandexDc3S2Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(60, yandexDc1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(120, yandexDc2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(180, yandexDc3TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(140, yandexS1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(220, yandexS2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(360, yandexTotalQuota.getActual(DiUnit.TEBIBYTE));
    }

    @Test
    public void syncedByDedicatedEndpointQuotaActualValuesShouldBeAggregatedCorrectly() {
        dispenser().service(YP).syncState().actualQuotas()
                .changeActualQuota(DiActualQuotaUpdate.forResource(SEGMENT_STORAGE).addSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1).actual(DiAmount.of(10, DiUnit.TEBIBYTE)).project(INFRA).build())
                .changeActualQuota(DiActualQuotaUpdate.forResource(SEGMENT_STORAGE).addSegments(DC_SEGMENT_2, SEGMENT_SEGMENT_1).actual(DiAmount.of(20, DiUnit.TEBIBYTE)).project(INFRA).build())
                .changeActualQuota(DiActualQuotaUpdate.forResource(SEGMENT_STORAGE).addSegments(DC_SEGMENT_3, SEGMENT_SEGMENT_1).actual(DiAmount.of(30, DiUnit.TEBIBYTE)).project(INFRA).build())
                .changeActualQuota(DiActualQuotaUpdate.forResource(SEGMENT_STORAGE).addSegments(DC_SEGMENT_2, SEGMENT_SEGMENT_2).actual(DiAmount.of(40, DiUnit.TEBIBYTE)).project(INFRA).build())
                .changeActualQuota(DiActualQuotaUpdate.forResource(SEGMENT_STORAGE).addSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_2).actual(DiAmount.of(50, DiUnit.TEBIBYTE)).project(VERTICALI).build())
                .changeActualQuota(DiActualQuotaUpdate.forResource(SEGMENT_STORAGE).addSegments(DC_SEGMENT_2, SEGMENT_SEGMENT_2).actual(DiAmount.of(60, DiUnit.TEBIBYTE)).project(VERTICALI).build())
                .changeActualQuota(DiActualQuotaUpdate.forResource(SEGMENT_STORAGE).addSegments(DC_SEGMENT_3, SEGMENT_SEGMENT_2).actual(DiAmount.of(70, DiUnit.TEBIBYTE)).project(VERTICALI).build())
                .changeActualQuota(DiActualQuotaUpdate.forResource(SEGMENT_STORAGE).addSegments(DC_SEGMENT_3, SEGMENT_SEGMENT_1).actual(DiAmount.of(80, DiUnit.TEBIBYTE)).project(VERTICALI).build())
                .perform();

        updateHierarchy();

        final DiQuotaGetResponse quotas = dispenser().quotas().get().inService(YP).forResource(SEGMENT_STORAGE).perform();

        final DiQuota infraDc1TotalQuota = findQuota(quotas, INFRA, Collections.singleton(DC_SEGMENT_1));
        final DiQuota infraDc2TotalQuota = findQuota(quotas, INFRA, Collections.singleton(DC_SEGMENT_2));
        final DiQuota infraDc3TotalQuota = findQuota(quotas, INFRA, Collections.singleton(DC_SEGMENT_3));
        final DiQuota infraS1TotalQuota = findQuota(quotas, INFRA, Collections.singleton(SEGMENT_SEGMENT_1));
        final DiQuota infraS2TotalQuota = findQuota(quotas, INFRA, Collections.singleton(SEGMENT_SEGMENT_2));
        final DiQuota infraTotalQuota = findQuota(quotas, INFRA, Collections.emptySet());

        assertEquals(10, infraDc1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(60, infraDc2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(30, infraDc3TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(60, infraS1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(40, infraS2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(100, infraTotalQuota.getActual(DiUnit.TEBIBYTE));

        final DiQuota verticaliDc1TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(DC_SEGMENT_1));
        final DiQuota verticaliDc2TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(DC_SEGMENT_2));
        final DiQuota verticaliDc3TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(DC_SEGMENT_3));
        final DiQuota verticaliS1TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(SEGMENT_SEGMENT_1));
        final DiQuota verticaliS2TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(SEGMENT_SEGMENT_2));
        final DiQuota verticaliTotalQuota = findQuota(quotas, VERTICALI, Collections.emptySet());

        assertEquals(50, verticaliDc1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(60, verticaliDc2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(150, verticaliDc3TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(80, verticaliS1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(180, verticaliS2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(260, verticaliTotalQuota.getActual(DiUnit.TEBIBYTE));

        final DiQuota yandexDc1S1Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1));
        final DiQuota yandexDc2S1Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1));
        final DiQuota yandexDc3S1Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_3, SEGMENT_SEGMENT_1));
        final DiQuota yandexDc1S2Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_2));
        final DiQuota yandexDc2S2Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_2));
        final DiQuota yandexDc3S2Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_3, SEGMENT_SEGMENT_2));
        final DiQuota yandexDc1TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(DC_SEGMENT_1));
        final DiQuota yandexDc2TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(DC_SEGMENT_2));
        final DiQuota yandexDc3TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(DC_SEGMENT_3));
        final DiQuota yandexS1TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(SEGMENT_SEGMENT_1));
        final DiQuota yandexS2TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(SEGMENT_SEGMENT_2));
        final DiQuota yandexTotalQuota = findQuota(quotas, YANDEX, Collections.emptySet());

        assertEquals(10, yandexDc1S1Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(20, yandexDc2S1Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(110, yandexDc3S1Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(50, yandexDc1S2Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(100, yandexDc2S2Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(70, yandexDc3S2Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(60, yandexDc1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(120, yandexDc2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(180, yandexDc3TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(140, yandexS1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(220, yandexS2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(360, yandexTotalQuota.getActual(DiUnit.TEBIBYTE));
    }

    @Test
    public void syncedRawQuotaActualValuesShouldBeAggregatedCorrectly() {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        dispenser().service(YP).syncState().rawQuotas()
                .changeRawQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1).withActual(DiAmount.of(10, DiUnit.TEBIBYTE)).forProject(INFRA).build())
                .changeRawQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_2, SEGMENT_SEGMENT_1).withActual(DiAmount.of(20, DiUnit.TEBIBYTE)).forProject(INFRA).build())
                .changeRawQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_3, SEGMENT_SEGMENT_1).withActual(DiAmount.of(30, DiUnit.TEBIBYTE)).forProject(INFRA).build())
                .changeRawQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_2, SEGMENT_SEGMENT_2).withActual(DiAmount.of(40, DiUnit.TEBIBYTE)).forProject(INFRA).build())
                .changeRawQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_2).withActual(DiAmount.of(50, DiUnit.TEBIBYTE)).forProject(VERTICALI).build())
                .changeRawQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_2, SEGMENT_SEGMENT_2).withActual(DiAmount.of(60, DiUnit.TEBIBYTE)).forProject(VERTICALI).build())
                .changeRawQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_3, SEGMENT_SEGMENT_2).withActual(DiAmount.of(70, DiUnit.TEBIBYTE)).forProject(VERTICALI).build())
                .changeRawQuota(DiQuotaState.forResource(SEGMENT_STORAGE).withSegments(DC_SEGMENT_3, SEGMENT_SEGMENT_1).withActual(DiAmount.of(80, DiUnit.TEBIBYTE)).forProject(VERTICALI).build())
                .perform();

        updateHierarchy();

        final DiQuotaGetResponse quotas = dispenser().quotas().get().inService(YP).forResource(SEGMENT_STORAGE).perform();

        final DiQuota infraDc1TotalQuota = findQuota(quotas, INFRA, Collections.singleton(DC_SEGMENT_1));
        final DiQuota infraDc2TotalQuota = findQuota(quotas, INFRA, Collections.singleton(DC_SEGMENT_2));
        final DiQuota infraDc3TotalQuota = findQuota(quotas, INFRA, Collections.singleton(DC_SEGMENT_3));
        final DiQuota infraS1TotalQuota = findQuota(quotas, INFRA, Collections.singleton(SEGMENT_SEGMENT_1));
        final DiQuota infraS2TotalQuota = findQuota(quotas, INFRA, Collections.singleton(SEGMENT_SEGMENT_2));
        final DiQuota infraTotalQuota = findQuota(quotas, INFRA, Collections.emptySet());

        assertEquals(10, infraDc1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(60, infraDc2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(30, infraDc3TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(60, infraS1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(40, infraS2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(100, infraTotalQuota.getActual(DiUnit.TEBIBYTE));

        final DiQuota verticaliDc1TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(DC_SEGMENT_1));
        final DiQuota verticaliDc2TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(DC_SEGMENT_2));
        final DiQuota verticaliDc3TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(DC_SEGMENT_3));
        final DiQuota verticaliS1TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(SEGMENT_SEGMENT_1));
        final DiQuota verticaliS2TotalQuota = findQuota(quotas, VERTICALI, Collections.singleton(SEGMENT_SEGMENT_2));
        final DiQuota verticaliTotalQuota = findQuota(quotas, VERTICALI, Collections.emptySet());

        assertEquals(50, verticaliDc1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(60, verticaliDc2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(150, verticaliDc3TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(80, verticaliS1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(180, verticaliS2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(260, verticaliTotalQuota.getActual(DiUnit.TEBIBYTE));

        final DiQuota yandexDc1S1Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1));
        final DiQuota yandexDc2S1Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1));
        final DiQuota yandexDc3S1Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_3, SEGMENT_SEGMENT_1));
        final DiQuota yandexDc1S2Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_2));
        final DiQuota yandexDc2S2Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_2));
        final DiQuota yandexDc3S2Quota = findQuota(quotas, YANDEX, Sets.newHashSet(DC_SEGMENT_3, SEGMENT_SEGMENT_2));
        final DiQuota yandexDc1TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(DC_SEGMENT_1));
        final DiQuota yandexDc2TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(DC_SEGMENT_2));
        final DiQuota yandexDc3TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(DC_SEGMENT_3));
        final DiQuota yandexS1TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(SEGMENT_SEGMENT_1));
        final DiQuota yandexS2TotalQuota = findQuota(quotas, YANDEX, Collections.singleton(SEGMENT_SEGMENT_2));
        final DiQuota yandexTotalQuota = findQuota(quotas, YANDEX, Collections.emptySet());

        assertEquals(10, yandexDc1S1Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(20, yandexDc2S1Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(110, yandexDc3S1Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(50, yandexDc1S2Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(100, yandexDc2S2Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(70, yandexDc3S2Quota.getActual(DiUnit.TEBIBYTE));
        assertEquals(60, yandexDc1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(120, yandexDc2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(180, yandexDc3TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(140, yandexS1TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(220, yandexS2TotalQuota.getActual(DiUnit.TEBIBYTE));
        assertEquals(360, yandexTotalQuota.getActual(DiUnit.TEBIBYTE));
    }

    private DiQuota findQuota(@NotNull final DiQuotaGetResponse quotas,
                              @NotNull final String projectKey,
                              @NotNull final Set<String> segmentKeys) {
        return quotas.stream()
                .filter(q -> q.getProject().getKey().equals(projectKey) && q.getSegmentKeys().equals(segmentKeys))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No quotas for specified project and segments in response"));
    }

    @Test
    public void quotasCanBeFilteredByService() {
        final DiQuotaGetResponse quotas = dispenser()
                .quotas()
                .get()
                .perform();

        assertFalse(quotas.isEmpty());
        assertTrue(quotas.stream()
                .anyMatch(q -> !NIRVANA.equals(q.getSpecification().getResource().getService().getKey()))
        );

        final DiQuotaGetResponse serviceQuotas = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .perform();

        assertFalse(serviceQuotas.isEmpty());
        assertTrue(serviceQuotas.stream()
                .allMatch(q -> NIRVANA.equals(q.getSpecification().getResource().getService().getKey()))
        );

    }

    @Test
    public void quotaOwnActualValueShouldBeAggregatedCorrectly() {
        dispenser()
                .projects()
                .create(DiProject.withKey("p1")
                        .withName("p1")
                        .withDescription("p1")
                        .withParentProject(YANDEX)
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .projects()
                .create(DiProject.withKey("p2")
                        .withName("p2")
                        .withDescription("p2")
                        .withParentProject("p1")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .projects()
                .create(DiProject.withKey("p3")
                        .withName("p3")
                        .withDescription("p3")
                        .withParentProject("p2")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .service(NIRVANA)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject("p3")
                        .withActual(DiAmount.of(5, DiUnit.COUNT))
                        .build())
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject("p2")
                        .withActual(DiAmount.of(17, DiUnit.COUNT))
                        .build())
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject("p1")
                        .withActual(DiAmount.of(13, DiUnit.COUNT))
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        final Map<String, Long> quotaAmountByProjectKey = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .forResource(YT_CPU)
                .perform()
                .stream()
                .collect(Collectors.toMap(q -> q.getProject().getKey(), q -> q.getActual(DiUnit.COUNT)));

        assertEquals(Long.valueOf(5), quotaAmountByProjectKey.get("p3"));
        assertEquals(Long.valueOf(22), quotaAmountByProjectKey.get("p2"));
        assertEquals(Long.valueOf(35), quotaAmountByProjectKey.get("p1"));
        assertEquals(Long.valueOf(35), quotaAmountByProjectKey.get(YANDEX));

    }

    @Test
    public void quotaOwnActualValueShouldBeAggregatedCorrectlyByDedicatedEndpoint() {
        dispenser()
                .projects()
                .create(DiProject.withKey("p1")
                        .withName("p1")
                        .withDescription("p1")
                        .withParentProject(YANDEX)
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .projects()
                .create(DiProject.withKey("p2")
                        .withName("p2")
                        .withDescription("p2")
                        .withParentProject("p1")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .projects()
                .create(DiProject.withKey("p3")
                        .withName("p3")
                        .withDescription("p3")
                        .withParentProject("p2")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .service(NIRVANA)
                .syncState()
                .actualQuotas()
                .changeActualQuota(DiActualQuotaUpdate.forResource(YT_CPU)
                        .project("p3")
                        .actual(DiAmount.of(5, DiUnit.COUNT))
                        .build())
                .changeActualQuota(DiActualQuotaUpdate.forResource(YT_CPU)
                        .project("p2")
                        .actual(DiAmount.of(17, DiUnit.COUNT))
                        .build())
                .changeActualQuota(DiActualQuotaUpdate.forResource(YT_CPU)
                        .project("p1")
                        .actual(DiAmount.of(13, DiUnit.COUNT))
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        final Map<String, Long> quotaAmountByProjectKey = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .forResource(YT_CPU)
                .perform()
                .stream()
                .collect(Collectors.toMap(q -> q.getProject().getKey(), q -> q.getActual(DiUnit.COUNT)));

        assertEquals(Long.valueOf(5), quotaAmountByProjectKey.get("p3"));
        assertEquals(Long.valueOf(22), quotaAmountByProjectKey.get("p2"));
        assertEquals(Long.valueOf(35), quotaAmountByProjectKey.get("p1"));
        assertEquals(Long.valueOf(35), quotaAmountByProjectKey.get(YANDEX));

    }

    @Test
    public void rawQuotaOwnActualValueShouldBeAggregatedCorrectly() {
        dispenser()
                .projects()
                .create(DiProject.withKey("p1")
                        .withName("p1")
                        .withDescription("p1")
                        .withParentProject(YANDEX)
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .projects()
                .create(DiProject.withKey("p2")
                        .withName("p2")
                        .withDescription("p2")
                        .withParentProject("p1")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .projects()
                .create(DiProject.withKey("p3")
                        .withName("p3")
                        .withDescription("p3")
                        .withParentProject("p2")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        dispenser()
                .service(NIRVANA)
                .syncState()
                .rawQuotas()
                .changeRawQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject("p3")
                        .withActual(DiAmount.of(5, DiUnit.COUNT))
                        .build())
                .changeRawQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject("p2")
                        .withActual(DiAmount.of(17, DiUnit.COUNT))
                        .build())
                .changeRawQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject("p1")
                        .withActual(DiAmount.of(13, DiUnit.COUNT))
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        final Map<String, Long> quotaAmountByProjectKey = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .forResource(YT_CPU)
                .perform()
                .stream()
                .collect(Collectors.toMap(q -> q.getProject().getKey(), q -> q.getActual(DiUnit.COUNT)));

        assertEquals(Long.valueOf(5), quotaAmountByProjectKey.get("p3"));
        assertEquals(Long.valueOf(22), quotaAmountByProjectKey.get("p2"));
        assertEquals(Long.valueOf(35), quotaAmountByProjectKey.get("p1"));
        assertEquals(Long.valueOf(35), quotaAmountByProjectKey.get(YANDEX));

    }

    @Test
    public void projectResponsibleCanUpdateOwnMax() {

        final long rootMax = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .forResource(YT_CPU)
                .ofProject(YANDEX)
                .perform().getFirst().getMax(DiUnit.COUNT);


        dispenser()
                .projects()
                .create(DiProject.withKey("p1")
                        .withName("p1")
                        .withDescription("p1")
                        .withParentProject(YANDEX)
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .withResponsibles(DiPersonGroup.builder()
                                .addPersons(BINARY_CAT.getLogin())
                                .build())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .projects()
                .create(DiProject.withKey("p2")
                        .withName("p2")
                        .withDescription("p2")
                        .withParentProject("p1")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .service(NIRVANA)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject("p1")
                        .withMax(DiAmount.of(116, DiUnit.COUNT))
                        .build())
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject(YANDEX)
                        .withMax(DiAmount.of(rootMax + 116, DiUnit.COUNT))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .service(NIRVANA)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject("p1")
                        .withOwnMax(DiAmount.of(12, DiUnit.COUNT))
                        .build())
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject("p2")
                        .withMax(DiAmount.of(38, DiUnit.COUNT))
                        .build())
                .performBy(BINARY_CAT);

        updateHierarchy();

        final Map<String, DiQuota> quotaByProjectKey = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .forResource(YT_CPU)
                .perform()
                .stream()
                .collect(Collectors.toMap(q -> q.getProject().getKey(), Function.identity()));


        assertEquals(12, DiUnit.COUNT.convert(quotaByProjectKey.get("p1").getOwnMax()));
    }

    @Test
    public void quotaMaxValueCantBeChangedBySyncStateAPIForForbiddenServices() {
        dispenser().properties()
                .setProperty(ServiceService.OLD_STYLE_QUOTA_MAX_CHANGE_ENTITY, ServiceService.FORBIDDEN_SERVICES_PROPERTY, "yp,gencfg")
                .performBy(AMOSOV_F);
        updateHierarchy();

        // changing max values should not work

        assertThrowsWithMessage(() -> {
                    dispenser().service(YP)
                            .syncState()
                            .quotas()
                            .changeQuota(DiQuotaState
                                    .forResource(SEGMENT_STORAGE)
                                    .forProject(DEFAULT)
                                    .forKey(SEGMENT_STORAGE)
                                    .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                                    .withMax(DiAmount.anyOf(DiUnit.BYTE))
                                    .build())
                            .performBy(WHISTLER);
                },
                "Max values of 'YP' quotas can't be changed"
        );
        assertThrowsWithMessage(() -> {
                    dispenser().service(GENCFG)
                            .syncState()
                            .quotas()
                            .changeQuota(DiQuotaState
                                    .forResource(GENCFG_SEGMENT_CPU)
                                    .forProject(DEFAULT)
                                    .forKey(GENCFG_SEGMENT_CPU)
                                    .withSegments(DC_SEGMENT_1, GENCFG_SEGMENT_SEGMENT_1)
                                    .withMax(DiAmount.anyOf(DiUnit.CORES))
                                    .build())
                            .performBy(WHISTLER);
                },
                "Max values of 'GenCfg' quotas can't be changed"
        );

        // changing actual values should not work

        dispenser().service(YP)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState
                        .forResource(SEGMENT_STORAGE)
                        .forProject(DEFAULT)
                        .forKey(SEGMENT_STORAGE)
                        .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                        .withActual(DiAmount.anyOf(DiUnit.BYTE))
                        .build())
                .perform();
        dispenser().service(GENCFG)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState
                        .forResource(GENCFG_SEGMENT_CPU)
                        .forProject(DEFAULT)
                        .forKey(GENCFG_SEGMENT_CPU)
                        .withSegments(DC_SEGMENT_1, GENCFG_SEGMENT_SEGMENT_1)
                        .withActual(DiAmount.anyOf(DiUnit.CORES))
                        .build())
                .perform();
    }

    @Test
    public void quotaValueCantBeChangedByRawSyncStateAPIWithoutProperty() {
        assertThrowsForbiddenWithMessage(() -> {
                    dispenser().service(YP)
                            .syncState()
                            .rawQuotas()
                            .changeRawQuota(DiQuotaState
                                    .forResource(SEGMENT_STORAGE)
                                    .forProject(DEFAULT)
                                    .forKey(SEGMENT_STORAGE)
                                    .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                                    .withMax(DiAmount.anyOf(DiUnit.BYTE))
                                    .build())
                            .performBy(WHISTLER);
                },
                "Raw quota import is forbidden"
        );
        assertThrowsForbiddenWithMessage(() -> {
                    dispenser().service(GENCFG)
                            .syncState()
                            .rawQuotas()
                            .changeRawQuota(DiQuotaState
                                    .forResource(GENCFG_SEGMENT_CPU)
                                    .forProject(DEFAULT)
                                    .forKey(GENCFG_SEGMENT_CPU)
                                    .withSegments(DC_SEGMENT_1, GENCFG_SEGMENT_SEGMENT_1)
                                    .withMax(DiAmount.anyOf(DiUnit.CORES))
                                    .build())
                            .performBy(WHISTLER);
                },
                "Raw quota import is forbidden"
        );
        assertThrowsForbiddenWithMessage(() -> {
                    dispenser().service(YP)
                            .syncState()
                            .rawQuotas()
                            .changeRawQuota(DiQuotaState
                                    .forResource(SEGMENT_STORAGE)
                                    .forProject(DEFAULT)
                                    .forKey(SEGMENT_STORAGE)
                                    .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                                    .withOwnMax(DiAmount.anyOf(DiUnit.BYTE))
                                    .build())
                            .performBy(WHISTLER);
                },
                "Raw quota import is forbidden"
        );
        assertThrowsForbiddenWithMessage(() -> {
                    dispenser().service(GENCFG)
                            .syncState()
                            .rawQuotas()
                            .changeRawQuota(DiQuotaState
                                    .forResource(GENCFG_SEGMENT_CPU)
                                    .forProject(DEFAULT)
                                    .forKey(GENCFG_SEGMENT_CPU)
                                    .withSegments(DC_SEGMENT_1, GENCFG_SEGMENT_SEGMENT_1)
                                    .withOwnMax(DiAmount.anyOf(DiUnit.CORES))
                                    .build())
                            .performBy(WHISTLER);
                },
                "Raw quota import is forbidden"
        );
        assertThrowsForbiddenWithMessage(() -> {
                    dispenser().service(YP)
                            .syncState()
                            .rawQuotas()
                            .changeRawQuota(DiQuotaState
                                    .forResource(SEGMENT_STORAGE)
                                    .forProject(DEFAULT)
                                    .forKey(SEGMENT_STORAGE)
                                    .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                                    .withActual(DiAmount.anyOf(DiUnit.BYTE))
                                    .build())
                            .performBy(WHISTLER);
                },
                "Raw quota import is forbidden"
        );
        assertThrowsForbiddenWithMessage(() -> {
                    dispenser().service(GENCFG)
                            .syncState()
                            .rawQuotas()
                            .changeRawQuota(DiQuotaState
                                    .forResource(GENCFG_SEGMENT_CPU)
                                    .forProject(DEFAULT)
                                    .forKey(GENCFG_SEGMENT_CPU)
                                    .withSegments(DC_SEGMENT_1, GENCFG_SEGMENT_SEGMENT_1)
                                    .withActual(DiAmount.anyOf(DiUnit.CORES))
                                    .build())
                            .performBy(WHISTLER);
                },
                "Raw quota import is forbidden"
        );
    }

    @Test
    public void quotaMaxValueCantBeChangedByQuotaUpdateAPIForForbiddenServices() {
        dispenser().properties()
                .setProperty(ServiceService.OLD_STYLE_QUOTA_MAX_CHANGE_ENTITY, ServiceService.FORBIDDEN_SERVICES_PROPERTY, "yp,gencfg")
                .performBy(AMOSOV_F);
        updateHierarchy();

        final Response ypResponse = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quotas/" + YANDEX + "/" + YP + "/" + SEGMENT_CPU + "/" + SEGMENT_CPU)
                .post(ImmutableMap.of("maxValue", 99, "unit", DiUnit.COUNT, "segments", ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1)));

        assertEquals(HttpStatus.SC_BAD_REQUEST, ypResponse.getStatus());
        assertTrue(ypResponse.readEntity(String.class).contains("Max values of 'YP' quotas can't be changed"));

        final Response gencfgResponse = createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quotas/" + YANDEX + "/" + GENCFG + "/" + GENCFG_SEGMENT_CPU + "/" + GENCFG_SEGMENT_CPU)
                .post(ImmutableMap.of("maxValue", 99, "unit", DiUnit.COUNT, "segments", ImmutableSet.of(DC_SEGMENT_1, GENCFG_SEGMENT_SEGMENT_2)));

        assertEquals(HttpStatus.SC_BAD_REQUEST, gencfgResponse.getStatus());
        assertTrue(gencfgResponse.readEntity(String.class).contains("Max values of 'GenCfg' quotas can't be changed"));
    }

    @Test
    public void totalActualForQuotaWithOneSegmentationAndOwnActualShouldCalculatedCorrectly() {
        final Service mds = Hierarchy.get().getServiceReader().read(MDS);
        final Segmentation dcSegmentation = Hierarchy.get().getSegmentationReader().read(new Segmentation.Key(DC_SEGMENTATION));

        final Resource storage = resourceDao.create(new Resource.Builder(STORAGE, mds)
                .name("Storage")
                .type(DiResourceType.STORAGE)
                .mode(DiQuotingMode.SYNCHRONIZATION)
                .build());

        updateHierarchy();

        resourceSegmentationDao.create(new ResourceSegmentation.Builder(
                storage, dcSegmentation
        ).build());

        updateHierarchy();

        quotaSpecDao.create(new QuotaSpec.Builder(STORAGE, storage)
                .description("MDS Storage quota")
                .build());

        serviceDao.attachTrustee(mds, personDao.readPersonByLogin(AGODIN.getLogin()));

        updateHierarchy();

        dispenser().service(MDS)
                .syncState()
                .actualQuotas()
                .changeActualQuota(DiActualQuotaUpdate.forResource(STORAGE)
                        .project(SEARCH)
                        .actual(DiAmount.of(6, DiUnit.TEBIBYTE))
                        .addSegments(DC_SEGMENT_1)
                        .build())
                .performBy(AGODIN);

        updateHierarchy();

        Map<Optional<String>, Long> actualBySegment = dispenser().quotas()
                .get()
                .inService(MDS)
                .forResource(STORAGE)
                .ofProject(SEARCH)
                .perform()
                .stream()
                .collect(Collectors.toMap(q -> Optional.ofNullable(Iterables.getFirst(q.getSegmentKeys(), null)), q -> q.getActual(DiUnit.TEBIBYTE)));

        assertEquals(6, (long) actualBySegment.get(Optional.of(DC_SEGMENT_1)));
        assertEquals(0, (long) actualBySegment.get(Optional.of(DC_SEGMENT_2)));
        assertEquals(6, (long) actualBySegment.get(Optional.<String>empty()));

        dispenser().service(MDS)
                .syncState()
                .actualQuotas()
                .changeActualQuota(DiActualQuotaUpdate.forResource(STORAGE)
                        .project(SEARCH)
                        .actual(DiAmount.of(3, DiUnit.TEBIBYTE))
                        .addSegments(DC_SEGMENT_2)
                        .build())
                .performBy(AGODIN);

        updateHierarchy();

        actualBySegment = dispenser().quotas()
                .get()
                .inService(MDS)
                .forResource(STORAGE)
                .ofProject(SEARCH)
                .perform()
                .stream()
                .collect(Collectors.toMap(q -> Optional.ofNullable(Iterables.getFirst(q.getSegmentKeys(), null)), q -> q.getActual(DiUnit.TEBIBYTE)));

        assertEquals(6, (long) actualBySegment.get(Optional.of(DC_SEGMENT_1)));
        assertEquals(3, (long) actualBySegment.get(Optional.of(DC_SEGMENT_2)));
        assertEquals(9, (long) actualBySegment.get(Optional.<String>empty()));

        dispenser().service(MDS)
                .syncState()
                .actualQuotas()
                .changeActualQuota(DiActualQuotaUpdate.forResource(STORAGE)
                        .project(INFRA_SPECIAL)
                        .actual(DiAmount.of(9, DiUnit.TEBIBYTE))
                        .addSegments(DC_SEGMENT_1)
                        .build())
                .performBy(AGODIN);

        updateHierarchy();

        actualBySegment = dispenser().quotas()
                .get()
                .inService(MDS)
                .forResource(STORAGE)
                .ofProject(SEARCH)
                .perform()
                .stream()
                .collect(Collectors.toMap(q -> Optional.ofNullable(Iterables.getFirst(q.getSegmentKeys(), null)), q -> q.getActual(DiUnit.TEBIBYTE)));

        assertEquals(15, (long) actualBySegment.get(Optional.of(DC_SEGMENT_1)));
        assertEquals(3, (long) actualBySegment.get(Optional.of(DC_SEGMENT_2)));
        assertEquals(18, (long) actualBySegment.get(Optional.<String>empty()));
    }
}
