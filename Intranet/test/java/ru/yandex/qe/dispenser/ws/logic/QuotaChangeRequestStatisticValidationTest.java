package ru.yandex.qe.dispenser.ws.logic;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeStatistic;
import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.quota.Body;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.QuotaSpec;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.segmentation.SegmentationDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.ws.QuotaChangeRequestStatisticService;
import ru.yandex.qe.dispenser.ws.bot.Provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class QuotaChangeRequestStatisticValidationTest extends BaseQuotaRequestTest {

    @Autowired
    private QuotaChangeRequestDao quotaChangeRequestDao;

    @Autowired
    private CampaignDao campaignDao;
    @Autowired
    private QuotaChangeRequestStatisticService quotaChangeRequestStatisticService;
    @Autowired
    private SegmentationDao segmentationDao;
    @Autowired
    private ResourceSegmentationDao resourceSegmentationDao;
    @Autowired
    private QuotaChangeRequestDao requestDao;
    @Autowired
    private PersonDao personDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;
    @Autowired
    private SegmentDao segmentDao;
    @Value("${dispenser.invisible_separator}")
    private String invisibleSeparator;

    private Campaign campaign;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne)
                .setKey("another-test-campaign")
                        .setName("Another test campaign")
                        .setStartDate(LocalDate.now())
                .setBigOrders(Lists.newArrayList(
                        new Campaign.BigOrder(bigOrderOne.getId(), TEST_BIG_ORDER_DATE),
                        new Campaign.BigOrder(bigOrderTwo.getId(), TEST_BIG_ORDER_DATE_2)
                )).build());
        final DiQuotaChangeStatistic emptyStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .perform();

        createProject("p1", YANDEX, BINARY_CAT.getLogin());
        createProject("p2", YANDEX, BINARY_CAT.getLogin());
        createProject("p3", YANDEX, BINARY_CAT.getLogin());
        final Project defaultProject = projectDao.read(DEFAULT);
        projectDao.update(Project.copyOf(defaultProject)
                .abcServiceId(988)
                .build());
        updateHierarchy();

        final Service nirvana = serviceDao.getAll().stream().filter(service -> service.getName().equals("Nirvana")).findFirst().orElse(null);
        final Resource cpu = resourceDao.create(new Resource.Builder(CPU, nirvana)
                .name("CPU")
                .type(DiResourceType.PROCESSOR)
                .mode(DiQuotingMode.ENTITIES_ONLY)
                .build());

        updateHierarchy();

        quotaSpecDao.create(new QuotaSpec.Builder(CPU, cpu).description("CPU quota").build());

        prepareCampaignResources();

        updateHierarchy();

        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .projectKey(DEFAULT)
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.PERMILLE))
                        .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.PERMILLE))
                        .changes(NIRVANA, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(8, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .summary("Test")
                        .build(), null)
                .performBy(AMOSOV_F);

        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .projectKey("p1")
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(NIRVANA, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(30, DiUnit.BYTE))
                        .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(35, DiUnit.PERMILLE))
                        .changes(NIRVANA, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(31, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .summary("Test")
                        .build(), null)
                .performBy(BINARY_CAT);

        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .projectKey("p3")
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(NIRVANA, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(8, DiUnit.BYTE))
                        .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(49, DiUnit.PERMILLE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .summary("Test")
                        .build(), null)
                .performBy(BINARY_CAT);


        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .projectKey("p2")
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), Sets.newHashSet(SEGMENT_SEGMENT_1, DC_SEGMENT_1), DiAmount.of(40, DiUnit.PERMILLE_CORES))
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), Sets.newHashSet(SEGMENT_SEGMENT_1, DC_SEGMENT_2), DiAmount.of(50, DiUnit.PERMILLE_CORES))
                        .changes(YP, SEGMENT_HDD, bigOrderOne.getId(), Sets.newHashSet(SEGMENT_SEGMENT_1, DC_SEGMENT_3), DiAmount.of(60, DiUnit.BYTE))
                        .changes(YP, SEGMENT_HDD, bigOrderOne.getId(), Sets.newHashSet(SEGMENT_SEGMENT_1, DC_SEGMENT_2), DiAmount.of(65, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .summary("Test")
                        .build(), null)
                .performBy(BINARY_CAT);

        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .projectKey("p1")
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(70, DiUnit.PERMILLE_CORES))
                        .changes(MDB, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(80, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .summary("Test")
                        .build(), null)
                .performBy(BINARY_CAT);

        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .projectKey("p1")
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(MDB, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(29, DiUnit.PERMILLE_CORES))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .summary("Test")
                        .build(), null)
                .performBy(BINARY_CAT);

        updateHierarchy();
    }

    @Test
    public void quotaChangeRequestStatsCanBeFetched() {

        final DiQuotaChangeStatistic allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .perform();

        final DiListResponse<DiQuotaChangeRequest> requests = dispenser()
                .quotaChangeRequests()
                .get()
                .perform();

        assertEquals(6, requests.size());

        final Map<String, DiQuotaChangeStatistic.Group> groupByTitle = allStat.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Group::getTitle, Function.identity()));

        assertEquals(5, groupByTitle.size());

        final ImmutableMap<Object, Object> expectedSummary = ImmutableMap.builder()
                .put("Черновиков", "6")
                .put("Отменено", "0")
                .put("Отклонено", "0")
                .put("Подтверждено", "0")
                .put("Готовы к защите", "0")
                .put("Квота выдана", "0")
                .put("Нужна информация", "0")
                .put("Одобрено", "0")
                .build();

        final DiQuotaChangeStatistic.Group totalByCampaign = groupByTitle.get("кампании");
        final Map<String, String> totalByCampaignSummary = totalByCampaign.getSummary();
        assertEquals(totalByCampaignSummary, expectedSummary);
        assertEquals(1, totalByCampaign.getItems().size());

        final DiQuotaChangeStatistic.Group totalByDeliveryDate = groupByTitle.get("даты поставки");
        final Map<String, String> totalByDeliveryDateSummary = totalByDeliveryDate.getSummary();
        assertEquals(totalByDeliveryDateSummary, expectedSummary);
        assertEquals(1, totalByDeliveryDate.getItems().size());

        final DiQuotaChangeStatistic.Group totalAnotherSegmentation = groupByTitle.get("Another segmentations");
        final Map<String, String> totalAnotherSegmentationSummary = totalAnotherSegmentation.getSummary();
        assertEquals(totalAnotherSegmentationSummary, expectedSummary);
        final List<DiQuotaChangeStatistic.Section> anotherSegmentationItems = totalAnotherSegmentation.getItems();
        assertEquals(2, anotherSegmentationItems.size());

        final Map<String, DiQuotaChangeStatistic.Section> anotherSegmentationSectionMap = anotherSegmentationItems.stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Section::getTitle, Function.identity()));

        final DiQuotaChangeStatistic.Section segment1 = anotherSegmentationSectionMap.get("Segment1");
        final List<DiQuotaChangeStatistic.Item> anotherSegmentationSegmentItems = segment1.getItems();
        assertEquals(1, anotherSegmentationSegmentItems.size());

        final DiQuotaChangeStatistic.Item segment1Item = anotherSegmentationSegmentItems.get(0);
        assertEquals("", segment1Item.getTitle());

        final List<DiQuotaChangeStatistic.Resource> anotherSegmentationResources = segment1Item.getResources();
        assertEquals(2, anotherSegmentationResources.size());

        final Map<String, DiQuotaChangeStatistic.Resource> anotherSegmentationResourceMap = anotherSegmentationResources.stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Resource::getName, Function.identity()));

        final DiQuotaChangeStatistic.Resource segment_storage = anotherSegmentationResourceMap.get("Segment Storage");
        assertEquals(segment_storage.getAmount(), DiAmount.of(125, DiUnit.BYTE));
        final DiQuotaChangeStatistic.Resource.ResourceKey segment_storageKey = segment_storage.getKey();
        assertEquals("segment-hdd", segment_storageKey.getResourceKey());
        assertEquals("yp", segment_storageKey.getServiceKey());

        final DiQuotaChangeStatistic.Resource segment_cpu = anotherSegmentationResourceMap.get("Segment CPU");
        assertEquals(segment_cpu.getAmount(), DiAmount.of(90, DiUnit.PERMILLE_CORES));
        final DiQuotaChangeStatistic.Resource.ResourceKey segment_cpuKey = segment_cpu.getKey();
        assertEquals("segment-cpu", segment_cpuKey.getResourceKey());
        assertEquals("yp", segment_cpuKey.getServiceKey());


        final DiQuotaChangeStatistic.Group totalByDC = groupByTitle.get("датацентры");
        final Map<String, String> totalSummary = totalByDC.getSummary();
        assertEquals(totalSummary, expectedSummary);

        assertEquals(4, totalByDC.getItems().size());

        final Map<String, DiQuotaChangeStatistic.Section> locationMap = totalByDC.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Section::getTitle, Function.identity()));

        final DiQuotaChangeStatistic.Section withoutLocation = locationMap.get("Без датацентра");
        assertEquals(2, withoutLocation.getItems().size());

        final Map<String, DiAmount> totalResourceAmountWithoutLocationNirvanaByKey = withoutLocation.getItems().stream()
                .filter(item -> item.getTitle().equals("Nirvana"))
                .flatMap(item -> item.getResources().stream())
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(4, totalResourceAmountWithoutLocationNirvanaByKey.size());

        assertEquals(10, DiUnit.PERMILLE.convert(totalResourceAmountWithoutLocationNirvanaByKey.get(YT_CPU)));
        assertEquals(104, DiUnit.PERMILLE.convert(totalResourceAmountWithoutLocationNirvanaByKey.get(YT_GPU)));
        assertEquals(46, DiUnit.BYTE.convert(totalResourceAmountWithoutLocationNirvanaByKey.get(STORAGE)));
        assertEquals(31, DiUnit.PERMILLE_CORES.convert(totalResourceAmountWithoutLocationNirvanaByKey.get(CPU)));

        final Map<String, DiAmount> totalResourceAmountWithoutLocationMdbByKey = withoutLocation.getItems().stream()
                .filter(item -> item.getTitle().equals("MDB"))
                .flatMap(item -> item.getResources().stream())
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(2, totalResourceAmountWithoutLocationMdbByKey.size());

        assertEquals(99, DiUnit.PERMILLE_CORES.convert(totalResourceAmountWithoutLocationMdbByKey.get(CPU)));
        assertEquals(80, DiUnit.BYTE.convert(totalResourceAmountWithoutLocationMdbByKey.get(RAM)));

        final DiQuotaChangeStatistic.Section location1 = locationMap.get("LOCATION_1");
        assertEquals(1, location1.getItems().size());
        final DiQuotaChangeStatistic.Item location1Yp = location1.getItems().get(0);
        assertEquals("YP", location1Yp.getTitle());
        assertEquals(1, location1Yp.getResources().size());
        final DiQuotaChangeStatistic.Resource location1Resource = location1Yp.getResources().get(0);
        assertEquals("Segment CPU", location1Resource.getName());
        assertEquals("yp", location1Resource.getKey().getServiceKey());
        assertEquals("segment-cpu", location1Resource.getKey().getResourceKey());
        assertEquals(location1Resource.getAmount(), DiAmount.of(40, DiUnit.PERMILLE_CORES));

        final DiQuotaChangeStatistic.Section location2 = locationMap.get("LOCATION_2");
        assertEquals(1, location2.getItems().size());
        final DiQuotaChangeStatistic.Item location2Yp = location2.getItems().get(0);
        assertEquals("YP", location2Yp.getTitle());
        assertEquals(2, location2Yp.getResources().size());
        final Map<String, DiQuotaChangeStatistic.Resource> location2ResourceMap = location2Yp.getResources().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Resource::getName, Function.identity()));

        final DiQuotaChangeStatistic.Resource location2SegmentStorage = location2ResourceMap.get("Segment Storage");
        assertEquals("yp", location2SegmentStorage.getKey().getServiceKey());
        assertEquals("segment-hdd", location2SegmentStorage.getKey().getResourceKey());
        assertEquals(location2SegmentStorage.getAmount(), DiAmount.of(65, DiUnit.BYTE));

        final DiQuotaChangeStatistic.Resource location2SegmentCPU = location2ResourceMap.get("Segment CPU");
        assertEquals("yp", location2SegmentCPU.getKey().getServiceKey());
        assertEquals("segment-cpu", location2SegmentCPU.getKey().getResourceKey());
        assertEquals(location2SegmentCPU.getAmount(), DiAmount.of(50, DiUnit.PERMILLE_CORES));

        final DiQuotaChangeStatistic.Section location3 = locationMap.get("LOCATION_3");
        assertEquals(1, location3.getItems().size());
        final DiQuotaChangeStatistic.Item location3Yp = location3.getItems().get(0);
        assertEquals("YP", location3Yp.getTitle());
        assertEquals(1, location3Yp.getResources().size());
        final DiQuotaChangeStatistic.Resource location3Resource = location3Yp.getResources().get(0);
        assertEquals("Segment Storage", location3Resource.getName());
        assertEquals("yp", location3Resource.getKey().getServiceKey());
        assertEquals("segment-hdd", location3Resource.getKey().getResourceKey());
        assertEquals(location3Resource.getAmount(), DiAmount.of(60, DiUnit.BYTE));

        final DiQuotaChangeStatistic.Group total = groupByTitle.get("без группировки");
        assertEquals(total.getSummary(), expectedSummary);

        final List<DiQuotaChangeStatistic.Item> items = total.getItems().get(0).getItems();
        assertEquals(3, items.size());
        assertEquals(new HashSet<>(Arrays.asList("Nirvana", "YP", "MDB")), items.stream().map(DiQuotaChangeStatistic.Item::getTitle).collect(Collectors.toSet()));

        final DiQuotaChangeStatistic.Item nirvana = items.stream().filter(e -> e.getTitle().equals("Nirvana")).findFirst().get();
        List<DiQuotaChangeStatistic.Resource> resources = new ArrayList<>(nirvana.getResources());
        assertEquals(4, resources.size());

        Map<String, DiAmount> resourcesMap = resources.stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(10, DiUnit.PERMILLE.convert(resourcesMap.get(YT_CPU)));
        assertEquals(46, DiUnit.BYTE.convert(resourcesMap.get(STORAGE)));
        assertEquals(31, DiUnit.PERMILLE_CORES.convert(resourcesMap.get(CPU)));
        assertEquals(104, DiUnit.PERMILLE.convert(resourcesMap.get(YT_GPU)));

        final DiQuotaChangeStatistic.Item yp = items.stream().filter(e -> e.getTitle().equals("YP")).findFirst().get();
        resources = new ArrayList<>(yp.getResources());
        assertEquals(2, resources.size());

        resourcesMap = resources.stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(125, DiUnit.BYTE.convert(resourcesMap.get(SEGMENT_HDD)));
        assertEquals(90, DiUnit.PERMILLE_CORES.convert(resourcesMap.get(SEGMENT_CPU)));

        final DiQuotaChangeStatistic.Item mdb = items.stream().filter(e -> e.getTitle().equals("MDB")).findFirst().get();
        resources = new ArrayList<>(mdb.getResources());
        assertEquals(2, resources.size());

        resourcesMap = resources.stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(99, DiUnit.PERMILLE_CORES.convert(resourcesMap.get(CPU)));
        assertEquals(80, DiUnit.BYTE.convert(resourcesMap.get(RAM)));
    }

    private QuotaChangeRequest createRequests(final Campaign campaign, final int count, final QuotaChangeRequest.Status status) {
        QuotaChangeRequest request = null;
        final Hierarchy hierarchy = Hierarchy.get();
        final Service service = hierarchy.getServiceReader().read(NIRVANA);
        final Resource resource = hierarchy.getResourceReader().read(new Resource.Key(YT_CPU, service));
        final Project project = hierarchy.getProjectReader().read(YANDEX);
        final QuotaChangeRequest.BigOrder bigOrder = new QuotaChangeRequest.BigOrder(bigOrderOne.getId(), LocalDate.now(), true);
        final Person person = hierarchy.getPersonReader().read(AMOSOV_F.getLogin());

        for (int i = 0; i < count; i++) {
            request = quotaChangeRequestDao.create(new QuotaChangeRequest.Builder()
                    .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                    .project(project)
                    .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                    .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                    .chartLinks(Collections.emptyList())
                    .changes(ImmutableList.of(
                            QuotaChangeRequest.Change.newChangeBuilder().resource(resource).segments(Collections.emptySet()).amount(10L).build()
                    ))
                    .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                    .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                    .status(status)
                    .author(person)
                    .created(0)
                    .updated(0)
                    .campaign(QuotaChangeRequest.Campaign.from(campaign))
                    .campaignType(campaign.getType())
                    .cost(0)
                    .requestOwningCost(0L)
                    .build());
        }

        return request;
    }

    @Test
    public void requestsShouldBeCountedInStat() {
        createRequests(campaign, 9, QuotaChangeRequest.Status.NEW);
        createRequests(campaign, 7, QuotaChangeRequest.Status.CANCELLED);
        createRequests(campaign, 5, QuotaChangeRequest.Status.REJECTED);
        createRequests(campaign, 3, QuotaChangeRequest.Status.CONFIRMED);
        createRequests(campaign, 11, QuotaChangeRequest.Status.NEED_INFO);
        createRequests(campaign, 13, QuotaChangeRequest.Status.APPROVED);

        final QuotaChangeRequest req = createRequests(campaign, 1, QuotaChangeRequest.Status.CONFIRMED);

        final QuotaChangeRequest confirmedRequest = quotaChangeRequestDao.read(req.getId());
        quotaChangeRequestDao.update(confirmedRequest.copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());
        updateHierarchy();

        final DiQuotaChangeStatistic allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("type", DiQuotaChangeRequest.Type.RESOURCE_PREORDER.name())
                .perform();

        final Map<String, DiQuotaChangeStatistic.Group> statByTitle = allStat.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Group::getTitle, Function.identity()));

        final Map<String, String> summary = statByTitle.get("без группировки").getSummary();

        assertEquals("15", summary.get("Черновиков"));
        assertEquals("7", summary.get("Отменено"));
        assertEquals("5", summary.get("Отклонено"));
        assertEquals("4", summary.get("Подтверждено"));
        assertEquals("0", summary.get("Готовы к защите"));
        assertEquals("13", summary.get("Одобрено"));
        assertEquals("11", summary.get("Нужна информация"));
    }

    @Test
    public void requestsPriceShouldNotVisibleOnlyResponsible() {
        createRequests(campaign, 3, QuotaChangeRequest.Status.NEW);

        final DiQuotaChangeStatistic allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("type", DiQuotaChangeRequest.Type.RESOURCE_PREORDER.name())
                .perform();

        Map<String, DiQuotaChangeStatistic.Group> statByTitle = allStat.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Group::getTitle, Function.identity()));

        Map<String, String> summary = statByTitle.get("без группировки").getSummary();
        assertNull(summary.get("Общая сумма"));

        Map<String, DiQuotaChangeStatistic.CountWithUnit> summaryWithUnits = statByTitle.get("без группировки").getSummaryWithUnits();
        assertNull(summaryWithUnits.get("Общая сумма"));

        final DiQuotaChangeStatistic responsibleStat = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/quota-requests/statistics")
                .query("type", DiQuotaChangeRequest.Type.RESOURCE_PREORDER.name())
                .get(DiQuotaChangeStatistic.class);

        statByTitle = responsibleStat.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Group::getTitle, Function.identity()));

        summary = statByTitle.get("без группировки").getSummary();
        assertNull(summary.get("Общая сумма"));

        summaryWithUnits = statByTitle.get("без группировки").getSummaryWithUnits();
        assertNull(summaryWithUnits.get("Общая сумма"));
    }


    @Test
    public void statisticForMultiServiceRequestShouldWork() {
        Project def = projectDao.read(DEFAULT);
        Project infra = projectDao.read(INFRA_SPECIAL);
        projectDao.update(Project.copyOf(def).abcServiceId(100).build());
        projectDao.update(Project.copyOf(infra).abcServiceId(101).build());

        updateHierarchy();

        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                        .projectKey(DEFAULT)
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.PERMILLE))
                        .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.PERMILLE))
                        .changes(NIRVANA, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(8, DiUnit.BYTE))
                        .changes(MDB, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(32, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F);


        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                        .projectKey(INFRA_SPECIAL)
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(12, DiUnit.CORES))
                        .changes(YP, SEGMENT_CPU, bigOrderTwo.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_1), DiAmount.of(17, DiUnit.CORES))
                        .changes(NIRVANA, STORAGE, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(18, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F);


        DiQuotaChangeStatistic allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("type", DiQuotaChangeRequest.Type.RESOURCE_PREORDER.name())
                .perform();

        Map<String, DiQuotaChangeStatistic.Group> groupByName = allStat.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Group::getTitle, Function.identity()));

        DiQuotaChangeStatistic.Group common = groupByName.get("без группировки");
        assertEquals("8", common.getSummary().get("Черновиков"));
        assertEquals(1, common.getItems().size());

        Map<String, DiQuotaChangeStatistic.Item> sectionsOfCommon = common.getItems().get(0).getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Item::getTitle, Function.identity()));

        Map<String, DiAmount> resourceByKey = sectionsOfCommon.get("Nirvana").getResources().stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(4, resourceByKey.size());
        assertEquals(20, DiUnit.PERMILLE.convert(resourceByKey.get(YT_CPU)));
        assertEquals(124, DiUnit.PERMILLE.convert(resourceByKey.get(YT_GPU)));
        assertEquals(72, DiUnit.BYTE.convert(resourceByKey.get(STORAGE)));

        resourceByKey = sectionsOfCommon.get("YP").getResources().stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(2, resourceByKey.size());
        assertEquals(29, DiUnit.CORES.convert(resourceByKey.get(SEGMENT_CPU)));

        resourceByKey = sectionsOfCommon.get("MDB").getResources().stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(2, resourceByKey.size());
        assertEquals(112, DiUnit.BYTE.convert(resourceByKey.get(RAM)));

        allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("type", DiQuotaChangeRequest.Type.RESOURCE_PREORDER.name())
                .query("project", SEARCH)
                .perform();

        groupByName = allStat.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Group::getTitle, Function.identity()));

        common = groupByName.get("без группировки");
        assertEquals("1", common.getSummary().get("Черновиков"));

        sectionsOfCommon = common.getItems().get(0).getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Item::getTitle, Function.identity()));

        resourceByKey = sectionsOfCommon.get("Nirvana").getResources().stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(1, resourceByKey.size());
        assertEquals(18, DiUnit.BYTE.convert(resourceByKey.get(STORAGE)));

        resourceByKey = sectionsOfCommon.get("YP").getResources().stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(1, resourceByKey.size());
        assertEquals(29, DiUnit.CORES.convert(resourceByKey.get(SEGMENT_CPU)));

        allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("type", DiQuotaChangeRequest.Type.RESOURCE_PREORDER.name())
                .query("service", MDB)
                .perform();

        groupByName = allStat.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Group::getTitle, Function.identity()));

        common = groupByName.get("без группировки");
        assertEquals("3", common.getSummary().get("Черновиков"));

        sectionsOfCommon = common.getItems().get(0).getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Item::getTitle, Function.identity()));

        assertNull(sectionsOfCommon.get("Nirvana"));

        resourceByKey = sectionsOfCommon.get("MDB").getResources().stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(2, resourceByKey.size());
        assertEquals(112, DiUnit.BYTE.convert(resourceByKey.get(RAM)));

        allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("type", DiQuotaChangeRequest.Type.RESOURCE_PREORDER.name())
                .query("service", NIRVANA, YP)
                .perform();

        groupByName = allStat.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Group::getTitle, Function.identity()));

        common = groupByName.get("без группировки");
        assertEquals("6", common.getSummary().get("Черновиков"));

        sectionsOfCommon = common.getItems().get(0).getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Item::getTitle, Function.identity()));

        resourceByKey = sectionsOfCommon.get("Nirvana").getResources().stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(4, resourceByKey.size());
        assertEquals(20, DiUnit.PERMILLE.convert(resourceByKey.get(YT_CPU)));
        assertEquals(124, DiUnit.PERMILLE.convert(resourceByKey.get(YT_GPU)));
        assertEquals(72, DiUnit.BYTE.convert(resourceByKey.get(STORAGE)));

        resourceByKey = sectionsOfCommon.get("YP").getResources().stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(2, resourceByKey.size());
        assertEquals(29, DiUnit.CORES.convert(resourceByKey.get(SEGMENT_CPU)));

        assertNull(sectionsOfCommon.get("MDB"));

        allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("type", DiQuotaChangeRequest.Type.RESOURCE_PREORDER.name())
                .query("service", YP)
                .query("order", String.valueOf(bigOrderTwo.getId()))
                .perform();

        groupByName = allStat.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Group::getTitle, Function.identity()));

        common = groupByName.get("без группировки");
        assertEquals("1", common.getSummary().get("Черновиков"));

        sectionsOfCommon = common.getItems().get(0).getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Item::getTitle, Function.identity()));

        assertNull(sectionsOfCommon.get("NIRVANA"));

        resourceByKey = sectionsOfCommon.get("YP").getResources().stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(1, resourceByKey.size());
        assertEquals(17, DiUnit.CORES.convert(resourceByKey.get(SEGMENT_CPU)));

        assertNull(sectionsOfCommon.get("MDB"));

        allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("type", DiQuotaChangeRequest.Type.RESOURCE_PREORDER.name())
                .query("order", String.valueOf(bigOrderTwo.getId()))
                .perform();

        groupByName = allStat.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Group::getTitle, Function.identity()));

        common = groupByName.get("без группировки");
        assertEquals("1", common.getSummary().get("Черновиков"));

        sectionsOfCommon = common.getItems().get(0).getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Item::getTitle, Function.identity()));

        resourceByKey = sectionsOfCommon.get("Nirvana").getResources().stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(1, resourceByKey.size());
        assertEquals(18, DiUnit.BYTE.convert(resourceByKey.get(STORAGE)));

        resourceByKey = sectionsOfCommon.get("YP").getResources().stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(1, resourceByKey.size());
        assertEquals(17, DiUnit.CORES.convert(resourceByKey.get(SEGMENT_CPU)));

        assertNull(sectionsOfCommon.get("MDB"));
    }

    @Test
    public void statisticHasOnlyRequestedStatuses() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());
        Project project2 = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT_2").name("Test Project 2").parent(yandex).abcServiceId(112).build());

        updateHierarchy();

        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                        .projectKey(project.getPublicKey())
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.PERMILLE))
                        .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.PERMILLE))
                        .changes(NIRVANA, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(8, DiUnit.BYTE))
                        .changes(MDB, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(32, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F);


        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                        .projectKey(project2.getPublicKey())
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(12, DiUnit.CORES))
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_1), DiAmount.of(17, DiUnit.CORES))
                        .changes(NIRVANA, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(18, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F);

        DiQuotaChangeStatistic allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("status", DiQuotaChangeRequest.Status.NEW.name())
                .query("type", DiQuotaChangeRequest.Type.RESOURCE_PREORDER.name())
                .perform();

        Map<String, String> summary = allStat.getItems().get(0).getSummary();

        assertEquals(1, summary.size());
        assertEquals("8", summary.get("Черновиков"));

        allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("status", DiQuotaChangeRequest.Status.NEW.name(), DiQuotaChangeRequest.Status.CONFIRMED.name())
                .query("type", DiQuotaChangeRequest.Type.RESOURCE_PREORDER.name())
                .perform();

        summary = allStat.getItems().get(0).getSummary();

        assertEquals(2, summary.size());
        assertEquals("8", summary.get("Черновиков"));
        assertEquals("0", summary.get("Подтверждено"));
    }

    @Test
    public void campaignAndDeliveryDateStatisticShouldWork() {
        Project yandex = projectDao.read(YANDEX);
        Project project = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT").name("Test Project").parent(yandex).abcServiceId(111).build());

        updateHierarchy();

        prepareCampaignResources();
        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                        .projectKey(project.getPublicKey())
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(NIRVANA, YT_CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.PERMILLE))
                        .changes(NIRVANA, YT_GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.PERMILLE))
                        .changes(NIRVANA, STORAGE, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(8, DiUnit.BYTE))
                        .changes(MDB, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(32, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F);

        Project project2 = projectDao.createIfAbsent(Project.withKey("TEST_PROJECT_2").name("Test Project 2").parent(yandex).abcServiceId(112).build());

        updateHierarchy();

        prepareCampaignResources();
        dispenser()
                .quotaChangeRequests()
                .create(new Body.BodyBuilder()
                        .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                        .projectKey(project2.getPublicKey())
                        .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                        .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                        .changes(YP, SEGMENT_CPU, bigOrderTwo.getId(), ImmutableSet.of(DC_SEGMENT_1, SEGMENT_SEGMENT_1), DiAmount.of(12, DiUnit.CORES))
                        .changes(YP, SEGMENT_CPU, bigOrderOne.getId(), ImmutableSet.of(DC_SEGMENT_2, SEGMENT_SEGMENT_1), DiAmount.of(17, DiUnit.CORES))
                        .changes(NIRVANA, STORAGE, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(18, DiUnit.BYTE))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .build(), null)
                .performBy(AMOSOV_F);

        final DiQuotaChangeStatistic allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("status", DiQuotaChangeRequest.Status.NEW.name())
                .query("type", DiQuotaChangeRequest.Type.RESOURCE_PREORDER.name())
                .perform();

        final Map<String, DiQuotaChangeStatistic.Group> statisticMap = allStat.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Group::getTitle, Function.identity()));

        final DiQuotaChangeStatistic.Group byCampaign = statisticMap.get("кампании");
        final List<DiQuotaChangeStatistic.Section> campaignItems = byCampaign.getItems();
        assertEquals(1, campaignItems.size());

        Map<String, DiQuotaChangeStatistic.Section> sectionMap = campaignItems.stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Section::getTitle, Function.identity()));

        DiQuotaChangeStatistic.Section section = sectionMap.get("Another test campaign");
        assertEquals(3, section.getItems().size());

        Map<String, DiQuotaChangeStatistic.Item> itemMap = section.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Item::getTitle, Function.identity()));

        List<DiQuotaChangeStatistic.Resource> resources = itemMap.get("Nirvana").getResources();
        assertEquals(4, resources.size());

        Map<String, DiAmount> resourcesToAmount = resources.stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(resourcesToAmount.get(YT_CPU), DiAmount.of(20, DiUnit.PERMILLE));
        assertEquals(resourcesToAmount.get(YT_GPU), DiAmount.of(124, DiUnit.PERMILLE));
        assertEquals(resourcesToAmount.get(STORAGE), DiAmount.of(72, DiUnit.BYTE));

        resources = itemMap.get("MDB").getResources();
        assertEquals(2, resources.size());

        resourcesToAmount = resources.stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(resourcesToAmount.get(RAM), DiAmount.of(112, DiUnit.BYTE));

        resources = itemMap.get("YP").getResources();
        assertEquals(2, resources.size());

        resourcesToAmount = resources.stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(resourcesToAmount.get(SEGMENT_CPU), DiAmount.of(29090, DiUnit.PERMILLE_CORES));


        final DiQuotaChangeStatistic.Group byDeliveryDate = statisticMap.get("даты поставки");
        assertEquals(2, byDeliveryDate.getItems().size());

        sectionMap = byDeliveryDate.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Section::getTitle, Function.identity()));

        assertEquals(2, sectionMap.size());

        section = sectionMap.get(TEST_BIG_ORDER_DATE.toString());
        assertEquals(3, section.getItems().size());

        itemMap = section.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Item::getTitle, Function.identity()));

        resources = itemMap.get("Nirvana").getResources();
        assertEquals(4, resources.size());

        resourcesToAmount = resources.stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(resourcesToAmount.get(YT_CPU), DiAmount.of(20, DiUnit.PERMILLE));
        assertEquals(resourcesToAmount.get(YT_GPU), DiAmount.of(124, DiUnit.PERMILLE));
        assertEquals(resourcesToAmount.get(STORAGE), DiAmount.of(54, DiUnit.BYTE));

        resources = itemMap.get("YP").getResources();
        assertEquals(2, resources.size());

        resourcesToAmount = resources.stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(resourcesToAmount.get(SEGMENT_CPU), DiAmount.of(17090, DiUnit.PERMILLE_CORES));

        resources = itemMap.get("MDB").getResources();
        assertEquals(2, resources.size());

        resourcesToAmount = resources.stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(resourcesToAmount.get(RAM), DiAmount.of(112, DiUnit.BYTE));


        section = sectionMap.get(bigOrderTwo.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        assertEquals(2, section.getItems().size());

        itemMap = section.getItems().stream()
                .collect(Collectors.toMap(DiQuotaChangeStatistic.Item::getTitle, Function.identity()));

        resources = itemMap.get("Nirvana").getResources();
        assertEquals(1, resources.size());

        resourcesToAmount = resources.stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(resourcesToAmount.get(STORAGE), DiAmount.of(18, DiUnit.BYTE));

        resources = itemMap.get("YP").getResources();
        assertEquals(1, resources.size());

        resourcesToAmount = resources.stream()
                .collect(Collectors.toMap(e -> e.getKey().getResourceKey(), DiQuotaChangeStatistic.Resource::getAmount));

        assertEquals(resourcesToAmount.get(SEGMENT_CPU), DiAmount.of(12000, DiUnit.PERMILLE_CORES));
    }

    @Test
    public void quotaChangeRequestStatsMustBeOrdered() {

        final DiQuotaChangeStatistic allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .perform();

        final List<DiQuotaChangeStatistic.Group> items = allStat.getItems();

        assertEquals("без группировки", items.get(0).getTitle());
        assertEquals("кампании", items.get(1).getTitle());
        assertEquals("даты поставки", items.get(2).getTitle());
        assertEquals("датацентры", items.get(3).getTitle());
        assertEquals("Another segmentations", items.get(4).getTitle());
    }

    protected long createRequest(final QuotaChangeRequest.Builder builder) {
        return requestDao.create(builder.build()).getId();
    }

    @Test
    public void allSegmentationMustHaveHumanTittleNames() {
        final String key = Provider.SAAS.getServiceKey();

        final Campaign.BigOrder bigOrder = campaign.getBigOrders().iterator().next();

        Service service;
        try {
            service = serviceDao.read(key);

            serviceDao.update(Service.copyOf(service)
                    .withSettings(Service.Settings.builder()
                            .manualQuotaAllocation(true)
                            .build())
                    .build());
        } catch (EmptyResultDataAccessException e) {
            serviceDao.create(Service.withKey(key)
                    .withName(key)
                    .withAbcServiceId(GENCFG_ABC_SERVICE_ID)
                    .withSettings(Service.Settings.builder()
                            .manualQuotaAllocation(true)
                            .build())
                    .build());
        }
        updateHierarchy();

        service = serviceDao.read(key);

        serviceDao.attachAdmin(service, personDao.read(LOTREK.getLogin()));

        final Segmentation dbaas_db = segmentationDao.create(new Segmentation.Builder("dbaas_db")
                .name("dbaas_db")
                .description("dbaas_db")
                .build());

        updateHierarchy();

        final Segment dbaas_db_one = segmentDao.create(new Segment.Builder("dbaas_db_one", dbaas_db)
                .name("ClickHouse")
                .description("ClickHouse")
                .priority((short) 1)
                .build());

        final Segmentation yt_cluster = segmentationDao.create(new Segmentation.Builder("yt_cluster")
                .name("yt_cluster")
                .description("yt_cluster")
                .build());

        updateHierarchy();

        final Segment yt_cluster_one = segmentDao.create(new Segment.Builder("yt_cluster_one", yt_cluster)
                .name("Hahn")
                .description("Hahn")
                .priority((short) 1)
                .build());

        final Segmentation logbroker = segmentationDao.create(new Segmentation.Builder("logbroker")
                .name("logbroker")
                .description("logbroker")
                .build());

        updateHierarchy();

        final Segment logbroker_one = segmentDao.create(new Segment.Builder("logbroker_one", logbroker)
                .name("Logbroker (SAS)")
                .description("Logbroker (SAS)")
                .priority((short) 1)
                .build());

        final Segmentation yp_segment = segmentationDao.create(new Segmentation.Builder("yp_segment")
                .name("yp_segment")
                .description("yp_segment")
                .build());

        updateHierarchy();

        final Segment yp_segment_one = segmentDao.create(new Segment.Builder("yp_segment_one", yp_segment)
                .name("Dev")
                .description("Dev")
                .priority((short) 1)
                .build());

        final Segmentation qloud_segment = segmentationDao.create(new Segmentation.Builder("qloud_segment")
                .name("qloud_segment")
                .description("qloud_segment")
                .build());

        updateHierarchy();

        final Segment qloud_segment_one = segmentDao.create(new Segment.Builder("qloud_segment_one", qloud_segment)
                .name("browser (qloud-ext)")
                .description("browser (qloud-ext)")
                .priority((short) 1)
                .build());

        final Segmentation sandbox_type = segmentationDao.create(new Segmentation.Builder("sandbox_type")
                .name("sandbox_type")
                .description("sandbox_type")
                .build());

        updateHierarchy();

        final Segment sandbox_type_one = segmentDao.create(new Segment.Builder("sandbox_type_one", sandbox_type)
                .name("linux (bare metal)")
                .description("linux (bare metal)")
                .priority((short) 1)
                .build());

        final Segmentation distbuild_segment = segmentationDao.create(new Segmentation.Builder("distbuild_segment")
                .name("distbuild_segment")
                .description("distbuild_segment")
                .build());

        updateHierarchy();

        final Segment distbuild_segment_one = segmentDao.create(new Segment.Builder("distbuild_user", distbuild_segment)
                .name("User")
                .description("User")
                .priority((short) 1)
                .build());

        final Segmentation random_segmentation = segmentationDao.create(new Segmentation.Builder("random_segmentation")
                .name("random_segmentation")
                .description("random_segmentation")
                .build());

        updateHierarchy();

        final Segment random_segmentation_one = segmentDao.create(new Segment.Builder("random_segmentation_one", random_segmentation)
                .name("random_segmentation_one")
                .description("random_segmentation_one")
                .priority((short) 1)
                .build());

        final Resource storage = resourceDao.create(new Resource.Builder(STORAGE, service)
                .name("Storage")
                .type(DiResourceType.STORAGE)
                .mode(DiQuotingMode.DEFAULT)
                .build());

        final Resource cpu = resourceDao.create(new Resource.Builder(CPU, service)
                .name("CPU")
                .type(DiResourceType.PROCESSOR)
                .mode(DiQuotingMode.DEFAULT)
                .build());

        final Resource ram = resourceDao.create(new Resource.Builder(RAM, service)
                .name("RAM")
                .type(DiResourceType.MEMORY)
                .mode(DiQuotingMode.DEFAULT)
                .build());

        final Resource gpu = resourceDao.create(new Resource.Builder(YT_GPU, service)
                .name("GPU")
                .type(DiResourceType.PROCESSOR)
                .mode(DiQuotingMode.DEFAULT)
                .build());

        updateHierarchy();

        resourceSegmentationDao.setSegmentations(storage, ImmutableSet.of(dbaas_db, yt_cluster));
        resourceSegmentationDao.setSegmentations(cpu, ImmutableSet.of(logbroker, yp_segment));
        resourceSegmentationDao.setSegmentations(ram, ImmutableSet.of(qloud_segment, sandbox_type));
        resourceSegmentationDao.setSegmentations(gpu, ImmutableSet.of(random_segmentation, distbuild_segment));

        updateHierarchy();

        final Person person = hierarchy.get().getPersonReader().read(AMOSOV_F.getLogin());

        final long requestId = createRequest(new QuotaChangeRequest.Builder()
                .summary("test")
                .project(hierarchy.get().getProjectReader().read(YANDEX))
                .campaign(QuotaChangeRequest.Campaign.from(campaign))
                .campaignType(campaign.getType())
                .author(person)
                .description("Default")
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
                .changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .resource(storage)
                                .order(bigOrder)
                                .segments(new HashSet<>(Arrays.asList(dbaas_db_one, yt_cluster_one)))
                                .amount(1024 * 1024).build(),
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .resource(cpu)
                                .order(bigOrder)
                                .segments(new HashSet<>(Arrays.asList(logbroker_one, yp_segment_one)))
                                .amount(100 * 1000).build(),
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .resource(ram)
                                .order(bigOrder)
                                .segments(new HashSet<>(Arrays.asList(qloud_segment_one, sandbox_type_one)))
                                .amount(1024 * 1024 * 1024).build(),
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .resource(gpu)
                                .order(bigOrder)
                                .segments(ImmutableSet.of(random_segmentation_one, distbuild_segment_one))
                                .amount(10 * 1000).build()
                        )
                ));

        updateHierarchy();

        final DiQuotaChangeStatistic allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("service", service.getKey())
                .perform();

        final List<DiQuotaChangeStatistic.Group> items = allStat.getItems();

        assertEquals("базы данных MDB", getTitle(items, 3));
        assertEquals(getFirstSegmentTitle(items, 3), "база данных ClickHouse " + invisibleSeparator + "MDB" + invisibleSeparator);
        assertEquals("Без баз данных MDB", getNoneTitle(items, 3));

        assertEquals("сегменты DistBuild", getTitle(items, 4));
        assertEquals(getFirstSegmentTitle(items, 4), "сегмент User " + invisibleSeparator + "DistBuild" + invisibleSeparator);
        assertEquals("Без сегментов DistBuild", getNoneTitle(items, 4));

        assertEquals("сегменты Logbroker", getTitle(items, 5));
        assertEquals(getFirstSegmentTitle(items, 5), "сегмент Logbroker (SAS) " + invisibleSeparator + "Logbroker" + invisibleSeparator);
        assertEquals("Без сегментов Logbroker", getNoneTitle(items, 5));

        assertEquals("сегменты Qloud", getTitle(items, 6));
        assertEquals(getFirstSegmentTitle(items, 6), "сегмент browser (qloud-ext) " + invisibleSeparator + "Qloud" + invisibleSeparator);
        assertEquals("Без сегмента Qloud", getNoneTitle(items, 6));

        assertEquals("random_segmentation", getTitle(items, 7));
        assertEquals("random_segmentation_one", getFirstSegmentTitle(items, 7));
        assertEquals("Без random_segmentation", getNoneTitle(items, 7));

        assertEquals("типы Sandbox", getTitle(items, 8));
        assertEquals(getFirstSegmentTitle(items, 8), "тип linux (bare metal) " + invisibleSeparator + "Sandbox" + invisibleSeparator);
        assertEquals("Без типов Sandbox", getNoneTitle(items, 8));

        assertEquals("сегменты YP", getTitle(items, 9));
        assertEquals(getFirstSegmentTitle(items, 9), "сегмент Dev " + invisibleSeparator + "YP" + invisibleSeparator);
        assertEquals("Без сегмента YP", getNoneTitle(items, 9));

        assertEquals("кластера YT", getTitle(items, 10));
        assertEquals(getFirstSegmentTitle(items, 10), "кластер Hahn " + invisibleSeparator + "YT" + invisibleSeparator);
        assertEquals("Без кластера YT", getNoneTitle(items, 10));
    }


    @Test
    public void customStatisticAmountTypeShouldAccepted() {
        List<DiQuotaChangeStatistic.Group> items = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("amountType", "ALLOCATED")
                .perform().getItems();

        Map<String, Long> amountByKey = items.get(0).getItems().get(0).getItems().get(0).getResources().stream().collect(Collectors.toMap(r -> r.getKey().getResourceKey(), r -> r.getAmount().getValue()));

        assertEquals(0, (long) amountByKey.get("yt-cpu"));

        quotaChangeRequestDao.clear();
        final Hierarchy hierarchy = Hierarchy.get();
        final Service service = hierarchy.getServiceReader().read(NIRVANA);
        final Resource resource = hierarchy.getResourceReader().read(new Resource.Key(YT_CPU, service));
        final Project project = hierarchy.getProjectReader().read(YANDEX);
        final QuotaChangeRequest.BigOrder bigOrder = new QuotaChangeRequest.BigOrder(bigOrderOne.getId(), LocalDate.now(), true);
        final Person person = hierarchy.getPersonReader().read(AMOSOV_F.getLogin());
        final Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());

        final QuotaChangeRequest request = quotaChangeRequestDao.create(new QuotaChangeRequest.Builder()
                .summary(QuotaChangeRequestValidationTest.TEST_SUMMARY)
                .project(project)
                .description(QuotaChangeRequestValidationTest.TEST_DESCRIPTION)
                .calculations(QuotaChangeRequestValidationTest.TEST_CALCULATIONS)
                .chartLinks(Collections.emptyList())
                .changes(ImmutableList.of(
                        QuotaChangeRequest.Change.newChangeBuilder()
                                .resource(resource)
                                .order(bigOrder)
                                .amount(100)
                                .amountAllocated(40)
                                .amountAllocating(40)
                                .amountReady(75)
                                .segments(Collections.emptySet())
                                .build()
                ))
                .type(QuotaChangeRequest.Type.RESOURCE_PREORDER)
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                .status(QuotaChangeRequest.Status.NEW)
                .author(person)
                .created(0)
                .updated(0)
                .campaign(QuotaChangeRequest.Campaign.from(campaign))
                .cost(0)
                .requestOwningCost(0L)
                .build());


        items = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("amountType", "ALLOCATED")
                .perform().getItems();

        amountByKey = items.get(0).getItems().get(0).getItems().get(0).getResources().stream().collect(Collectors.toMap(r -> r.getKey().getResourceKey(), r -> r.getAmount().getValue()));

        assertEquals(40, (long) amountByKey.get("yt-cpu"));

        items = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("amountType", "READY")
                .perform().getItems();

        amountByKey = items.get(0).getItems().get(0).getItems().get(0).getResources().stream().collect(Collectors.toMap(r -> r.getKey().getResourceKey(), r -> r.getAmount().getValue()));

        assertEquals(75, (long) amountByKey.get("yt-cpu"));

        items = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("amountType", "NOT_READY")
                .perform().getItems();

        amountByKey = items.get(0).getItems().get(0).getItems().get(0).getResources().stream().collect(Collectors.toMap(r -> r.getKey().getResourceKey(), r -> r.getAmount().getValue()));

        assertEquals(25, (long) amountByKey.get("yt-cpu"));

        items = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("amountType", "ORDERED_NOT_ALLOCATED")
                .perform().getItems();

        amountByKey = items.get(0).getItems().get(0).getItems().get(0).getResources().stream().collect(Collectors.toMap(r -> r.getKey().getResourceKey(), r -> r.getAmount().getValue()));

        assertEquals(60, (long) amountByKey.get("yt-cpu"));

        items = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .query("amountType", "READY_NOT_ALLOCATED")
                .perform().getItems();

        amountByKey = items.get(0).getItems().get(0).getItems().get(0).getResources().stream().collect(Collectors.toMap(r -> r.getKey().getResourceKey(), r -> r.getAmount().getValue()));

        assertEquals(35, (long) amountByKey.get("yt-cpu"));
    }

    @NotNull
    private String getTitle(final List<DiQuotaChangeStatistic.Group> items, final int i) {
        return items.get(i).getTitle();
    }

    @NotNull
    private String getFirstSegmentTitle(final List<DiQuotaChangeStatistic.Group> items, final int i) {
        return items.get(i).getItems().get(0).getTitle();
    }

    @NotNull
    private String getNoneTitle(final List<DiQuotaChangeStatistic.Group> items, final int i) {
        return items.get(i).getItems().get(items.get(i).getItems().size() - 1).getTitle();
    }
}
