package ru.yandex.qe.dispenser.ws.logic;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceReader;
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.resources_model.ResourcesModelMappingDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentReader;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.domain.resources_model.ExternalResource;
import ru.yandex.qe.dispenser.domain.resources_model.InternalResource;
import ru.yandex.qe.dispenser.domain.resources_model.QuotaRequestDelivery;
import ru.yandex.qe.dispenser.domain.resources_model.QuotaRequestDeliveryContext;
import ru.yandex.qe.dispenser.domain.resources_model.ResourceModelMappingTarget;
import ru.yandex.qe.dispenser.domain.resources_model.ResourcesModelMapping;
import ru.yandex.qe.dispenser.domain.util.CollectionUtils;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.bot.BotResourceType;
import ru.yandex.qe.dispenser.ws.resources_model.ResourcesModelMapper;
import ru.yandex.qe.dispenser.ws.resources_model.ResourcesModelMapperManager;
import ru.yandex.qe.dispenser.ws.resources_model.YdbResourceModelMapper;
import ru.yandex.qe.dispenser.ws.resources_model.YpResourceModelMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class ResourcesModelMappingsTest extends BusinessLogicTestBase {
    public static final String YDB_CLUSTER_SEGMENTATION = "ydb_cluster";
    @Autowired
    private ResourcesModelMapperManager mapperManager;

    @Autowired
    private CampaignDao campaignDao;

    @Autowired
    private BigOrderManager bigOrderManager;

    @Autowired
    private ResourcesModelMappingDao resourcesModelMappingDao;

    @Autowired
    private ResourceSegmentationDao resourceSegmentationDao;

    @Autowired
    private SegmentationDao segmentationDao;

    @Autowired
    private PersonDao personDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;
    @Autowired
    private SegmentDao segmentDao;

    private Campaign campaign;
    private List<BigOrder> bigOrders;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        resourcesModelMappingDao.clear();
        bigOrderManager.clear();
        BigOrder bigOrderOne = bigOrderManager.create(BigOrder.builder(LocalDate.of(2019, 1, 1)));
        BigOrder bigOrderTwo = bigOrderManager.create(BigOrder.builder(LocalDate.of(2019, 2, 1)));
        BigOrder bigOrderThree = bigOrderManager.create(BigOrder.builder(LocalDate.of(2019, 3, 1)));
        BigOrder bigOrderFour = bigOrderManager.create(BigOrder.builder(LocalDate.of(2019, 4, 1)));
        List<Long> bigOrderIds = ImmutableList.of(bigOrderOne.getId(), bigOrderTwo.getId(), bigOrderThree.getId(), bigOrderFour.getId());
        bigOrders = bigOrderManager.getByIds(bigOrderIds).stream()
                .sorted(Comparator.comparing(BigOrder::getId))
                .collect(Collectors.toList());
        campaign = createCampaign(bigOrderIds);

        final Segmentation ydbClusterSegmentation =
                segmentationDao.create(new Segmentation.Builder(YDB_CLUSTER_SEGMENTATION)
                        .description("YDB")
                        .name("YDB")
                        .priority(42)
                        .build());
        updateHierarchy();

        segmentDao.createAll(Stream.of("ydb-ru", "ydb-eu", "ydb-ru-prestable")
                .map(clusterKey -> new Segment.Builder(clusterKey, ydbClusterSegmentation)
                        .name(clusterKey)
                        .description(clusterKey)
                        .priority((short) 42)
                        .build()
                )
                .collect(Collectors.toList())
        );

        updateHierarchy();
    }

    @Test
    public void simpleMapperShouldWork() {
        Person author = personDao.readPersonByLogin(QDEEE.getLogin());
        Service nirvana = serviceDao.read(NIRVANA);
        Service updatedNirvana = Service.copyOf(nirvana)
                .withSettings(nirvana.getSettings().copyBuilder()
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build();
        serviceDao.update(updatedNirvana);

        UUID resourceId = UUID.randomUUID();
        String baseUnitKey = "bytes";

        ResourceReader resourceReader = hierarchy.get().getResourceReader();

        Resource ytCpuResource = resourceReader.read(new Resource.Key(YT_CPU, nirvana));

        List<QuotaChangeRequest.Change> changes = List.of(
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(ytCpuResource)
                        .amount(100_000)
                        .segments(Set.of())
                        .amountReady(20_000)
                        .order(bigOrder(bigOrders.get(0)))
                        .build()
        );
        QuotaChangeRequest request = createRequest(changes);

        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(ytCpuResource)
                .addSegments(Set.of())
                .campaignId(campaign.getId())
                .target(ResourceModelMappingTarget.builder()
                        .numerator(1024)
                        .denominator(1000)
                        .externalResourceId(resourceId)
                        .externalResourceBaseUnitKey(baseUnitKey)
                        .build()));

        ResourcesModelMapper mapper = mapperManager.getMapperForProvider(updatedNirvana).get();

        QuotaRequestDelivery result = mapper.mapNonAllocatingToExternalResources(request, request.getChanges(),
                        updatedNirvana, author)
                .getQuotaRequestDelivery();

        Map<UUID, Map<Long, ExternalResource>> externalResources = new HashMap<>();
        Map<Pair<Long, Set<Long>>, Map<Long, InternalResource>> internalResources = new HashMap<>();
        result.getExternalResources().forEach(r -> externalResources
                .computeIfAbsent(r.getResourceId(), k -> new HashMap<>()).put(r.getBigOrderId(), r));
        result.getInternalResources().forEach(r -> internalResources
                .computeIfAbsent(Pair.of(r.getResourceId(), r.getSegmentIds()), k -> new HashMap<>())
                .put(r.getBigOrderId(), r));
        assertEquals(1, externalResources.size());
        assertEquals(1, internalResources.size());
        ExternalResource externalAmount = externalResources.get(resourceId)
                .get(bigOrders.get(0).getId());
        InternalResource internalAmount = internalResources
                .get(Pair.of(ytCpuResource.getId(), Set.of())).get(bigOrders.get(0).getId());
        assertEquals(baseUnitKey, externalAmount.getUnitKey());
        assertEquals(externalAmount.getAmount(), 20_480);
        assertEquals(internalAmount.getAmount(), 20_000);
    }

    @Test
    public void roundingModeTest() {
        Person author = personDao.readPersonByLogin(QDEEE.getLogin());
        Service nirvana = serviceDao.read(NIRVANA);
        Service updatedNirvana = Service.copyOf(nirvana)
                .withSettings(nirvana.getSettings().copyBuilder()
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build();
        serviceDao.update(updatedNirvana);

        UUID resourceId = UUID.randomUUID();
        String baseUnitKey = "cores";

        ResourceReader resourceReader = hierarchy.get().getResourceReader();

        Resource ytCpuResource = resourceReader.read(new Resource.Key(YT_CPU, nirvana));

        List<QuotaChangeRequest.Change> changes = List.of(
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(ytCpuResource)
                        .amount(51_000)
                        .segments(Set.of())
                        .amountReady(51_000)
                        .order(bigOrder(bigOrders.get(0)))
                        .build()
        );
        QuotaChangeRequest request = createRequest(changes);

        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(ytCpuResource)
                .addSegments(Set.of())
                .campaignId(campaign.getId())
                .rounding(RoundingMode.UP)
                .target(ResourceModelMappingTarget.builder()
                        .numerator(1)
                        .denominator(50_000)
                        .externalResourceId(resourceId)
                        .externalResourceBaseUnitKey(baseUnitKey)
                        .build()));

        ResourcesModelMapper mapper = mapperManager.getMapperForProvider(updatedNirvana).orElseThrow();

        QuotaRequestDelivery result = mapper.mapNonAllocatingToExternalResources(request, request.getChanges(),
                        updatedNirvana, author)
                .getQuotaRequestDelivery();

        Map<UUID, Map<Long, ExternalResource>> externalResources = new HashMap<>();
        Map<Pair<Long, Set<Long>>, Map<Long, InternalResource>> internalResources = new HashMap<>();
        result.getExternalResources().forEach(r -> externalResources
                .computeIfAbsent(r.getResourceId(), k -> new HashMap<>()).put(r.getBigOrderId(), r));
        result.getInternalResources().forEach(r -> internalResources
                .computeIfAbsent(Pair.of(r.getResourceId(), r.getSegmentIds()), k -> new HashMap<>())
                .put(r.getBigOrderId(), r));
        assertEquals(1, externalResources.size());
        assertEquals(1, internalResources.size());
        ExternalResource externalAmount = externalResources.get(resourceId)
                .get(bigOrders.get(0).getId());
        InternalResource internalAmount = internalResources
                .get(Pair.of(ytCpuResource.getId(), Set.of())).get(bigOrders.get(0).getId());
        assertEquals(baseUnitKey, externalAmount.getUnitKey());
        assertEquals(2, externalAmount.getAmount());
        assertEquals(51_000, internalAmount.getAmount());
    }

    @Test
    public void simpleMapperShouldWorkEvenWithoutChanges() {
        Person author = personDao.readPersonByLogin(QDEEE.getLogin());
        Service nirvana = serviceDao.read(NIRVANA);
        Service updatedNirvana = Service.copyOf(nirvana)
                .withSettings(nirvana.getSettings().copyBuilder()
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build();
        serviceDao.update(updatedNirvana);

        UUID resourceId = UUID.randomUUID();
        String baseUnitKey = "bytes";

        ResourceReader resourceReader = hierarchy.get().getResourceReader();

        Resource ytCpuResource = resourceReader.read(new Resource.Key(YT_CPU, nirvana));

        List<QuotaChangeRequest.Change> changes = List.of(
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(ytCpuResource)
                        .amount(100_000)
                        .segments(Set.of())
                        .amountReady(0)
                        .order(bigOrder(bigOrders.get(0)))
                        .build()
        );
        QuotaChangeRequest request = createRequest(changes);

        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(ytCpuResource)
                .addSegments(Set.of())
                .campaignId(campaign.getId())
                .target(ResourceModelMappingTarget.builder()
                        .numerator(1024)
                        .denominator(1000)
                        .externalResourceId(resourceId)
                        .externalResourceBaseUnitKey(baseUnitKey)
                        .build()));

        ResourcesModelMapper mapper = mapperManager.getMapperForProvider(updatedNirvana).get();

        QuotaRequestDeliveryContext result = mapper.mapNonAllocatingToExternalResources(request, request.getChanges()
                , updatedNirvana, author);

        assertTrue(result.getAffectedChanges().isEmpty());
        final QuotaRequestDelivery quotaRequestDelivery = result.getQuotaRequestDelivery();
        assertTrue(quotaRequestDelivery.getInternalResources().isEmpty());
        assertTrue(quotaRequestDelivery.getExternalResources().isEmpty());
    }

    @Test
    public void simpleMapperShouldIgnoreZeroAmountExternalResources() {
        Person author = personDao.readPersonByLogin(QDEEE.getLogin());
        Service nirvana = serviceDao.read(NIRVANA);
        Service updatedNirvana = Service.copyOf(nirvana)
                .withSettings(nirvana.getSettings().copyBuilder()
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build();
        serviceDao.update(updatedNirvana);

        UUID resourceId = UUID.randomUUID();
        String baseUnitKey = "bytes";

        ResourceReader resourceReader = hierarchy.get().getResourceReader();

        Resource ytCpuResource = resourceReader.read(new Resource.Key(YT_CPU, nirvana));

        List<QuotaChangeRequest.Change> changes = List.of(
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(ytCpuResource)
                        .amount(100_000)
                        .segments(Set.of())
                        .amountReady(100_000)
                        .order(bigOrder(bigOrders.get(0)))
                        .build()
        );
        QuotaChangeRequest request = createRequest(changes);

        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(ytCpuResource)
                .addSegments(Set.of())
                .campaignId(campaign.getId())
                .target(ResourceModelMappingTarget.builder()
                        .numerator(1)
                        .denominator(1_000_000)
                        .externalResourceId(resourceId)
                        .externalResourceBaseUnitKey(baseUnitKey)
                        .build()));

        ResourcesModelMapper mapper = mapperManager.getMapperForProvider(updatedNirvana).get();

        QuotaRequestDeliveryContext result = mapper.mapNonAllocatingToExternalResources(request, request.getChanges()
                , updatedNirvana, author);

        final QuotaRequestDelivery quotaRequestDelivery = result.getQuotaRequestDelivery();
        assertFalse(quotaRequestDelivery.getInternalResources().isEmpty());
        assertTrue(quotaRequestDelivery.getExternalResources().isEmpty());
        assertFalse(result.getAffectedChanges().isEmpty());
    }

    @Test
    public void simpleMapperEmptyTargetShouldWork() {
        Person author = personDao.readPersonByLogin(QDEEE.getLogin());
        Service nirvana = serviceDao.read(NIRVANA);
        Service updatedNirvana = Service.copyOf(nirvana)
                .withSettings(nirvana.getSettings().copyBuilder()
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build();
        serviceDao.update(updatedNirvana);

        ResourceReader resourceReader = hierarchy.get().getResourceReader();

        Resource ytCpuResource = resourceReader.read(new Resource.Key(YT_CPU, nirvana));

        List<QuotaChangeRequest.Change> changes = List.of(
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(ytCpuResource)
                        .amount(100_000)
                        .segments(Set.of())
                        .amountReady(20_000)
                        .order(bigOrder(bigOrders.get(0)))
                        .build()
        );
        QuotaChangeRequest request = createRequest(changes);

        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(ytCpuResource)
                .addSegments(Set.of())
                .campaignId(campaign.getId()));

        ResourcesModelMapper mapper = mapperManager.getMapperForProvider(updatedNirvana).get();

        QuotaRequestDelivery result = mapper.mapNonAllocatingToExternalResources(request, request.getChanges(),
                        updatedNirvana, author)
                .getQuotaRequestDelivery();

        Map<UUID, Map<Long, ExternalResource>> externalResources = new HashMap<>();
        Map<Pair<Long, Set<Long>>, Map<Long, InternalResource>> internalResources = new HashMap<>();
        result.getExternalResources().forEach(r -> externalResources
                .computeIfAbsent(r.getResourceId(), k -> new HashMap<>()).put(r.getBigOrderId(), r));
        result.getInternalResources().forEach(r -> internalResources
                .computeIfAbsent(Pair.of(r.getResourceId(), r.getSegmentIds()), k -> new HashMap<>())
                .put(r.getBigOrderId(), r));
        assertEquals(0, externalResources.size());
        assertEquals(1, internalResources.size());
        InternalResource internalAmount = internalResources
                .get(Pair.of(ytCpuResource.getId(), Set.of())).get(bigOrders.get(0).getId());
        assertEquals(internalAmount.getAmount(), 20_000);
    }

    @Test
    public void simpleMapperWithSeveralMappingsShouldWork() {
        Person author = personDao.readPersonByLogin(QDEEE.getLogin());
        Service nirvana = serviceDao.read(NIRVANA);
        Service updatedNirvana = Service.copyOf(nirvana)
                .withSettings(nirvana.getSettings().copyBuilder()
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build();
        serviceDao.update(updatedNirvana);

        UUID resourceId = UUID.randomUUID();
        UUID anotherResourceId = UUID.randomUUID();
        String baseUnitKey = "bytes";

        ResourceReader resourceReader = hierarchy.get().getResourceReader();

        Resource ytCpuResource = resourceReader.read(new Resource.Key(YT_CPU, nirvana));

        List<QuotaChangeRequest.Change> changes = List.of(
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(ytCpuResource)
                        .amount(100_000)
                        .segments(Set.of())
                        .amountReady(20_000)
                        .order(bigOrder(bigOrders.get(0)))
                        .build()
        );
        QuotaChangeRequest request = createRequest(changes);

        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(ytCpuResource)
                .addSegments(Set.of())
                .campaignId(campaign.getId())
                .target(ResourceModelMappingTarget.builder()
                        .numerator(1024)
                        .denominator(1000)
                        .externalResourceId(resourceId)
                        .externalResourceBaseUnitKey(baseUnitKey)
                        .build()));
        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(ytCpuResource)
                .addSegments(Set.of())
                .campaignId(campaign.getId())
                .target(ResourceModelMappingTarget.builder()
                        .numerator(2048)
                        .denominator(1000)
                        .externalResourceId(anotherResourceId)
                        .externalResourceBaseUnitKey(baseUnitKey)
                        .build()));

        ResourcesModelMapper mapper = mapperManager.getMapperForProvider(updatedNirvana).get();

        QuotaRequestDelivery result = mapper.mapNonAllocatingToExternalResources(request, request.getChanges(),
                        updatedNirvana, author)
                .getQuotaRequestDelivery();

        Map<UUID, Map<Long, ExternalResource>> externalResources = new HashMap<>();
        Map<Pair<Long, Set<Long>>, Map<Long, InternalResource>> internalResources = new HashMap<>();
        result.getExternalResources().forEach(r -> externalResources
                .computeIfAbsent(r.getResourceId(), k -> new HashMap<>()).put(r.getBigOrderId(), r));
        result.getInternalResources().forEach(r -> internalResources
                .computeIfAbsent(Pair.of(r.getResourceId(), r.getSegmentIds()), k -> new HashMap<>())
                .put(r.getBigOrderId(), r));
        assertEquals(2, externalResources.size());
        assertEquals(1, internalResources.size());
        ExternalResource externalAmountOne = externalResources.get(resourceId)
                .get(bigOrders.get(0).getId());
        ExternalResource externalAmountTwo = externalResources.get(anotherResourceId)
                .get(bigOrders.get(0).getId());
        InternalResource internalAmount = internalResources
                .get(Pair.of(ytCpuResource.getId(), Set.of())).get(bigOrders.get(0).getId());
        assertEquals(baseUnitKey, externalAmountOne.getUnitKey());
        assertEquals(externalAmountOne.getAmount(), 20_480);
        assertEquals(externalAmountTwo.getAmount(), 40_960);
        assertEquals(internalAmount.getAmount(), 20_000);
    }

    @Test
    public void simpleMapperWithSeveralResourcesShouldWork() {
        Person author = personDao.readPersonByLogin(QDEEE.getLogin());
        Service nirvana = serviceDao.read(NIRVANA);
        Service updatedNirvana = Service.copyOf(nirvana)
                .withSettings(nirvana.getSettings().copyBuilder()
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build();
        serviceDao.update(updatedNirvana);

        UUID resourceId = UUID.randomUUID();
        UUID resource2Id = UUID.randomUUID();
        String baseUnitKey = "bytes";
        String baseUnit2Key = "cores";

        ResourceReader resourceReader = hierarchy.get().getResourceReader();

        Resource ytCpuResource = resourceReader.read(new Resource.Key(YT_CPU, nirvana));
        Resource ytGpuResource = resourceReader.read(new Resource.Key(YT_GPU, nirvana));
        Resource storageResource = resourceReader.read(new Resource.Key(STORAGE, nirvana));

        ResourcesModelMapper mapper = mapperManager.getMapperForProvider(updatedNirvana).get();
        List<QuotaChangeRequest.Change> changes = List.of(
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(ytCpuResource)
                        .amount(100_000)
                        .segments(Set.of())
                        .amountReady(20_000)
                        .order(bigOrder(bigOrders.get(0)))
                        .build(),
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(ytGpuResource)
                        .amount(200_000)
                        .segments(Set.of())
                        .amountReady(30_000)
                        .order(bigOrder(bigOrders.get(1)))
                        .build(),
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(storageResource)
                        .amount(150_000)
                        .segments(Set.of())
                        .amountReady(40_000)
                        .order(bigOrder(bigOrders.get(1)))
                        .build()
        );
        QuotaChangeRequest request = createRequest(changes);

        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(ytCpuResource)
                .addSegments(Set.of())
                .campaignId(campaign.getId())
                .target(ResourceModelMappingTarget.builder()
                        .numerator(17)
                        .denominator(100)
                        .externalResourceId(resourceId)
                        .externalResourceBaseUnitKey(baseUnitKey)
                        .build()));

        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(ytGpuResource)
                .addSegments(Set.of())
                .campaignId(campaign.getId())
                .target(ResourceModelMappingTarget.builder()
                        .numerator(1000)
                        .denominator(1000)
                        .externalResourceId(resource2Id)
                        .externalResourceBaseUnitKey(baseUnit2Key)
                        .build()));

        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(storageResource)
                .addSegments(Set.of())
                .campaignId(campaign.getId())
                .target(ResourceModelMappingTarget.builder()
                        .numerator(997)
                        .denominator(1000)
                        .externalResourceId(resource2Id)
                        .externalResourceBaseUnitKey(baseUnit2Key)
                        .build()));

        QuotaRequestDelivery result = mapper.mapNonAllocatingToExternalResources(request, request.getChanges(),
                        updatedNirvana, author)
                .getQuotaRequestDelivery();

        Map<UUID, Map<Long, ExternalResource>> externalResources = new HashMap<>();
        Map<Pair<Long, Set<Long>>, Map<Long, InternalResource>> internalResources
                = new HashMap<>();
        result.getExternalResources().forEach(r -> externalResources
                .computeIfAbsent(r.getResourceId(), k -> new HashMap<>()).put(r.getBigOrderId(), r));
        result.getInternalResources().forEach(r -> internalResources
                .computeIfAbsent(Pair.of(r.getResourceId(), r.getSegmentIds()), k -> new HashMap<>())
                .put(r.getBigOrderId(), r));

        assertEquals(2, externalResources.size());
        assertEquals(3, internalResources.size());
        ExternalResource externalAmount = externalResources.get(resourceId)
                .get(bigOrders.get(0).getId());
        InternalResource internalAmount = internalResources
                .get(Pair.of(ytCpuResource.getId(), Set.of())).get(bigOrders.get(0).getId());
        assertEquals(baseUnitKey, externalAmount.getUnitKey());
        assertEquals(externalAmount.getAmount(), 3_400);
        assertEquals(internalAmount.getAmount(), 20_000);

        externalAmount = externalResources.get(resource2Id).get(bigOrders.get(1).getId());
        InternalResource internalAmountOne = internalResources
                .get(Pair.of(ytGpuResource.getId(), Set.of())).get(bigOrders.get(1).getId());
        InternalResource internalAmountTwo = internalResources
                .get(Pair.of(storageResource.getId(), Set.of())).get(bigOrders.get(1).getId());
        assertEquals(baseUnit2Key, externalAmount.getUnitKey());
        assertEquals(externalAmount.getAmount(), 30_000 + 39_880);
        assertEquals(internalAmountOne.getAmount(), 30_000);
        assertEquals(internalAmountTwo.getAmount(), 40_000);
    }

    @Test
    public void simpleMapperForSegmentedResourceShouldWork() {
        Person author = personDao.readPersonByLogin(QDEEE.getLogin());
        Service yp = serviceDao.read(YP);
        Service service = Service.copyOf(yp)
                .withSettings(yp.getSettings().copyBuilder()
                        .resourcesMappingBeanName("simpleResourceModelMapper")
                        .build())
                .build();
        serviceDao.update(service);

        UUID resourceId = UUID.randomUUID();
        String baseUnitKey = "bytes";

        UUID resource2Id = UUID.randomUUID();
        String baseUnit2Key = "cores";

        ResourceReader resourceReader = hierarchy.get().getResourceReader();

        SegmentReader segmentReader = hierarchy.get().getSegmentReader();
        Segment ss1 = segmentReader.read(SEGMENT_SEGMENT_1);
        Segment dc1 = segmentReader.read(DC_SEGMENT_1);
        Segment dc2 = segmentReader.read(DC_SEGMENT_2);

        Resource cpuResource = resourceReader.read(new Resource.Key(SEGMENT_CPU, yp));

        List<QuotaChangeRequest.Change> changes = List.of(
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(cpuResource)
                        .amount(80_000)
                        .segments(Set.of(ss1, dc1))
                        .amountReady(40_000)
                        .order(bigOrder(bigOrders.get(0)))
                        .build(),
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(cpuResource)
                        .amount(100_000)
                        .segments(Set.of(ss1, dc2))
                        .amountReady(20_000)
                        .order(bigOrder(bigOrders.get(0)))
                        .build()
        );
        QuotaChangeRequest request = createRequest(changes);

        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(cpuResource)
                .addSegments(Set.of(ss1, dc1))
                .campaignId(campaign.getId())
                .target(ResourceModelMappingTarget.builder()
                        .numerator(500)
                        .denominator(1000)
                        .externalResourceId(resourceId)
                        .externalResourceBaseUnitKey(baseUnitKey)
                        .build()));

        resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(cpuResource)
                .addSegments(Set.of(ss1, dc2))
                .campaignId(campaign.getId())
                .target(ResourceModelMappingTarget.builder()
                        .numerator(600)
                        .denominator(1000)
                        .externalResourceId(resource2Id)
                        .externalResourceBaseUnitKey(baseUnit2Key)
                        .build()));

        ResourcesModelMapper mapper = mapperManager.getMapperForProvider(service).get();
        QuotaRequestDelivery result = mapper.mapNonAllocatingToExternalResources(request, request.getChanges(),
                        service, author)
                .getQuotaRequestDelivery();

        Map<UUID, Map<Long, ExternalResource>> externalResources = new HashMap<>();
        Map<Pair<Long, Set<Long>>, Map<Long, InternalResource>> internalResources
                = new HashMap<>();
        result.getExternalResources().forEach(r -> externalResources
                .computeIfAbsent(r.getResourceId(), k -> new HashMap<>()).put(r.getBigOrderId(), r));
        result.getInternalResources().forEach(r -> internalResources
                .computeIfAbsent(Pair.of(r.getResourceId(), r.getSegmentIds()), k -> new HashMap<>())
                .put(r.getBigOrderId(), r));

        assertEquals(2, externalResources.size());
        assertEquals(2, internalResources.size());
        ExternalResource externalAmount = externalResources.get(resourceId)
                .get(bigOrders.get(0).getId());
        InternalResource internalAmount = internalResources
                .get(Pair.of(cpuResource.getId(), Set.of(ss1.getId(), dc1.getId()))).get(bigOrders.get(0).getId());
        assertEquals(baseUnitKey, externalAmount.getUnitKey());
        assertEquals(externalAmount.getAmount(), 20_000);
        assertEquals(internalAmount.getAmount(), 40_000);

        externalAmount = externalResources.get(resource2Id).get(bigOrders.get(0).getId());
        internalAmount = internalResources
                .get(Pair.of(cpuResource.getId(), Set.of(ss1.getId(), dc2.getId()))).get(bigOrders.get(0).getId());
        assertEquals(baseUnit2Key, externalAmount.getUnitKey());
        assertEquals(externalAmount.getAmount(), 12_000);
        assertEquals(internalAmount.getAmount(), 20_000);
    }

    @Test
    public void ypMapperShouldWorkCorrectlyWithNewResources() {
        Person author = personDao.readPersonByLogin(QDEEE.getLogin());
        Service yp = serviceDao.read(YP);
        Service service = Service.copyOf(yp)
                .withSettings(yp.getSettings().copyBuilder()
                        .resourcesMappingBeanName("ypResourceModelMapper")
                        .build())
                .build();
        serviceDao.update(service);
        updateHierarchy();
        final Resource cpuSegmented = createResource("cpu_segmented", service, DiResourceType.PROCESSOR);
        final Resource hddSegmented = createResource("hdd_segmented", service, DiResourceType.STORAGE);
        final Resource ssdSegmented = createResource("ssd_segmented", service, DiResourceType.STORAGE);
        final Resource ioSsd = createResource("io_ssd", service, DiResourceType.BINARY_TRAFFIC);
        final Resource ioHdd = createResource("io_hdd", service, DiResourceType.BINARY_TRAFFIC);
        updateHierarchy();

        final Map<Segmentation.Key, Segmentation> segmentationByKey =
                Hierarchy.get().getSegmentationReader().readAll(List.of(
                        new Segmentation.Key(DC_SEGMENTATION),
                        new Segmentation.Key(SEGMENT_SEGMENTATION)
                ));
        for (Resource resource : List.of(cpuSegmented, hddSegmented, ssdSegmented, ioSsd, ioHdd)) {
            resourceSegmentationDao.setSegmentations(resource, Set.copyOf(segmentationByKey.values()));
        }
        ResourceReader resourceReader = hierarchy.get().getResourceReader();

        SegmentReader segmentReader = hierarchy.get().getSegmentReader();
        Segment ss1 = segmentReader.read(SEGMENT_SEGMENT_1);
        Segment dc1 = segmentReader.read(DC_SEGMENT_1);
        Segment dc2 = segmentReader.read(DC_SEGMENT_2);
        updateHierarchy();

        prepareCampaignResources();

        final Set<Segment> segmentSet1 = Set.of(ss1, dc1);
        final Set<Segment> segmentSet2 = Set.of(ss1, dc2);
        final BigOrder bigOrder1 = bigOrders.get(0);
        final BigOrder bigOrder2 = bigOrders.get(1);
        List<QuotaChangeRequest.Change> changes = List.of(
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(cpuSegmented)
                        .amount(100_000)
                        .segments(segmentSet1)
                        .amountReady(100_000)
                        .order(bigOrder(bigOrder2))
                        .build(),
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(hddSegmented)
                        .amount(100_000)
                        .segments(segmentSet1)
                        .amountReady(50_000)
                        .order(bigOrder(bigOrder1))
                        .build(),
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(ioHdd)
                        .amount(100_000)
                        .segments(segmentSet1)
                        .amountReady(70_000)
                        .order(bigOrder(bigOrder1))
                        .build(),

                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(ssdSegmented)
                        .amount(100_000)
                        .segments(segmentSet2)
                        .amountReady(100_000)
                        .order(bigOrder(bigOrder2))
                        .build(),
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(ioSsd)
                        .amount(100_000)
                        .segments(segmentSet2)
                        .amountReady(10_000)
                        .order(bigOrder(bigOrder1))
                        .build()
        );
        QuotaChangeRequest request = createRequest(changes);

        final UUID extCpu = UUID.randomUUID();
        final UUID extHdd = UUID.randomUUID();
        final UUID extSsd = UUID.randomUUID();
        final UUID extIoSsd = UUID.randomUUID();
        final UUID extIoHdd = UUID.randomUUID();

        createMapping(cpuSegmented, segmentSet1, 1, 1000, extCpu, "cores");
        createMapping(hddSegmented, segmentSet1, 1, 1, extHdd, "bytes");
        createMapping(ioHdd, segmentSet1, 1, 1, extIoHdd, "bps");

        createMapping(ssdSegmented, segmentSet2, 1, 1, extSsd, "bytes");
        createMapping(ioSsd, segmentSet2, 1, 1, extIoSsd, "bps");

        ResourcesModelMapper mapper = mapperManager.getMapperForProvider(service).get();
        assertTrue(mapper instanceof YpResourceModelMapper);

        QuotaRequestDelivery result = mapper.mapNonAllocatingToExternalResources(request, request.getChanges(),
                        service, author)
                .getQuotaRequestDelivery();

        Map<UUID, Map<Long, ExternalResource>> externalResources = new HashMap<>();
        Map<Pair<Long, Set<Long>>, Map<Long, InternalResource>> internalResources
                = new HashMap<>();
        result.getExternalResources().forEach(r -> externalResources
                .computeIfAbsent(r.getResourceId(), k -> new HashMap<>()).put(r.getBigOrderId(), r));
        result.getInternalResources().forEach(r -> internalResources
                .computeIfAbsent(Pair.of(r.getResourceId(), r.getSegmentIds()), k -> new HashMap<>())
                .put(r.getBigOrderId(), r));

        assertEquals(5, externalResources.size());
        assertEquals(5, internalResources.size());

        final Set<Long> ss1Ids = CollectionUtils.ids(segmentSet1);
        final Set<Long> ss2Ids = CollectionUtils.ids(segmentSet2);
        assertEquals(100_000L,
                internalResources.get(Pair.of(cpuSegmented.getId(), ss1Ids)).get(bigOrder2.getId()).getAmount());
        assertEquals(50_000L,
                internalResources.get(Pair.of(hddSegmented.getId(), ss1Ids)).get(bigOrder1.getId()).getAmount());
        assertEquals(70_000L, internalResources.get(Pair.of(ioHdd.getId(), ss1Ids)).get(bigOrder1.getId()).getAmount());
        assertEquals(100_000L,
                internalResources.get(Pair.of(ssdSegmented.getId(), ss2Ids)).get(bigOrder2.getId()).getAmount());
        assertEquals(10_000L, internalResources.get(Pair.of(ioSsd.getId(), ss2Ids)).get(bigOrder1.getId()).getAmount());

        assertEquals(100L, externalResources.get(extCpu).get(bigOrder2.getId()).getAmount());
        assertEquals(50_000L, externalResources.get(extHdd).get(bigOrder1.getId()).getAmount());
        assertEquals(70_000L, externalResources.get(extIoHdd).get(bigOrder1.getId()).getAmount());
        assertEquals(100_000L, externalResources.get(extSsd).get(bigOrder2.getId()).getAmount());
        assertEquals(10_000L, externalResources.get(extIoSsd).get(bigOrder1.getId()).getAmount());
    }

    @Test
    public void ypMapperShouldWorkCorrectlyWithOldResources() {
        Person author = personDao.readPersonByLogin(QDEEE.getLogin());
        Service yp = serviceDao.read(YP);
        final Service gencfg = serviceDao.read(GENCFG);
        Service service = Service.copyOf(gencfg)
                .withSettings(gencfg.getSettings().copyBuilder()
                        .resourcesMappingBeanName("ypResourceModelMapper")
                        .build())
                .build();
        serviceDao.update(service);
        updateHierarchy();
        final Resource cpu = createResource("cpu", service, DiResourceType.PROCESSOR);
        final Resource hdd = createResource("hdd", service, DiResourceType.STORAGE);
        final Resource ssd = createResource("ssd", service, DiResourceType.STORAGE);
        final Resource ioSsd = createResource("io_ssd", yp, DiResourceType.STORAGE);
        final Resource ioHdd = createResource("io_hdd", yp, DiResourceType.STORAGE);
        updateHierarchy();

        final Map<Segmentation.Key, Segmentation> segmentationByKey =
                Hierarchy.get().getSegmentationReader().readAll(List.of(
                        new Segmentation.Key(DC_SEGMENTATION)
                ));
        for (Resource resource : List.of(cpu, hdd, ssd)) {
            resourceSegmentationDao.setSegmentations(resource, Set.copyOf(segmentationByKey.values()));
        }
        SegmentReader segmentReader = hierarchy.get().getSegmentReader();
        Segment dc1 = segmentReader.read(DC_SEGMENT_1);
        Segment dc2 = segmentReader.read(DC_SEGMENT_2);
        updateHierarchy();

        prepareCampaignResources();

        final Set<Segment> segmentSet1 = Set.of(dc1);
        final Set<Segment> segmentSet2 = Set.of(dc2);
        final BigOrder bigOrder1 = bigOrders.get(0);
        final BigOrder bigOrder2 = bigOrders.get(1);
        List<QuotaChangeRequest.Change> changes = List.of(
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(cpu)
                        .amount(100_000)
                        .segments(segmentSet1)
                        .amountReady(100_000)
                        .order(bigOrder(bigOrder1))
                        .build(),
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(cpu)
                        .amount(100_000)
                        .segments(segmentSet1)
                        .amountReady(50_000)
                        .order(bigOrder(bigOrder2))
                        .build(),
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(hdd)
                        .amount(100_000)
                        .segments(segmentSet1)
                        .amountReady(50_000)
                        .order(bigOrder(bigOrder1))
                        .build(),
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(ssd)
                        .amount(100_000)
                        .segments(segmentSet1)
                        .amountReady(70_000)
                        .order(bigOrder(bigOrder1))
                        .build(),

                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(cpu)
                        .amount(100_000)
                        .segments(segmentSet2)
                        .amountReady(100_000)
                        .order(bigOrder(bigOrder2))
                        .build()
        );
        QuotaChangeRequest request = createRequest(changes);

        final UUID extCpu = UUID.randomUUID();
        final UUID extCpu2 = UUID.randomUUID();
        final UUID extHdd = UUID.randomUUID();
        final UUID extSsd = UUID.randomUUID();
        final UUID extIoSsd = UUID.randomUUID();
        final UUID extIoHdd = UUID.randomUUID();

        createMapping(cpu, segmentSet1, 1, 1000, extCpu, "cores");
        createMapping(cpu, segmentSet2, 1, 1000, extCpu2, "cores");
        createMapping(hdd, segmentSet1, 1, 1, extHdd, "bytes");
        createMapping(ssd, segmentSet1, 1, 1, extSsd, "bytes");

        createMapping(ioHdd, segmentSet1, 1, 1, extIoHdd, "bps");
        createMapping(ioSsd, segmentSet1, 1, 1, extIoSsd, "bps");

        ResourcesModelMapper mapper = mapperManager.getMapperForProvider(service).get();
        assertTrue(mapper instanceof YpResourceModelMapper);

        QuotaRequestDelivery result = mapper.mapNonAllocatingToExternalResources(request, request.getChanges(),
                        service, author)
                .getQuotaRequestDelivery();

        Map<UUID, Map<Long, ExternalResource>> externalResources = new HashMap<>();
        Map<Pair<Long, Set<Long>>, Map<Long, InternalResource>> internalResources
                = new HashMap<>();
        result.getExternalResources().forEach(r -> externalResources
                .computeIfAbsent(r.getResourceId(), k -> new HashMap<>()).put(r.getBigOrderId(), r));
        result.getInternalResources().forEach(r -> internalResources
                .computeIfAbsent(Pair.of(r.getResourceId(), r.getSegmentIds()), k -> new HashMap<>())
                .put(r.getBigOrderId(), r));

        assertEquals(6, externalResources.size());
        assertEquals(6, internalResources.size());

        long bpsPerCoreIoSsd = 5 * 1024 * 1024;
        long bpsPerCoreIoHdd = 2 * 1024 * 1024;

        final Set<Long> ss1Ids = CollectionUtils.ids(segmentSet1);
        final Set<Long> ss2Ids = CollectionUtils.ids(segmentSet2);
        assertEquals(100_000, internalResources.get(Pair.of(cpu.getId(), ss1Ids)).get(bigOrder1.getId()).getAmount());
        assertEquals(50_000L, internalResources.get(Pair.of(cpu.getId(), ss1Ids)).get(bigOrder2.getId()).getAmount());
        assertEquals(100_000L, internalResources.get(Pair.of(cpu.getId(), ss2Ids)).get(bigOrder2.getId()).getAmount());
        assertEquals(50_000L, internalResources.get(Pair.of(hdd.getId(), ss1Ids)).get(bigOrder1.getId()).getAmount());
        assertEquals(70_000L, internalResources.get(Pair.of(ssd.getId(), ss1Ids)).get(bigOrder1.getId()).getAmount());
        assertEquals(100 * bpsPerCoreIoSsd, internalResources.get(Pair.of(ioSsd.getId(), ss1Ids)).get(bigOrder1.getId()).getAmount());
        assertEquals(50 * bpsPerCoreIoSsd, internalResources.get(Pair.of(ioSsd.getId(), ss1Ids)).get(bigOrder2.getId()).getAmount());
        assertEquals(100 * bpsPerCoreIoHdd, internalResources.get(Pair.of(ioHdd.getId(), ss1Ids)).get(bigOrder1.getId()).getAmount());
        assertEquals(50 * bpsPerCoreIoHdd, internalResources.get(Pair.of(ioHdd.getId(), ss1Ids)).get(bigOrder2.getId()).getAmount());

        assertEquals(100L, externalResources.get(extCpu).get(bigOrder1.getId()).getAmount());
        assertEquals(50L, externalResources.get(extCpu).get(bigOrder2.getId()).getAmount());
        assertEquals(100L, externalResources.get(extCpu2).get(bigOrder2.getId()).getAmount());
        assertEquals(50_000L, externalResources.get(extHdd).get(bigOrder1.getId()).getAmount());
        assertEquals(70_000L, externalResources.get(extSsd).get(bigOrder1.getId()).getAmount());
        assertEquals(100 * bpsPerCoreIoSsd, externalResources.get(extIoSsd).get(bigOrder1.getId()).getAmount());
        assertEquals(50 * bpsPerCoreIoSsd, externalResources.get(extIoSsd).get(bigOrder2.getId()).getAmount());
        assertEquals(100 * bpsPerCoreIoHdd, externalResources.get(extIoHdd).get(bigOrder1.getId()).getAmount());
        assertEquals(50 * bpsPerCoreIoHdd, externalResources.get(extIoHdd).get(bigOrder2.getId()).getAmount());
    }

    @Test
    public void ydbMapperShouldWorkCorrectly() {
        Person author = personDao.readPersonByLogin(QDEEE.getLogin());
        Service service = Service.withKey("ydb")
                .withName("YDB")
                .withAbcServiceId(YDB_ABC_SERVICE_ID)
                .withPriority(42)
                .withSettings(new Service.Settings.Builder()
                        .resourcesMappingBeanName("ydbResourceModelMapper")
                        .build())
                .build();
        service = serviceDao.create(service);
        updateHierarchy();
        final Resource userpoolCores = createResource("userpool_cores", service, DiResourceType.PROCESSOR);
        final Resource dataSize = createResource("data_size", service, DiResourceType.STORAGE);
        final Resource userpoolCoresRu = createResource("ydb_ru-userpool_cores", service, DiResourceType.PROCESSOR);
        final Resource rps = createResource("ydb_ru-rps", service, DiResourceType.ENUMERABLE);
        updateHierarchy();

        final Map<Segmentation.Key, Segmentation> segmentationByKey =
                Hierarchy.get().getSegmentationReader().readAll(List.of(
                        new Segmentation.Key(YDB_CLUSTER_SEGMENTATION)
                ));
        for (Resource resource : List.of(userpoolCores, dataSize)) {
            resourceSegmentationDao.setSegmentations(resource, Set.copyOf(segmentationByKey.values()));
        }
        SegmentReader segmentReader = hierarchy.get().getSegmentReader();
        Segment ydbRu = segmentReader.read("ydb-ru");
        Segment ydbEu = segmentReader.read("ydb-eu");
        Segment ydbRuPrestable = segmentReader.read("ydb-ru-prestable");
        updateHierarchy();

        prepareCampaignResources();

        final Set<Segment> segmentSet1 = Set.of(ydbRu);
        final Set<Segment> segmentSet2 = Set.of(ydbEu);
        final Set<Segment> segmentSet3 = Set.of(ydbRuPrestable);
        final BigOrder bigOrder1 = bigOrders.get(0);
        final BigOrder bigOrder2 = bigOrders.get(1);
        List<QuotaChangeRequest.Change> changes = List.of(
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(userpoolCores)
                        .amount(100_000)
                        .segments(segmentSet1)
                        .amountReady(100_000)
                        .order(bigOrder(bigOrder1))
                        .build(),
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(userpoolCores)
                        .amount(100_000)
                        .segments(segmentSet1)
                        .amountReady(50_000)
                        .order(bigOrder(bigOrder2))
                        .build(),
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(dataSize)
                        .amount(99 * BotResourceType.RAM_BYTES_IN_GB)
                        .segments(segmentSet1)
                        .amountReady(99 * BotResourceType.RAM_BYTES_IN_GB)
                        .order(bigOrder(bigOrder1))
                        .build(),
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(dataSize)
                        .amount(101 * BotResourceType.RAM_BYTES_IN_GB)
                        .segments(segmentSet2)
                        .amountReady(101 * BotResourceType.RAM_BYTES_IN_GB)
                        .order(bigOrder(bigOrder2))
                        .build()
        );
        QuotaChangeRequest request = createRequest(changes);

        final UUID extCores = UUID.randomUUID();
        final UUID extMemory = UUID.randomUUID();
        final UUID extGroup = UUID.randomUUID();
        final UUID extHost = UUID.randomUUID();

        Stream.of(segmentSet1, segmentSet2)
                .forEach(ss -> {
                    createMapping(userpoolCores, ss, 25, 10000, extCores, "cores");
                    createMapping(userpoolCores, ss, 5 * 5 * BotResourceType.RAM_BYTES_IN_GB, 2 * 1000, extMemory,
                            "bytes");
                    createMapping(userpoolCores, ss, 1, 1000 * 2 * 2, extHost, "items", YdbResourceModelMapper.HOST_COUNT_KEY);
                    createMapping(dataSize, ss, 1, BotResourceType.RAM_BYTES_IN_GB * 100, extGroup, "items", YdbResourceModelMapper.GROUP_COUNT_KEY);
                });
        createMapping(userpoolCoresRu, Set.of(), 25, 10000, extCores, "cores");
        createMapping(userpoolCoresRu, Set.of(), 5 * 5 * BotResourceType.RAM_BYTES_IN_GB, 2 * 1000, extMemory,
                "bytes");
        createMapping(userpoolCoresRu, Set.of(), 1, 1000 * 2 * 2, extHost, "items", YdbResourceModelMapper.HOST_COUNT_KEY);

        ResourcesModelMapper mapper = mapperManager.getMapperForProvider(service).get();
        assertTrue(mapper instanceof YdbResourceModelMapper);

        QuotaRequestDelivery result = mapper.mapNonAllocatingToExternalResources(request, request.getChanges(),
                        service, author)
                .getQuotaRequestDelivery();

        Map<UUID, Map<Long, ExternalResource>> externalResources = new HashMap<>();
        Map<Pair<Long, Set<Long>>, Map<Long, InternalResource>> internalResources
                = new HashMap<>();
        for (ExternalResource r : result.getExternalResources()) {
            externalResources.computeIfAbsent(r.getResourceId(), k -> new HashMap<>()).put(r.getBigOrderId(), r);
        }
        for (InternalResource r : result.getInternalResources()) {
            internalResources
                    .computeIfAbsent(Pair.of(r.getResourceId(), r.getSegmentIds()), k -> new HashMap<>())
                    .put(r.getBigOrderId(), r);
        }

        assertEquals(4, externalResources.size());

        final Set<Long> ss1Ids = CollectionUtils.ids(segmentSet1);
        final Set<Long> ss2Ids = CollectionUtils.ids(segmentSet2);
        final Set<Long> ss3Ids = CollectionUtils.ids(segmentSet3);
        assertEquals(100_000,
                internalResources.get(Pair.of(userpoolCores.getId(), ss1Ids)).get(bigOrder1.getId()).getAmount());
        assertEquals(50_000L,
                internalResources.get(Pair.of(userpoolCores.getId(), ss1Ids)).get(bigOrder2.getId()).getAmount());
        assertEquals(99 * BotResourceType.RAM_BYTES_IN_GB,
                internalResources.get(Pair.of(dataSize.getId(), ss1Ids)).get(bigOrder1.getId()).getAmount());
        assertEquals(101 * BotResourceType.RAM_BYTES_IN_GB,
                internalResources.get(Pair.of(dataSize.getId(), ss2Ids)).get(bigOrder2.getId()).getAmount());

        assertEquals(250L, externalResources.get(extCores).get(bigOrder1.getId()).getAmount());
        assertEquals(5 * 2.5 * 100 * BotResourceType.RAM_BYTES_IN_GB, externalResources.get(extMemory).get(bigOrder1.getId()).getAmount());
        assertEquals(25L, externalResources.get(extHost).get(bigOrder1.getId()).getAmount());
        assertEquals(1L, externalResources.get(extGroup).get(bigOrder1.getId()).getAmount());

        changes = List.of(
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(rps)
                        .amount(499_000L)
                        .segments(Set.of())
                        .amountReady(499_000L)
                        .order(bigOrder(bigOrder1))
                        .build(),
                QuotaChangeRequest.Change.newChangeBuilder()
                        .resource(rps)
                        .amount(501_000L)
                        .segments(Set.of())
                        .amountReady(501_000L)
                        .order(bigOrder(bigOrder2))
                        .build()
        );
        request = createRequest(changes);

        result = mapper.mapNonAllocatingToExternalResources(request, request.getChanges(),
                        service, author)
                .getQuotaRequestDelivery();

        externalResources = new HashMap<>();
        internalResources = new HashMap<>();
        for (ExternalResource r : result.getExternalResources()) {
            externalResources.computeIfAbsent(r.getResourceId(), k -> new HashMap<>()).put(r.getBigOrderId(), r);
        }
        for (InternalResource r : result.getInternalResources()) {
            internalResources
                    .computeIfAbsent(Pair.of(r.getResourceId(), r.getSegmentIds()), k -> new HashMap<>())
                    .put(r.getBigOrderId(), r);
        }
        assertEquals(3, externalResources.size());

        assertEquals(1_000L,
                internalResources.get(Pair.of(userpoolCoresRu.getId(), Set.of())).get(bigOrder1.getId()).getAmount());
        assertEquals(2_000L,
                internalResources.get(Pair.of(userpoolCoresRu.getId(), Set.of())).get(bigOrder2.getId()).getAmount());

        assertEquals(3L, externalResources.get(extCores).get(bigOrder1.getId()).getAmount());
        assertEquals(5 * 2.5 * BotResourceType.RAM_BYTES_IN_GB, externalResources.get(extMemory).get(bigOrder1.getId()).getAmount());
        assertEquals(1L, externalResources.get(extHost).get(bigOrder1.getId()).getAmount());

        assertEquals(5L, externalResources.get(extCores).get(bigOrder2.getId()).getAmount());
        assertEquals(5 * 5 * BotResourceType.RAM_BYTES_IN_GB, externalResources.get(extMemory).get(bigOrder2.getId()).getAmount());
        assertEquals(1L, externalResources.get(extHost).get(bigOrder2.getId()).getAmount());
    }

    private Resource createResource(String key, Service service, DiResourceType type) {
        return resourceDao.create(new Resource.Builder(key, service)
                .name(key)
                .description(key)
                .noGroup()
                .priority(42)
                .type(type)
                .build());
    }

    private ResourcesModelMapping createMapping(Resource resource,
                                                Set<Segment> segments,
                                                long numerator,
                                                long denominator,
                                                UUID externalResourceId,
                                                String baseUnitKey) {
        return createMapping(resource, segments, numerator, denominator, externalResourceId, baseUnitKey, null);
    }

    private ResourcesModelMapping createMapping(Resource resource,
                                                Set<Segment> segments,
                                                long numerator,
                                                long denominator,
                                                UUID externalResourceId,
                                                String baseUnitKey,
                                                String key) {
        return resourcesModelMappingDao.createResourceMapping(ResourcesModelMapping.builder()
                .resource(resource)
                .addSegments(segments)
                .campaignId(campaign.getId())
                .target(ResourceModelMappingTarget.builder()
                        .numerator(numerator)
                        .denominator(denominator)
                        .externalResourceId(externalResourceId)
                        .externalResourceBaseUnitKey(baseUnitKey)
                        .key(key)
                        .build()));
    }


    @NotNull
    private QuotaChangeRequest createRequest(List<QuotaChangeRequest.Change> changes) {
        return new QuotaChangeRequest.Builder()
                .changes(changes)
                .project(Project.withKey("test").name("Test").parent(projectDao.read(YANDEX)).abcServiceId(42).build())
                .author(personDao.readPersonByLogin(AMOSOV_F.getLogin()))
                .campaign(QuotaChangeRequest.Campaign.from(campaign))
                .campaignType(campaign.getType())
                .description("Default")
                .summary("Test")
                .comment("Default")
                .calculations("Default")
                .created(0)
                .updated(0)
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                .chartLinks(Collections.emptyList())
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                .cost(0)
                .requestOwningCost(0L)
                .build();
    }

    private Campaign createCampaign(final List<Long> bigOrders) {
        return campaignDao.create(defaultCampaignBuilder(Objects.requireNonNull(bigOrderManager.getById(bigOrders.get(0))))
                .setBigOrders(bigOrders.stream().map(i -> new Campaign.BigOrder(i, LocalDate.now()))
                        .collect(Collectors.toList()))
                .build());
    }

    private QuotaChangeRequest.BigOrder bigOrder(BigOrder bigOrder) {
        return new QuotaChangeRequest.BigOrder(bigOrder.getId(), bigOrder.getDate(), true);
    }

}
