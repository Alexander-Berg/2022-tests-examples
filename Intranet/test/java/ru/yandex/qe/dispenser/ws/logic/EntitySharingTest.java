package ru.yandex.qe.dispenser.ws.logic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.HttpHeaders;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiPerformer;
import ru.yandex.qe.dispenser.api.v1.DiQuota;
import ru.yandex.qe.dispenser.api.v1.DiQuotaType;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.request.DiEntityUsage;
import ru.yandex.qe.dispenser.api.v1.request.DiProcessingMode;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.domain.EntitySpec;
import ru.yandex.qe.dispenser.domain.QuotaSpec;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.entity.SqlEntityDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.support.EntitySharingRelease;
import ru.yandex.qe.dispenser.domain.support.EntityUsageDiff;
import ru.yandex.qe.dispenser.domain.util.StreamUtils;
import ru.yandex.qe.dispenser.ws.BatchQuotaServiceImpl;
import ru.yandex.qe.dispenser.ws.param.batch.QuotaChangeBodyReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EntitySharingTest extends BusinessLogicTestBase {

    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;

    private static final String POPUGAI = "popugai";
    private static final String POPUGAI_SPEC = "popugai-spec";

    @Override
    @BeforeEach
    public void setUp() {
        reinitialize();
        dispenser().service(NIRVANA)
                .resource(POPUGAI)
                .create()
                .withName("Popugai")
                .withType(DiResourceType.ENUMERABLE)
                .inMode(DiQuotingMode.ENTITIES_ONLY)
                .performBy(SANCHO);
        final Service nirvana = serviceDao.read(NIRVANA);
        final Resource popugai = resourceDao.read(new Resource.Key(POPUGAI, nirvana));
        final QuotaSpec popugaiInfo = quotaSpecDao.create(new QuotaSpec.Builder("popugai", popugai)
                .type(DiQuotaType.ABSOLUTE)
                .description("Popugai")
                .build());
        final EntitySpec popugaiEntities = EntitySpec.builder()
                .withKey(POPUGAI_SPEC)
                .withDescription("Popugai spec description")
                .overResource(popugai)
                .overResource(resourceDao.read(new Resource.Key(STORAGE, nirvana)))
                .build();
        entitySpecDao.create(popugaiEntities);

        updateHierarchy();
    }

    /**
     * {@link BatchQuotaServiceImpl#changeQuotas}
     */
    @Test
    public void acquireUsageAfterAcquireMustShareQuota() {
        final DiEntity entity = DiEntity.withKey("pool")
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.of(60, DiUnit.BYTE))
                .build();

        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(entity, LYADZHIN.chooses(INFRA))
                .shareEntity(DiEntityUsage.singleOf(entity), LYADZHIN.chooses(VERTICALI))
                .perform();

        updateHierarchy();

        DiQuotaGetResponse leafQuotas = dispenser().quotas().get().inLeafs().perform();
        assertActualEquals(leafQuotas, STORAGE, INFRA, 30L, VERTICALI, 30L);

        dispenser().quotas()
                .changeInService(NIRVANA)
                .shareEntity(DiEntityUsage.singleOf(entity), LYADZHIN.chooses(VERTICALI))
                .perform();

        updateHierarchy();

        leafQuotas = dispenser().quotas().get().inLeafs().perform();
        assertActualEquals(leafQuotas, STORAGE, INFRA, 20L, VERTICALI, 40L);
    }

    /**
     * {@link BatchQuotaServiceImpl#changeQuotas}
     */
    @Test
    public void onlyReleaseMustRemoveEntity() {
        final DiPerformer performer = LYADZHIN.chooses(INFRA);
        final DiEntity pool = DiEntity.withKey("pool").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(60, DiUnit.BYTE)).build();
        final DiEntity pool2 = DiEntity.withKey("pool2").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(70, DiUnit.BYTE)).build();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(pool, performer)
                .createEntity(pool2, performer)
                .perform();

        assertEquals(1, dispenser().getEntities().inService(NIRVANA)
                .bySpecification(YT_FILE)
                .page(1).limit(1).perform().size());

        dispenser().quotas()
                .changeInService(NIRVANA)
                .releaseEntitySharing(DiEntityUsage.singleOf(pool), performer)
                .perform();

        updateHierarchy();

        DiQuotaGetResponse leafQuotas = dispenser().quotas().get().inLeafs().perform();
        assertActualEquals(leafQuotas, STORAGE, INFRA, pool2.getSize(STORAGE).getValue());

        assertEquals(2, dispenser().getEntities().inService(NIRVANA).bySpecification(YT_FILE).perform().size());
        final DiListResponse<DiEntity> trashEntities = dispenser().getEntities()
                .trashOnly()
                .inService(NIRVANA)
                .bySpecification(YT_FILE)
                .perform();
        final DiEntity trashEntity = StreamUtils.requireSingle(trashEntities.stream(), "One trash entity left required!");
        assertEquals(trashEntity.getKey(), pool.getKey());

        dispenser().quotas()
                .changeInService(NIRVANA)
                .releaseEntity(trashEntity)
                .perform();
        leafQuotas = dispenser().quotas().get().inLeafs().perform();
        assertActualEquals(leafQuotas, STORAGE, INFRA, pool2.getSize(STORAGE).getValue());

        assertEquals(1, dispenser().getEntities().inService(NIRVANA).bySpecification(YT_FILE).perform().size());
        assertTrue(dispenser().getEntities().inService(NIRVANA).trashOnly().bySpecification(YT_FILE).perform().isEmpty());
    }

    /**
     * {@link EntityUsageDiff#processOverUsages}
     */
    @Test
    public void releaseUsageForNoPersonalUsageMustDecrementBrothersUsage() {
        Assumptions.assumeFalse(isStubMode(), "Real projects don't have created personal subprojects");

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
                .releaseEntitySharing(DiEntityUsage.singleOf(pool), WHISTLER.chooses(INFRA))
                .perform();

        updateHierarchy();

        final DiQuotaGetResponse allQuotas = dispenser().quotas().get().perform();
        assertActualEquals(allQuotas, STORAGE, YANDEX, 0);

        assertEquals(1, dispenser().getEntities().inService(NIRVANA).bySpecification(YT_FILE).perform().size());
    }

    @Test
    public void emptyFileShouldNotModifyQuotas() {
        final DiPerformer performer = LYADZHIN.chooses(INFRA);
        final DiEntity emptyFile = DiEntity.withKey("empty-file")
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.of(0, DiUnit.BYTE))
                .build();
        final DiEntity somePopugai = DiEntity.withKey("popugai")
                .bySpecification(POPUGAI_SPEC)
                .occupies(POPUGAI, DiAmount.of(20, DiUnit.COUNT))
                .occupies(STORAGE, DiAmount.of(0, DiUnit.BYTE))
                .build();

        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(emptyFile, performer)
                .createEntity(somePopugai, performer)
                .perform();

        final Set<DiQuota> quotas = dispenser().quotas().get().perform().stream().collect(Collectors.toSet());

        dispenser().quotas()
                .changeInService(NIRVANA)
                .releaseEntitySharing(DiEntityUsage.singleOf(emptyFile), performer)
                .perform()
                .stream();

        final Set<DiQuota> notModifiedQuotas = dispenser().quotas().get().perform().stream().collect(Collectors.toSet());

        assertEquals(notModifiedQuotas, quotas);
    }

    @Test
    public void quotaSharingMustBePossibleForManyResourcesAtOnce() {
        final DiEntity file30 = DiEntity.withKey("file30").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(30, DiUnit.BYTE)).build();
        final DiEntity file60 = DiEntity.withKey("file60").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(60, DiUnit.BYTE)).build();
        final DiEntity file90 = DiEntity.withKey("file90").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(90, DiUnit.BYTE)).build();
        final DiEntity popugai100 = DiEntity.withKey("popugai100")
                .bySpecification(POPUGAI_SPEC)
                .occupies(POPUGAI, DiAmount.of(100, DiUnit.COUNT))
                .occupies(STORAGE, DiAmount.of(6, DiUnit.BYTE))
                .build();

        final List<DiEntity> entities = Arrays.asList(file30, file60, popugai100, file90);
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntities(entities, LYADZHIN.chooses(INFRA))
                .perform();

        updateHierarchy();

        DiQuotaGetResponse leafQuotas = dispenser().quotas().get().inLeafs().perform();
        assertActualEquals(leafQuotas, STORAGE, INFRA, 30L + 60 + 90 + 6);
        assertActualEquals(leafQuotas, POPUGAI, INFRA, 100_000L);

        dispenser().quotas()
                .changeInService(NIRVANA)
                .shareEntities(Stream.of(file60, popugai100).map(DiEntityUsage::singleOf).collect(Collectors.toList()), LYADZHIN.chooses(VERTICALI))
                .perform();

        updateHierarchy();

        leafQuotas = dispenser().quotas().get().inLeafs().perform();
        //186 infra -> 120 infra + 66/2 infra + 33 vert
        assertActualEquals(leafQuotas, STORAGE, INFRA, 30L + 90 + (60 + 6) / 2, VERTICALI, (60L + 6) / 2);
        assertActualEquals(leafQuotas, POPUGAI, INFRA, 100_000L / 2, VERTICALI, 100_000L / 2);

        dispenser().quotas()
                .changeInService(NIRVANA)
                .releaseEntitiesSharing(Stream.of(popugai100, file60, file90).map(DiEntityUsage::singleOf).collect(Collectors.toList()), LYADZHIN.chooses(INFRA))
                .perform();

        updateHierarchy();
    /*
      186 total
      153 infra + 33 vert
     assertActualEquals(leafQuotas, STORAGE, INFRA, 30L + 60 + 90 + 6);     i-3 v+3 (popugai) | i-30 v+30 (file60) | i-90 (file90)
      i: 153 - 3 - 30 -> 120
     */
        final DiQuotaGetResponse allQuotas = dispenser().quotas().get().perform();
        assertActualEquals(allQuotas, STORAGE, ImmutableMap.of(INFRA, 30L, VERTICALI, 60L + 6, YANDEX, 30L + 60 + 6));
        assertActualEquals(allQuotas, VERTICALI, ImmutableMap.of(INFRA, 0L, VERTICALI, 100L, YANDEX, 100L));
    }

    @Test
    public void releaseEntityWithUsagesInManyProjectsMustBePossible() {
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(UNIT_ENTITY, LYADZHIN.chooses(INFRA))
                .shareEntity(SINGLE_USAGE_UNIT_ENTITY, LYADZHIN.chooses(INFRA))
                .shareEntity(SINGLE_USAGE_UNIT_ENTITY, LYADZHIN.chooses(DEFAULT))
                .perform();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .releaseEntity(UNIT_ENTITY)
                .perform();
        dispenser().quotas().get().perform().forEach(q -> assertTrue(q.getActual().getValue() == 0));
        assertTrue(dispenser().getEntities()
                .inService(NIRVANA)
                .bySpecification(YT_FILE)
                .perform().isEmpty());
    }

    @Test
    public void createEntityMustBePossibleForSubsetOfEntitySpecificationResources() {
        final DiEntity popugai = DiEntity.withKey("popugai")
                .bySpecification(POPUGAI_SPEC)
                .occupies(POPUGAI, DiAmount.of(100, DiUnit.COUNT))
                .build();

        dispenser().quotas().changeInService(NIRVANA).createEntity(popugai, LYADZHIN.chooses(INFRA)).perform();
    }

    @Test
    public void setMaxAndCanAcquireMustConvertUnits() {
        createLocalClient()
                .path("/v1/quotas/" + YANDEX + "/" + NIRVANA + "/" + POPUGAI + "/popugai")
                .header("Content-Type", "application/json")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + AMOSOV_F)
                .post(ImmutableMap.of("maxValue", 100, "unit", DiUnit.COUNT));
        createLocalClient()
                .path("/v1/quotas/" + INFRA + "/" + NIRVANA + "/" + POPUGAI + "/popugai")
                .header("Content-Type", "application/json")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + WHISTLER)
                .post(ImmutableMap.of("maxValue", 100_000, "unit", DiUnit.PERMILLE));

        final DiEntity pool = DiEntity.withKey("pool")
                .bySpecification(POPUGAI_SPEC)
                .occupies(POPUGAI, DiAmount.of(70_00, DiUnit.PERCENT))
                .build();
        dispenser().quotas().changeInService(NIRVANA).createEntity(pool, LYADZHIN.chooses(INFRA)).perform();

        updateHierarchy();

        final DiQuotaGetResponse response = dispenser().quotas().get()
                .inService(NIRVANA)
                .byEntitySpecification(POPUGAI_SPEC)
                .ofProject(INFRA)
                .perform();
        assertTrue(response.canAcquire(POPUGAI, DiAmount.of(30, DiUnit.COUNT)));
        assertFalse(response.canAcquire(POPUGAI, DiAmount.of(30_001, DiUnit.PERMILLE)));
    }

    /**
     * {@link EntitySharingRelease#processOverUsages}
     */
    @Test
    public void releaseAllSharingsMustRemoveEntityUsagesInProject() {
        final DiEntity e = DiEntity.withKey("pool")
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.of(2, DiUnit.BYTE))
                .build();
        dispenser().quotas().changeInService(NIRVANA)
                .createEntity(e, LYADZHIN.chooses(INFRA))
                .shareEntity(DiEntityUsage.singleOf(e), LYADZHIN.chooses(VERTICALI))
                .perform();
        dispenser().quotas().changeInService(NIRVANA).releaseAllEntitySharings(e, LYADZHIN.chooses(INFRA)).perform();

        updateHierarchy();

        assertActualEquals(dispenser().quotas().get().inLeafs().perform(), STORAGE, VERTICALI, 2);
        assertTrue(dispenser().getEntities()
                .trashOnly()
                .inService(NIRVANA)
                .bySpecification(YT_FILE)
                .perform().isEmpty());
    }

    /**
     * DISPENSER-589: Приравнять отсутствие ресурса у entity к 0
     * <p>
     * {@link SqlEntityDao#getEntityMapper}
     **/
    @Test
    public void presentEntitiesMustHaveZeroSizeInNewResource() {
        dispenser().quotas().changeInService(NIRVANA).createEntity(UNIT_ENTITY, LYADZHIN.chooses(INFRA)).perform();

        final String ytChunk = "yt-chunk";
        final Service nirvana = serviceDao.read(NIRVANA);
        final Resource resource = resourceDao.create(new Resource.Builder(ytChunk, nirvana)
                .name("Чанки в YT")
                .type(DiResourceType.ENUMERABLE)
                .mode(DiQuotingMode.ENTITIES_ONLY)
                .build());

        entitySpecDao.addRelations(entitySpecDao.read(new EntitySpec.Key(YT_FILE, nirvana)), Collections.singleton(resource));

        updateHierarchy();

        final DiEntity e = dispenser().getEntities()
                .inService(NIRVANA)
                .bySpecification(YT_FILE)
                .perform().getFirst();
        assertEquals(e.getSize(ytChunk), DiAmount.of(0, DiUnit.PERMILLE));
    }

    /**
     * DISPENSER-687: Режим ингорирования неизвестных entities при удалении использований
     * <p>
     * {@link EntityUsageDiff#processOverUsages}
     */
    @Test
    public void releaseTooMuchUsagesMustZeroiseUsagesInSpecificMode() {
        dispenser().quotas().changeInService(NIRVANA, DiProcessingMode.IGNORE_UNKNOWN_ENTITIES_AND_USAGES)
                .createEntity(UNIT_ENTITY, LYADZHIN.chooses(INFRA))
                .releaseEntitySharing(DiEntityUsage.of(UNIT_ENTITY, 2), LYADZHIN.chooses(INFRA))
                .perform();

        assertTrue(dispenser().getEntityOwnerships().perform().isEmpty());
        assertEquals(1, dispenser().getEntities()
                .inService(NIRVANA)
                .bySpecification(YT_FILE)
                .trashOnly()
                .perform().size());
    }

    /**
     * DISPENSER-687: Режим ингорирования неизвестных entities при удалении использований
     * <p>
     * {@link QuotaChangeBodyReader#readEntities}
     */
    @Test
    public void releaseUsagesMustIgnoreUnknownEntitiesInSpecificMode() {
        dispenser().quotas().changeInService(NIRVANA, DiProcessingMode.IGNORE_UNKNOWN_ENTITIES_AND_USAGES)
                .releaseEntitySharing(DiEntityUsage.singleOf(UNIT_ENTITY), LYADZHIN.chooses(INFRA))
                .perform();

        assertTrue(dispenser().getEntityOwnerships().perform().isEmpty());
        assertTrue(dispenser().getEntities()
                .inService(NIRVANA)
                .bySpecification(YT_FILE)
                .trashOnly()
                .perform().isEmpty());
    }

    /**
     * DISPENSER-782: Ошибка при удалении entity в режиме IGNORE_UNKNOWN_ENTITIES_AND_USAGES
     * <p>
     * {@link ru.yandex.qe.dispenser.ws.param.batch.ConversionUtils#convert}
     */
    @Test
    public void noErrorOnReleaseUnknownEntityInSpecificMode() {
        dispenser().quotas()
                .changeInService(NIRVANA, DiProcessingMode.IGNORE_UNKNOWN_ENTITIES_AND_USAGES)
                .releaseEntity(UNIT_ENTITY)
                .perform();
    }
}
