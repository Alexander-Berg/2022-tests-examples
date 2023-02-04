package ru.yandex.qe.dispenser.ws.logic;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequestImportantFilter;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeStatistic;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignCache;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignOwningCostCache;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.ws.quota.request.owning_cost.campaignOwningCost.CampaignOwningCostRefreshManager;

import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignGroupBuilder;

public class QuotaChangeRequestOwningCostStatisticTest extends BaseQuotaRequestTest {

    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private CampaignOwningCostCache campaignOwningCostCache;
    @Autowired
    private QuotaChangeRequestDao quotaChangeRequestDao;
    @Autowired
    private CampaignOwningCostRefreshManager campaignOwningCostRefreshManager;
    @Autowired
    private CampaignCache campaignCache;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        LocalDate date = LocalDate.of(2021, Month.AUGUST, 1);
        Campaign campaign = campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2021")
                .setName("aug2021")
                .setId(143L)
                .setStartDate(date)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderTwo.getId(), date)))
                .build());
        botCampaignGroupDao.create(
                defaultCampaignGroupBuilder()
                        .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bigOrderTwo.getId())))
                        .addCampaign(campaignDao.readForBot(Collections.singleton(campaign.getId())).values().iterator().next())
                        .build()
        );
        newCampaignId = campaign.getId();
    }

    @Test
    public void testStat() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());
        String SSD = "ssd_segmented";
        String HDD = "hdd_segmented";
        String CPU = "cpu_segmented";
        String RAM = "ram_segmented";
        String GPU = "gpu_segmented";
        String IO_HDD = "io_hdd";
        String IO_SSD = "io_ssd";
        Service service = Hierarchy.get().getServiceReader().read(YP);
        resourceDao.create(new Resource.Builder(SSD, service).name(SSD).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(HDD, service).name(HDD).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());
        resourceDao.create(new Resource.Builder(RAM, service).name(RAM).type(DiResourceType.MEMORY).build());
        resourceDao.create(new Resource.Builder(GPU, service).name(GPU).type(DiResourceType.ENUMERABLE).build());
        resourceDao.create(new Resource.Builder(IO_HDD, service).name(IO_HDD).type(DiResourceType.BINARY_TRAFFIC).build());
        resourceDao.create(new Resource.Builder(IO_SSD, service).name(IO_SSD).type(DiResourceType.BINARY_TRAFFIC).build());
        updateHierarchy();
        prepareCampaignResources();
        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();
        dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(30, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(200, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();
        updateHierarchy();
        DiQuotaChangeStatistic allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .perform();
        Assertions.assertFalse(allStat.getItems().isEmpty());
        allStat.getItems().forEach(group -> {
            Assertions.assertEquals("1961895", group.getTotalOwningCost());
            Assertions.assertEquals("1961895", group.getSummaryOwningCosts().get("Черновиков"));
            group.getItems().forEach(section -> {
                section.getItems().forEach(item -> {
                    Map<String, DiQuotaChangeStatistic.Resource> byResource = item.getResources().stream()
                            .collect(Collectors.toMap(DiQuotaChangeStatistic.Resource::getName, Function.identity()));
                    Assertions.assertEquals("0", byResource.get("io_hdd").getOwningCost());
                    Assertions.assertEquals("0", byResource.get("io_ssd").getOwningCost());
                    Assertions.assertEquals("25", byResource.get("hdd_segmented").getOwningCost());
                    Assertions.assertEquals("1782330", byResource.get("gpu_segmented").getOwningCost());
                    Assertions.assertEquals("276", byResource.get("ram_segmented").getOwningCost());
                    Assertions.assertEquals("179091", byResource.get("cpu_segmented").getOwningCost());
                    Assertions.assertEquals("173", byResource.get("ssd_segmented").getOwningCost());
                });
            });
        });
    }

    @Test
    public void percentageOfCampaignOwningCostInStatisticsTest() {
        LocalDate date100 = LocalDate.of(2020, Month.AUGUST, 1);
        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2020")
                .setName("aug2020")
                .setId(100L)
                .setStartDate(date100)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderThree.getId(),
                        date100)))
                .build());

        LocalDate date142 = LocalDate.of(2021, Month.FEBRUARY, 1);
        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("feb2021")
                .setName("feb2021")
                .setId(142L)
                .setStartDate(date142)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderTwo.getId(),
                        date142)))
                .build());

        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2022aggregated")
                .setName("aug2022aggregated")
                .setId(176L)
                .setStartDate(date142)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(),
                        date142)))
                .build());

        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2022draft")
                .setName("aug2022draft")
                .setId(177L)
                .setStartDate(date142)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(),
                        date142)))
                .build());

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String SSD = "ssd_segmented";
        String HDD = "hdd_segmented";
        String CPU = "cpu_segmented";
        String RAM = "ram_segmented";
        String GPU = "gpu_segmented";
        String IO_HDD = "io_hdd";
        String IO_SSD = "io_ssd";

        Service service = Hierarchy.get().getServiceReader().read(YP);
        resourceDao.create(new Resource.Builder(SSD, service).name(SSD).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(HDD, service).name(HDD).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());
        resourceDao.create(new Resource.Builder(RAM, service).name(RAM).type(DiResourceType.MEMORY).build());
        resourceDao.create(new Resource.Builder(GPU, service).name(GPU).type(DiResourceType.ENUMERABLE).build());
        resourceDao.create(new Resource.Builder(IO_HDD, service).name(IO_HDD).type(DiResourceType.BINARY_TRAFFIC).build());
        resourceDao.create(new Resource.Builder(IO_SSD, service).name(IO_SSD).type(DiResourceType.BINARY_TRAFFIC).build());

        updateHierarchy();
        prepareCampaignResources();

        DiQuotaChangeRequest first = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest second = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest third = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1000L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(30, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(200, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        Stream.of(first, second)
                .map(DiQuotaChangeRequest::getId)
                .forEach(id -> quotaChangeRequestDao.update(quotaChangeRequestDao.read(id).copyBuilder()
                        .status(QuotaChangeRequest.Status.CONFIRMED).build()));

        Stream.of(third)
                .map(DiQuotaChangeRequest::getId)
                .forEach(id -> {
                    QuotaChangeRequest build = quotaChangeRequestDao.read(id).copyBuilder()
                            .status(QuotaChangeRequest.Status.CONFIRMED)
                            .campaign(QuotaChangeRequest.Campaign.from(campaignCache.getById(142L).orElseThrow()))
                            .campaignType(campaignCache.getById(142L).orElseThrow().getType())
                            .build();
                    quotaChangeRequestDao.delete(build);
                    quotaChangeRequestDao.create(build);
                });

        updateHierarchy();

        campaignOwningCostRefreshManager.refresh();

        updateHierarchy();

        DiQuotaChangeStatistic allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .perform();

        Assertions.assertFalse(allStat.getItems().isEmpty());
        Map<Long, String> summaryPercentageByCampaignOwningCosts =
                allStat.getItems().get(0).getSummaryPercentageByCampaignOwningCosts();
        Assertions.assertEquals(Map.of(143L, "100", 142L, "100"),
                summaryPercentageByCampaignOwningCosts);

        allStat = createAuthorizedLocalClient(WHISTLER)
                .path("/v1/quota-requests/statistics")
                .query("importantFilter", DiQuotaChangeRequestImportantFilter.IMPORTANT)
                .get(DiQuotaChangeStatistic.class);

        Assertions.assertFalse(allStat.getItems().isEmpty());
        summaryPercentageByCampaignOwningCosts = allStat.getItems().get(0).getSummaryPercentageByCampaignOwningCosts();
        Assertions.assertEquals(Map.of(143L, "50", 142L, "100"),
                summaryPercentageByCampaignOwningCosts);
    }

    @Test
    public void percentageOfCampaignOwningCostInStatisticsCalculateOnlyRequestInValidStatusTest() {
        LocalDate date100 = LocalDate.of(2020, Month.AUGUST, 1);
        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2020")
                .setName("aug2020")
                .setId(100L)
                .setStartDate(date100)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderThree.getId(),
                        date100)))
                .build());

        LocalDate date142 = LocalDate.of(2021, Month.FEBRUARY, 1);
        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("feb2021")
                .setName("feb2021")
                .setId(142L)
                .setStartDate(date142)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderTwo.getId(),
                        date142)))
                .build());

        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2022aggregated")
                .setName("aug2022aggregated")
                .setId(176L)
                .setStartDate(date142)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(),
                        date142)))
                .build());

        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2022draft")
                .setName("aug2022draft")
                .setId(177L)
                .setStartDate(date142)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(),
                        date142)))
                .build());

        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        String SSD = "ssd_segmented";
        String HDD = "hdd_segmented";
        String CPU = "cpu_segmented";
        String RAM = "ram_segmented";
        String GPU = "gpu_segmented";
        String IO_HDD = "io_hdd";
        String IO_SSD = "io_ssd";

        Service service = Hierarchy.get().getServiceReader().read(YP);
        resourceDao.create(new Resource.Builder(SSD, service).name(SSD).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(HDD, service).name(HDD).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());
        resourceDao.create(new Resource.Builder(RAM, service).name(RAM).type(DiResourceType.MEMORY).build());
        resourceDao.create(new Resource.Builder(GPU, service).name(GPU).type(DiResourceType.ENUMERABLE).build());
        resourceDao.create(new Resource.Builder(IO_HDD, service).name(IO_HDD).type(DiResourceType.BINARY_TRAFFIC).build());
        resourceDao.create(new Resource.Builder(IO_SSD, service).name(IO_SSD).type(DiResourceType.BINARY_TRAFFIC).build());

        updateHierarchy();
        prepareCampaignResources();

        DiQuotaChangeRequest first = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest second = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest third = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest four = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest five = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest six = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest seven = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest eight = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest nine = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderTwo.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        quotaChangeRequestDao.update(quotaChangeRequestDao.read(first.getId()).copyBuilder().status(QuotaChangeRequest.Status.NEW).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(second.getId()).copyBuilder().status(QuotaChangeRequest.Status.CANCELLED).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(third.getId()).copyBuilder().status(QuotaChangeRequest.Status.REJECTED).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(four.getId()).copyBuilder().status(QuotaChangeRequest.Status.CONFIRMED).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(five.getId()).copyBuilder().status(QuotaChangeRequest.Status.READY_FOR_REVIEW).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(six.getId()).copyBuilder().status(QuotaChangeRequest.Status.COMPLETED).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(seven.getId()).copyBuilder().status(QuotaChangeRequest.Status.APPLIED).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(eight.getId()).copyBuilder().status(QuotaChangeRequest.Status.NEED_INFO).build());
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(nine.getId()).copyBuilder().status(QuotaChangeRequest.Status.APPROVED).build());

        updateHierarchy();

        campaignOwningCostRefreshManager.refresh();

        updateHierarchy();

        DiQuotaChangeStatistic allStat = dispenser()
                .quotaChangeRequests()
                .getStatistic()
                .perform();

        Assertions.assertFalse(allStat.getItems().isEmpty());
        Map<Long, String> summaryPercentageByCampaignOwningCosts =
                allStat.getItems().get(0).getSummaryPercentageByCampaignOwningCosts();
        Assertions.assertEquals(Map.of(143L, "100"),
                summaryPercentageByCampaignOwningCosts);
    }
}
