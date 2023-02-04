package ru.yandex.qe.dispenser.ws.logic;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiOrder;
import ru.yandex.qe.dispenser.api.v1.DiPerformer;
import ru.yandex.qe.dispenser.api.v1.DiProject;
import ru.yandex.qe.dispenser.api.v1.DiQuota;
import ru.yandex.qe.dispenser.api.v1.DiQuotaLightView;
import ru.yandex.qe.dispenser.api.v1.DiQuotaSpec;
import ru.yandex.qe.dispenser.api.v1.DiResource;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiSegment;
import ru.yandex.qe.dispenser.api.v1.DiSegmentation;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiActualQuotaUpdate;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.client.v1.impl.ResourceFilter;
import ru.yandex.qe.dispenser.client.v1.impl.SpyWebClient;
import ru.yandex.qe.dispenser.domain.util.StreamUtils;
import ru.yandex.qe.dispenser.ws.ServiceService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class QuotaReadUpdateTest extends BusinessLogicTestBase {

    @Test
    public void actualOfNotLeafMustBeSumOfLeafActuals() {
        final DiEntity pool1 = DiEntity.withKey("pool1").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(100, DiUnit.BYTE)).build();
        final DiEntity pool2 = DiEntity.withKey("pool2").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(25, DiUnit.BYTE)).build();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(pool1, LYADZHIN.chooses(INFRA))
                .createEntity(pool2, LYADZHIN.chooses(VERTICALI))
                .perform();

        updateHierarchy();

        final long yandexActual = dispenser().quotas().get()
                .inService(NIRVANA)
                .forResource(STORAGE)
                .ofProject(YANDEX)
                .perform()
                .getFirst()
                .getActual()
                .getValue();
        assertEquals(125, yandexActual);
    }

    @Test
    public void canFilterQuotasByManyResourcesFromQueryParams() {
        final DiQuotaGetResponse resp = dispenser().quotas().get()
                .filterBy(ResourceFilter.resource("nirvana", "storage").or("cluster-api", "computer").build())
                .perform();

        final long projectsCount = dispenser().projects().get().perform().size();

        final long computerQuotasCount = StreamUtils.stream(resp)
                .filter(q -> q.getSpecification().getResource().getKey().equals(COMPUTER))
                .count();
        final long storageQuotasCount = StreamUtils.stream(resp)
                .filter(q -> q.getSpecification().getResource().getKey().equals(STORAGE))
                .count();
        assertEquals(computerQuotasCount, projectsCount);
        assertEquals(storageQuotasCount, projectsCount);
    }

    @Test
    public void canFilterQuotasByManyResourcesWithSequentialMethodsCall() {
        final DiQuotaGetResponse resp = dispenser().quotas().get()
                .inService("nirvana").forResource("storage")
                .inService("cluster-api").forResource("computer")
                .perform();

        final long projectsCount = dispenser().projects().get().perform().size();

        final long computerQuotasCount = StreamUtils.stream(resp)
                .filter(q -> q.getSpecification().getResource().getKey().equals(COMPUTER))
                .count();
        final long storageQuotasCount = StreamUtils.stream(resp)
                .filter(q -> q.getSpecification().getResource().getKey().equals(STORAGE))
                .count();
        assertEquals(computerQuotasCount, projectsCount);
        assertEquals(storageQuotasCount, projectsCount);
    }

    @Test
    public void canFilterQuotasByMemberProjectsAfterPersonalProjectsCreating() {
        dispenser().quotas().changeInService(NIRVANA).createEntity(UNIT_ENTITY, DiPerformer.login("bendyna").chooses(DEFAULT)).perform();

        final DiQuotaGetResponse bendynaAvailableQuotas = dispenser().quotas().get()
                .inService(NIRVANA)
                .byEntitySpecification(YT_FILE)
                .availableFor("bendyna")
                .perform();
        assertEquals(1, bendynaAvailableQuotas.size());
        assertEquals(DEFAULT, bendynaAvailableQuotas.getFirst().getProject().getKey());
    }

    /**
     * DISPENSER-610: Транзакционное обновление квот проектов
     * <p>
     * {@link ServiceService#syncQuotas}
     */
    @Test
    public void canReshareQuotaBetweenSubprojects() {
        final DiQuotaGetResponse modifiedQuotas = dispenser().service(NIRVANA).syncState().quotas()
                .changeQuota(DiQuotaState.forResource(STORAGE).forProject(DEFAULT).withMax(DiAmount.of(200, DiUnit.BYTE)).build())
                .changeQuota(DiQuotaState.forResource(STORAGE).forProject(INFRA).withMax(DiAmount.of(40, DiUnit.BYTE)).build())
                .changeQuota(DiQuotaState.forResource(STORAGE).forProject(VERTICALI).withMax(DiAmount.of(10, DiUnit.BYTE)).build())
                .performBy(WHISTLER);
        final Map<String, Long> project2max = modifiedQuotas.stream()
                .collect(Collectors.toMap(q -> q.getProject().getKey(), q -> q.getMax().getValue()));
        assertEquals(200, (long) project2max.get(DEFAULT));
        assertEquals(40, (long) project2max.get(INFRA));
        assertEquals(10, (long) project2max.get(VERTICALI));
    }

    @Test
    public void canSortQuotasV1ByAsc() {
        final DiEntity pool1 = DiEntity.withKey("pool1").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(100, DiUnit.BYTE)).build();
        final DiEntity pool2 = DiEntity.withKey("pool2").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(25, DiUnit.BYTE)).build();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(pool1, LYADZHIN.chooses(INFRA))
                .createEntity(pool2, LYADZHIN.chooses(VERTICALI))
                .perform();

        updateHierarchy();

        final DiQuotaGetResponse response = createLocalClient()
                .path("/v1/quotas")
                .query("resource", "/" + NIRVANA + "/" + STORAGE)
                .query("order", DiOrder.ASC)
                .get(DiQuotaGetResponse.class);

        final List<Long> res = response
                .stream()
                .map(DiQuota::getActual)
                .map(DiAmount::getValue)
                .collect(Collectors.toList());

        assertTrue(Ordering.natural().isOrdered(res));
        final long first = res.get(0);
        final long last = res.get(res.size() - 1);

        assertTrue(first <= last);
    }


    @Test
    public void canSortQuotasV1ByDesc() {
        final DiEntity pool1 = DiEntity.withKey("pool1").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(100, DiUnit.BYTE)).build();
        final DiEntity pool2 = DiEntity.withKey("pool2").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(25, DiUnit.BYTE)).build();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(pool1, LYADZHIN.chooses(INFRA))
                .createEntity(pool2, LYADZHIN.chooses(VERTICALI))
                .perform();

        updateHierarchy();

        final DiQuotaGetResponse response = createLocalClient()
                .path("/v1/quotas")
                .query("resource", "/" + NIRVANA + "/" + STORAGE)
                .query("order", DiOrder.DESC)
                .get(DiQuotaGetResponse.class);

        final List<Long> res = response
                .stream()
                .map(DiQuota::getActual)
                .map(DiAmount::getValue)
                .collect(Collectors.toList());

        assertTrue(Ordering.natural().reverse().isOrdered(res));
        final long first = res.get(0);
        final long last = res.get(res.size() - 1);

        assertTrue(first >= last);
    }

    @Test
    public void quotaWithSegmentsCanBeUpdated() {
        final HashSet<String> segments = Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1);
        final DiUnit unit = DiUnit.PERMILLE_CORES;

        final long currentMax = getYandexSegmentCpuQuota(unit, segments);
        assertEquals(30000, currentMax);

        updateYandexSegmentCpuQuota(29000, unit, segments);

        final long newMax = getYandexSegmentCpuQuota(unit, segments);
        assertEquals(29000, newMax);
    }

    @Test
    public void quotaWithAggregationSegmentCanBeUpdated() {
        final Set<String> segments = Collections.singleton(DC_SEGMENT_1);
        final DiUnit unit = DiUnit.CORES;

        final long currentMax = getYandexSegmentCpuQuota(unit, segments);
        assertEquals(70, currentMax);

        updateYandexSegmentCpuQuota(50, unit, segments);

        final long updatedMax = getYandexSegmentCpuQuota(unit, segments);
        assertEquals(50, updatedMax);
    }

    @Test
    public void quotaWithAggregationSegmentsOnlyShouldContainsSumRegardlessOwnValue() {
        final Set<String> segments = Collections.emptySet();
        final DiUnit unit = DiUnit.CORES;

        final long currentMax = getYandexSegmentCpuQuota(unit, segments);
        assertEquals(100, currentMax);

        updateYandexSegmentCpuQuota(150, unit, segments);

        final long updatedMax = getYandexSegmentCpuQuota(unit, segments);
        assertEquals(100, updatedMax);
    }


    private long getYandexSegmentCpuQuota(@NotNull final DiUnit unit, @NotNull final Set<String> segments) {
        return dispenser().quotas().get()
                .inService(YP)
                .forResource(SEGMENT_CPU)
                .ofProject(YANDEX)
                .perform()
                .stream()
                .filter(q -> q.getSegmentKeys().equals(segments))
                .findFirst()
                .get()
                .getMax(unit);
    }

    private void updateYandexSegmentCpuQuota(final long maxValue, @NotNull final DiUnit unit, @NotNull final Set<String> segments) {
        createAuthorizedLocalClient(SLONNN)
                .path("/v1/quotas/yandex/yp/segment-cpu/segment-cpu")
                .post(ImmutableMap.of(
                        "maxValue", maxValue,
                        "unit", unit,
                        "segments", segments
                ), DiQuota.class);
        updateHierarchy();
    }

    @Test
    public void canFilterQuotasBySegments() {
        testQuotaFilteringBySegments();
        testQuotaFilteringBySegments(DC_SEGMENT_1);
        testQuotaFilteringBySegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1);
    }

    private void testQuotaFilteringBySegments(final String... segmentKeys) {
        final DiQuotaGetResponse filteredQuotas = dispenser().quotas().get()
                .inService(YP)
                .forResource(SEGMENT_CPU)
                .withSegments(segmentKeys)
                .perform();
        final Set<DiQuota> filteredQuotaSet = filteredQuotas.stream().collect(Collectors.toSet());
        final Set<DiQuota> expectedQuotaSet = dispenser().quotas().get()
                .inService(YP)
                .forResource(SEGMENT_CPU)
                .perform()
                .stream()
                .filter(q -> q.getSegmentKeys().containsAll(Sets.newHashSet(segmentKeys)))
                .collect(Collectors.toSet());
        assertEquals(filteredQuotas.size(), filteredQuotaSet.size());
        assertEquals(filteredQuotaSet, expectedQuotaSet);
    }

    @Test
    public void filteringByIncompatibleSegmentsShouldReturnEmptyResult() {
        final DiQuotaGetResponse quotas = dispenser().quotas().get()
                .withSegments(DC_SEGMENT_1, DC_SEGMENT_2)
                .perform();
        assertTrue(quotas.isEmpty());
    }

    @Test
    public void filteringByNotExistingSegmentsShouldThrowCorrespondingException() {
        testQuotaFilteringBySegmentsThrowsException("");
        testQuotaFilteringBySegmentsThrowsException("not-existing-segment-key");
        testQuotaFilteringBySegmentsThrowsException(DC_SEGMENT_1, "");
        testQuotaFilteringBySegmentsThrowsException(DC_SEGMENT_1, "not-existing-segment-key");
    }

    private void testQuotaFilteringBySegmentsThrowsException(final String... segmentKeys) {
        final BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            dispenser().quotas().get()
                    .inService(YP)
                    .forResource(SEGMENT_CPU)
                    .withSegments(segmentKeys)
                    .perform();
        });
        assertTrue(exception.getMessage().contains("No segment"));
    }

    @Test
    public void cantFilterQuotasBySegmentsInMoreThan2Segmentation() {

        final DiSegmentation complextag = dispenser().segmentations()
                .create(new DiSegmentation.Builder("complextag")
                        .withDescription("complextag")
                        .withName("complextag")
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiSegment complextag1 = dispenser().segmentations()
                .byKey("complextag")
                .segments()
                .create(new DiSegment.Builder("tag1")
                        .withName("tag1")
                        .withDescription("tag1")
                        .inSegmentation(complextag)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiResource ubercpu = dispenser().service(NIRVANA)
                .resource("UBERCPU")
                .create()
                .withName("UBERCPU")
                .withDescription("UBERCPU")
                .withType(DiResourceType.ENUMERABLE)
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser().service(NIRVANA)
                .resource(ubercpu.getKey())
                .segmentations()
                .update(ImmutableList.of(
                        complextag,
                        new DiSegmentation.Builder(DC_SEGMENTATION).withDescription("dc").withName("dc").build(),
                        new DiSegmentation.Builder(SEGMENT_SEGMENTATION).withDescription("s").withName("s").build()
                ))
                .performBy(AMOSOV_F);

        updateHierarchy();

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-specifications/" + NIRVANA + "/" + ubercpu.getKey() + "/" + ubercpu.getKey() + "-quota")
                .put("{\"description\":\"description\"}", DiQuotaSpec.class);

        updateHierarchy();

        assertThrowsWithMessage(() -> {
            final DiQuotaGetResponse filteredQuotas = dispenser().quotas().get()
                    .inService(NIRVANA)
                    .forResource(ubercpu.getKey())
                    .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1, complextag1.getKey())
                    .perform();
        }, "Can't filter by more than 2 segmentation");
    }

    @Test
    public void aggregationSegmentMustContainsTotalMaxForSubsegments() {
        final DiQuotaGetResponse quotas = dispenser().quotas().get()
                .inService(YP)
                .forResource(SEGMENT_CPU)
                .ofProject(YANDEX)
                .perform();

        assertTotalQuotaValid(quotas);

        dispenser()
                .service(YP)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState
                        .forResource(SEGMENT_CPU)
                        .forProject(YANDEX)
                        .withMax(DiAmount.of(100000, DiUnit.PERMILLE_CORES))
                        .withSegments(DC_SEGMENT_2, SEGMENT_SEGMENT_1)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaGetResponse quotas2 = dispenser().quotas().get()
                .inService(YP)
                .forResource(SEGMENT_CPU)
                .ofProject(YANDEX)
                .perform();

        assertTotalQuotaValid(quotas2);
    }

    private void assertTotalQuotaValid(final DiQuotaGetResponse quotas) {
        final long realSum = quotas.stream()
                .filter(q -> q.getSegmentKeys().size() == 2)
                .mapToLong(q -> q.getMax(DiUnit.PERMILLE_CORES))
                .sum();

        final DiQuota totalQuota = quotas.stream()
                .filter(q -> q.getSegmentKeys().isEmpty())
                .findFirst()
                .get();

        assertEquals(totalQuota.getMax(DiUnit.PERMILLE_CORES), realSum);
    }

    @Test
    private void updatingQuotaWithSegmentsForResourceWithoutSegmentationShouldFail() {
        assertThrows(Throwable.class, () -> {
            dispenser()
                    .service(NIRVANA)
                    .syncState()
                    .quotas()
                    .changeQuota(DiQuotaState
                            .forResource(YT_GPU)
                            .forProject(YANDEX)
                            .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                            .withMax(DiAmount.of(1000, DiUnit.COUNT))
                            .build())
                    .performBy(AMOSOV_F);
        });

        assertEquals(HttpStatus.SC_BAD_REQUEST, SpyWebClient.lastResponseStatus());
        assertTrue(SpyWebClient.lastResponse().contains("No segmentation available for resource"));
    }

    @Test
    private void updatingRawQuotaWithSegmentsForResourceWithoutSegmentationShouldFail() {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        assertThrows(Throwable.class, () -> {
            dispenser()
                    .service(NIRVANA)
                    .syncState()
                    .rawQuotas()
                    .changeRawQuota(DiQuotaState
                            .forResource(YT_GPU)
                            .forProject(YANDEX)
                            .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                            .withMax(DiAmount.of(1000, DiUnit.COUNT))
                            .build())
                    .performBy(DiPerson.login(ZOMB_MOBSEARCH));
        });

        assertEquals(HttpStatus.SC_BAD_REQUEST, SpyWebClient.lastResponseStatus());
        assertTrue(SpyWebClient.lastResponse().contains("No segmentation available for resource"));
    }

    @Test
    private void updatingQuotaWithSegmentsFromInvalidSegmentationShouldFail() {
        assertThrows(Throwable.class, () -> {
            dispenser()
                    .service(GENCFG)
                    .syncState()
                    .quotas()
                    .changeQuota(DiQuotaState
                            .forResource(GENCFG_SEGMENT_CPU)
                            .forProject(YANDEX)
                            .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                            .withMax(DiAmount.of(1000, DiUnit.CORES))
                            .build())
                    .performBy(AMOSOV_F);

        });
        assertEquals(HttpStatus.SC_BAD_REQUEST, SpyWebClient.lastResponseStatus());
        assertTrue(SpyWebClient.lastResponse().contains("Invalid segmentation"));
    }

    @Test
    private void updatingRawQuotaWithSegmentsFromInvalidSegmentationShouldFail() {
        dispenser()
                .properties()
                .setProperty(ServiceService.RAW_QUOTA_IMPORT_ENTITY, ServiceService.ENDPOINT_ENABLED_PROPERTY, true)
                .performBy(AMOSOV_F);
        updateHierarchy();
        assertThrows(Throwable.class, () -> {
            dispenser()
                    .service(GENCFG)
                    .syncState()
                    .rawQuotas()
                    .changeRawQuota(DiQuotaState
                            .forResource(GENCFG_SEGMENT_CPU)
                            .forProject(YANDEX)
                            .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                            .withMax(DiAmount.of(1000, DiUnit.CORES))
                            .build())
                    .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        });
        assertEquals(HttpStatus.SC_BAD_REQUEST, SpyWebClient.lastResponseStatus());
        assertTrue(SpyWebClient.lastResponse().contains("Invalid segmentation"));
    }

    @Test
    public void quotaLightViewsCanBeRequested() {
        final DiListResponse<DiQuotaLightView> quotaLightViews = dispenser()
                .quotas()
                .getLightViews()
                .inService(YP)
                .forResource(SEGMENT_CPU)
                .ofProject(DEFAULT)
                .withSegments(DC_SEGMENT_1)
                .perform();

        assertFalse(quotaLightViews.isEmpty());
        assertTrue(quotaLightViews.stream().allMatch(q -> q.getKey().getProjectKey().equals(DEFAULT)));
        assertTrue(quotaLightViews.stream().allMatch(q -> q.getKey().getServiceKey().equals(YP)));
        assertTrue(quotaLightViews.stream().allMatch(q -> q.getKey().getResourceKey().equals(SEGMENT_CPU)));
        assertTrue(quotaLightViews.stream().allMatch(q -> q.getKey().getSegmentKeys().contains(DC_SEGMENT_1)));
    }

    @Test
    public void quotaLightViewShouldContainCorrectData() {
        dispenser().service(YP).syncState().quotas()
                .changeQuota(DiQuotaState
                        .forResource(SEGMENT_STORAGE)
                        .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                        .forProject(VERTICALI)
                        .withMax(DiAmount.of(256, DiUnit.BYTE))
                        .build())
                .performBy(SLONNN);
        dispenser().service(YP).syncState().quotas()
                .changeQuota(DiQuotaState
                        .forResource(SEGMENT_STORAGE)
                        .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                        .forProject(VERTICALI)
                        .withActual(DiAmount.of(100, DiUnit.BYTE))
                        .build())
                .perform();

        updateHierarchy();

        final DiListResponse<DiQuotaLightView> quotaLightViews = dispenser()
                .quotas()
                .getLightViews()
                .inService(YP)
                .forResource(SEGMENT_STORAGE)
                .ofProject(VERTICALI)
                .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                .perform();

        assertEquals(1, quotaLightViews.size());

        final DiQuotaLightView quotaView = quotaLightViews.getFirst();

        assertEquals(VERTICALI, quotaView.getKey().getProjectKey());
        assertEquals(YP, quotaView.getKey().getServiceKey());
        assertEquals(SEGMENT_STORAGE, quotaView.getKey().getResourceKey());
        assertEquals(Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1), quotaView.getKey().getSegmentKeys());
        assertEquals(256, quotaView.getMax());
        assertEquals(100, quotaView.getActual());
    }

    @Test
    public void quotaOwnMaxCanBeUpdated() {

        final DiQuotaGetResponse quotas = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .ofProject(YANDEX)
                .forResource(YT_CPU)
                .perform();

        final DiQuota quota = quotas.getFirst();

        final DiUnit baseUnit = quota.getMax().getUnit();

        final long newMax = quota.getMax().getValue() + 100;
        dispenser()
                .service(NIRVANA)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject(YANDEX)
                        .withOwnMax(DiAmount.of(100, baseUnit))
                        .withMax(DiAmount.of(newMax, baseUnit))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaGetResponse updatedQuotas = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .ofProject(YANDEX)
                .forResource(YT_CPU)
                .perform();

        final DiQuota updatedQuota = updatedQuotas.getFirst();

        assertEquals(100, baseUnit.convert(updatedQuota.getOwnMax()));
        assertEquals(baseUnit.convert(updatedQuota.getMax()), newMax);
    }

    @Test
    public void quotaOwnMaxCanBeUpdatedRaw() {

        final DiQuotaGetResponse quotas = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .ofProject(YANDEX)
                .forResource(YT_CPU)
                .perform();

        final DiQuota quota = quotas.getFirst();

        final DiUnit baseUnit = quota.getMax().getUnit();

        final long newMax = quota.getMax().getValue() + 100;
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
                        .forProject(YANDEX)
                        .withOwnMax(DiAmount.of(100, baseUnit))
                        .withMax(DiAmount.of(newMax, baseUnit))
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        final DiQuotaGetResponse updatedQuotas = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .ofProject(YANDEX)
                .forResource(YT_CPU)
                .perform();

        final DiQuota updatedQuota = updatedQuotas.getFirst();

        assertEquals(100, baseUnit.convert(updatedQuota.getOwnMax()));
        assertEquals(baseUnit.convert(updatedQuota.getMax()), newMax);
    }

    @Test
    public void ownMaxShouldBeUsedInCheckMaxValues() {
        assertThrowsWithMessage(() -> {
            dispenser()
                    .service(NIRVANA)
                    .syncState()
                    .quotas()
                    .changeQuota(DiQuotaState.forResource(YT_CPU)
                            .forProject(YANDEX)
                            .withOwnMax(DiAmount.of(42, DiUnit.COUNT))
                            .build())
                    .performBy(AMOSOV_F);
        }, "is less than subprojects usage");
    }

    @Test
    public void actualValuesForNonLeafProjectQuotasShouldBeAggregated() {
        dispenser()
                .service(NIRVANA)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject(YANDEX)
                        .withActual(DiAmount.of(10, DiUnit.COUNT))
                        .build())
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject(INFRA)
                        .withActual(DiAmount.of(20, DiUnit.COUNT))
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        final DiQuotaGetResponse quotas = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .ofProject(YANDEX)
                .forResource(YT_CPU)
                .perform();

        final DiQuota quota = quotas.getFirst();

        assertEquals(10, DiUnit.COUNT.convert(quota.getOwnActual()));
        assertEquals(30, DiUnit.COUNT.convert(quota.getActual()));
    }

    @Test
    public void actualValuesForNonLeafProjectQuotasShouldBeAggregatedByDedicatedEndpoint() {
        dispenser()
                .service(NIRVANA)
                .syncState()
                .actualQuotas()
                .changeActualQuota(DiActualQuotaUpdate.forResource(YT_CPU)
                        .project(YANDEX)
                        .actual(DiAmount.of(10, DiUnit.COUNT))
                        .build())
                .changeActualQuota(DiActualQuotaUpdate.forResource(YT_CPU)
                        .project(INFRA)
                        .actual(DiAmount.of(20, DiUnit.COUNT))
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        final DiQuotaGetResponse quotas = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .ofProject(YANDEX)
                .forResource(YT_CPU)
                .perform();

        final DiQuota quota = quotas.getFirst();

        assertEquals(10, DiUnit.COUNT.convert(quota.getOwnActual()));
        assertEquals(30, DiUnit.COUNT.convert(quota.getActual()));
    }

    @Test
    public void actualValuesRawForNonLeafProjectQuotasShouldBeAggregated() {
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
                        .forProject(YANDEX)
                        .withActual(DiAmount.of(10, DiUnit.COUNT))
                        .build())
                .changeRawQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject(INFRA)
                        .withActual(DiAmount.of(20, DiUnit.COUNT))
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        final DiQuotaGetResponse quotas = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .ofProject(YANDEX)
                .forResource(YT_CPU)
                .perform();

        final DiQuota quota = quotas.getFirst();

        assertEquals(10, DiUnit.COUNT.convert(quota.getOwnActual()));
        assertEquals(30, DiUnit.COUNT.convert(quota.getActual()));
    }

    @Test
    public void ownMaxValueCantBeUpdateLessThanActual() {
        dispenser()
                .service(GENCFG)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(GENCFG_SEGMENT_CPU)
                        .forProject(YANDEX)
                        .withActual(DiAmount.of(10, DiUnit.CORES))
                        .withSegments(DC_SEGMENT_1, GENCFG_SEGMENT_SEGMENT_2)
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        assertThrowsWithMessage(() -> {
            dispenser()
                    .service(GENCFG)
                    .syncState()
                    .quotas()
                    .changeQuota(DiQuotaState.forResource(GENCFG_SEGMENT_CPU)
                            .forProject(YANDEX)
                            .withMax(DiAmount.of(1000, DiUnit.CORES))
                            .withOwnMax(DiAmount.of(1, DiUnit.CORES))
                            .withSegments(DC_SEGMENT_1, GENCFG_SEGMENT_SEGMENT_2)
                            .build())
                    .performBy(AMOSOV_F);
        }, "is less than own actual");

    }

    @Test
    public void totalMaxConvertedToOwnMaxOnlyForLeafProjects() {

        final long rootMax = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .ofProject(YANDEX)
                .forResource(YT_CPU)
                .perform().getFirst().getMax(DiUnit.PERMILLE);

        dispenser()
                .projects()
                .create(DiProject.withKey("p1")
                        .withName("p1")
                        .withDescription("p1")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .withParentProject(YANDEX)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .service(NIRVANA)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject(YANDEX)
                        .withMax(DiAmount.of(rootMax + 100, DiUnit.PERMILLE))
                        .build())
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject("p1")
                        .withMax(DiAmount.of(100, DiUnit.PERMILLE))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        DiQuota p1Quota = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .ofProject("p1")
                .forResource(YT_CPU)
                .perform().getFirst();

        assertEquals(100, p1Quota.getMax(DiUnit.PERMILLE));
        assertEquals(100, DiUnit.PERMILLE.convert(p1Quota.getOwnMax()));

        dispenser()
                .projects()
                .create(DiProject.withKey("p2")
                        .withName("p2")
                        .withDescription("p2")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .withParentProject("p1")
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser()
                .service(NIRVANA)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject("p2")
                        .withMax(DiAmount.of(50, DiUnit.PERMILLE))
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        p1Quota = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .ofProject("p1")
                .forResource(YT_CPU)
                .perform().getFirst();

        assertEquals(100, p1Quota.getMax(DiUnit.PERMILLE));
        assertEquals(0, DiUnit.PERMILLE.convert(p1Quota.getOwnMax()));
    }

    @Test
    public void ytGpuResourcesQuotaShouldContainsCustomStatisticsLink() {

        final DiResource gpu = dispenser().service(NIRVANA)
                .resource("tesla-m40-gpu")
                .create()
                .withName("tesla-m40-gpu")
                .withDescription("tesla-m40-gpu")
                .withType(DiResourceType.ENUMERABLE)
                .performBy(AMOSOV_F);

        updateHierarchy();

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/quota-specifications/" + NIRVANA + "/" + gpu.getKey() + "/" + gpu.getKey() + "-quota")
                .put("{\"description\":\"description\"}", DiQuotaSpec.class);

        updateHierarchy();

        DiQuota quota = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .ofProject(YANDEX)
                .forResource(YT_CPU)
                .perform().getFirst();

        assertEquals("https://solomon.yandex-team.ru/?project=dispenser_common_dev&cluster=dispenser_qloud_env&service=dispenser_dev&graph=auto&l.sensor=yandex:%20nirvana/yt-cpu/yt-cpu&l.attribute=actual%7Cmax&b=31d&stack=0&secondaryGraphMode=none", quota.getStatisticsLink());

        quota = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .ofProject(YANDEX)
                .forResource(gpu.getKey())
                .perform().getFirst();

        assertEquals("https://solomon.yandex-team.ru/?project=yt&cluster=hahn&service=yt_scheduler&graph=gpu-utilization&l.tree=gpu_tesla_m40&l.pool=nirvana-yandex&autorefresh=y&b=31d&e=", quota.getStatisticsLink());
    }
}
