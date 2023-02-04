package ru.yandex.qe.dispenser.ws.logic;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiSegmentation;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.request.DiResourceAmount;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.client.v1.builder.GetEntitiesRequestBuilder;
import ru.yandex.qe.dispenser.client.v1.impl.PageFilter;
import ru.yandex.qe.dispenser.client.v1.impl.RequestFilter;
import ru.yandex.qe.dispenser.client.v1.impl.TimeFilter;
import ru.yandex.qe.dispenser.domain.EntitySpec;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.utils.DiEntityIdSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class EntityServiceBusinessLogicTest extends BusinessLogicTestBase {

    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;

    @Test
    public void getEntitiesRequestForNoCreatedEntitiesShouldReturnEmptyList() {
        final DiListResponse<DiEntity> entities = dispenser().getEntities()
                .inService(NIRVANA)
                .bySpecification(YT_FILE)
                .perform();
        assertTrue(entities.isEmpty());
    }

    @Test
    public void createdEntityShouldBeReturned() {
        final String entityKey = "entity";
        final DiEntity entity = DiEntity.withKey(entityKey)
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.of(100, DiUnit.BYTE))
                .build();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(entity, LYADZHIN.chooses(INFRA))
                .perform();

        // all created entities
        final DiListResponse<DiEntity> entities = dispenser().getEntities().inService(NIRVANA).bySpecification(YT_FILE).perform();
        assertEquals(1, entities.size());
        assertEquals(entities.getFirst(), entity);

        // concrete entity
        final DiEntity returnedEntity = dispenser().getEntities()
                .inService(NIRVANA)
                .bySpecification(YT_FILE)
                .withKey(entityKey)
                .perform();
        assertEquals(returnedEntity, entity);
    }

    @Test
    public void gettingEntitiesWithPaginationShouldReturnAllCreatedEntities() {
        // create entities
        final int entitiesCount = 100;
        final Set<DiEntity> entitiesToCreate = generateNirvanaYtFiles(entitiesCount)
                .collect(Collectors.toSet());
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntities(entitiesToCreate, LYADZHIN.chooses(INFRA))
                .perform();

        // test pagination
        final Set<DiEntity> returnedEntities = new HashSet<>();
        final int pageSize = 10;
        for (int i = 0; i < entitiesCount / pageSize; i++) {
            dispenser().getEntities()
                    .inService(NIRVANA)
                    .bySpecification(YT_FILE)
                    .page(i)
                    .limit(pageSize)
                    .perform()
                    .forEach(returnedEntities::add);
        }
        assertEquals(returnedEntities, entitiesToCreate);
    }

    @Test
    public void getEntitiesRequestForEntitiesCreatedFromCertainTimeShouldReturnCorrectEntities() {
        final Triple<Long, Set<DiEntity>, Long> creationResult = createEntitiesDuringTimeInterval(5, 10, 0);

        final DiListResponse<DiEntity> response = dispenser().getEntities()
                .createdFrom(creationResult.getLeft())
                .inService(NIRVANA)
                .bySpecification(YT_FILE)
                .perform();
        final Set<DiEntity> returnedEntities = response.stream().collect(Collectors.toSet());
        assertEquals(returnedEntities, creationResult.getMiddle());
    }


    @Test
    public void getEntitiesRequestForEntitiesCreatedToCertainTimeShouldReturnCorrectEntities() {
        final Triple<Long, Set<DiEntity>, Long> creationResult = createEntitiesDuringTimeInterval(0, 10, 5);

        final DiListResponse<DiEntity> response = dispenser().getEntities()
                .createdTo(creationResult.getRight())
                .inService(NIRVANA)
                .bySpecification(YT_FILE)
                .perform();
        final Set<DiEntity> returnedEntities = response.stream().collect(Collectors.toSet());
        assertEquals(returnedEntities, creationResult.getMiddle());
    }

    @Test
    public void getEntitiesRequestForEntitiesCreatedDuringTimeIntervalShouldReturnCorrectEntities() {
        final Triple<Long, Set<DiEntity>, Long> creationResult = createEntitiesDuringTimeInterval(5, 10, 5);

        final DiListResponse<DiEntity> response = dispenser().getEntities()
                .createdFrom(creationResult.getLeft())
                .createdTo(creationResult.getRight())
                .inService(NIRVANA)
                .bySpecification(YT_FILE)
                .perform();

        final Set<DiEntity> returnedEntities = response.stream().collect(Collectors.toSet());
        assertEquals(returnedEntities, creationResult.getMiddle());
    }

    @Test
    public void getEntitiesRequestForEntitiesCreatedDuringTimeIntervalWithPaginationShouldReturnCorrectEntities() {
        final Triple<Long, Set<DiEntity>, Long> creationResult = createEntitiesDuringTimeInterval(5, 25, 5);

        final TimeFilter intervalFilter = TimeFilter.interval(creationResult.getLeft(), creationResult.getRight());
        final Set<DiEntity> returnedEntities = getEntitiesWithPagination(10, Collections.singletonList(intervalFilter));

        assertEquals(returnedEntities, creationResult.getMiddle());
    }

    @Test
    public void entityWithNegativeDimensionCannotBeCreated() {
        assertThrows(BadRequestException.class, () -> {
            final DiEntity entity = DiEntity.withKey("yt-file")
                    .bySpecification(YT_FILE)
                    .occupies(STORAGE, DiAmountWithNegativeValueAllowed.of(-100, DiUnit.GIBIBYTE))
                    .build();
            dispenser().quotas()
                    .changeInService(NIRVANA)
                    .createEntity(entity, LYADZHIN.chooses(INFRA))
                    .perform();
        });
    }

    // Utility methods

    @NotNull
    private Triple<Long, Set<DiEntity>, Long> createEntitiesDuringTimeInterval(final int beforeIntervalCount,
                                                                               final int duringIntervalCount,
                                                                               final int afterIntervalCount) {
        final DiEntityIdSupplier idSupplier = new DiEntityIdSupplier();

        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntities(generateNirvanaYtFiles(beforeIntervalCount, idSupplier).collect(Collectors.toSet()), LYADZHIN.chooses(INFRA))
                .perform();

        sleep(1, TimeUnit.SECONDS);
        final long intervalStart = System.currentTimeMillis();
        sleep(1, TimeUnit.SECONDS);

        final Set<DiEntity> entitiesOfInterval = generateNirvanaYtFiles(duringIntervalCount, idSupplier).collect(Collectors.toSet());
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntities(entitiesOfInterval, LYADZHIN.chooses(INFRA))
                .perform();

        sleep(1, TimeUnit.SECONDS);
        final long intervalEnd = System.currentTimeMillis();
        sleep(1, TimeUnit.SECONDS);

        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntities(generateNirvanaYtFiles(afterIntervalCount, idSupplier).collect(Collectors.toSet()), LYADZHIN.chooses(INFRA))
                .perform();

        return Triple.of(intervalStart, entitiesOfInterval, intervalEnd);
    }

    @NotNull
    private Set<DiEntity> getEntitiesWithPagination(final int pageSize, @NotNull final Collection<RequestFilter> filters) {
        final Set<DiEntity> entities = new HashSet<>();
        int pageNumber = 0;
        while (true) {
            final GetEntitiesRequestBuilder<?> request = dispenser().getEntities()
                    .inService(NIRVANA)
                    .bySpecification(YT_FILE)
                    .filterBy(PageFilter.page(pageNumber++, pageSize));
            filters.forEach(request::filterBy);
            final DiListResponse<DiEntity> response = request.perform();
            if (response.isEmpty()) {
                break;
            }
            response.forEach(entities::add);
        }
        return entities;
    }

    @NotNull
    private Set<DiEntity> getEntitiesWithPagination(final int pageSize) {
        return getEntitiesWithPagination(pageSize, Collections.emptyList());
    }

    @Test
    public void entitiesWithSegmentedResourceCanBeFetched() {

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-specifications/" + CLUSTER_API + "/" + CPU + "/" + CPU)
                .delete();

        updateHierarchy();

        dispenser()
                .service(CLUSTER_API)
                .resource(CPU)
                .segmentations()
                .update(Collections.singletonList(
                        new DiSegmentation.Builder(DC_SEGMENTATION).build()
                ))
                .performBy(AMOSOV_F);

        updateHierarchy();

        final Service clusterApi = serviceDao.read(CLUSTER_API);
        final Resource cpu = resourceDao.read(new Resource.Key(CPU, clusterApi));

        final EntitySpec file = EntitySpec.builder()
                .withKey("cluster-file")
                .withDescription("Файлы в YP")
                .overResource(cpu)
                .build();

        entitySpecDao.create(file);

        updateHierarchy();

        final DiEntity entity = DiEntity.withKey("entity")
                .bySpecification("cluster-file")
                .occupies(DiResourceAmount.ofResource(CPU)
                        .withSegments(DC_SEGMENT_1)
                        .withAmount(DiAmount.of(100, DiUnit.COUNT))
                        .build())
                .build();

        dispenser().quotas()
                .changeInService(CLUSTER_API)
                .createEntity(entity, LYADZHIN.chooses(INFRA))
                .perform();

        final DiListResponse<DiEntity> entities = dispenser().getEntities()
                .inService(CLUSTER_API)
                .bySpecification("cluster-file")
                .perform();
        assertEquals(1, entities.size());


        final DiEntity firstEntity = entities.getFirst();


        final Set<String> segments = Collections.singleton(DC_SEGMENT_1);

        assertEquals(DiUnit.PERMILLE.convert(firstEntity.getSize(CPU, segments)), DiUnit.PERMILLE.convert(entity.getSize(CPU, segments)));
    }
}
