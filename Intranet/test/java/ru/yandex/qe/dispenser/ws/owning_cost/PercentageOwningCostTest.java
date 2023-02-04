package ru.yandex.qe.dispenser.ws.owning_cost;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.CampaignOwningCost;
import ru.yandex.qe.dispenser.domain.QuotaChangeRequest;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignOwningCostCache;
import ru.yandex.qe.dispenser.domain.dao.quota.request.QuotaChangeRequestDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest;
import ru.yandex.qe.dispenser.ws.quota.request.owning_cost.campaignOwningCost.CampaignOwningCostRefreshManager;

import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignGroupBuilder;
import static ru.yandex.qe.dispenser.ws.quota.request.owning_cost.campaignOwningCost.CampaignOwningCostRefreshTransactionWrapper.VALID_STATUSES;
import static ru.yandex.qe.dispenser.ws.quota.request.owning_cost.formula.ProviderOwningCostFormula.MATH_CONTEXT;

/**
 * Percentage of campaign or request owning cost tests.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
public class PercentageOwningCostTest extends BaseQuotaRequestTest {
    @Autowired
    private CampaignOwningCostCache campaignOwningCostCache;

    @Autowired
    private CampaignOwningCostRefreshManager campaignOwningCostRefreshManager;

    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;

    @Autowired
    private QuotaChangeRequestDao quotaChangeRequestDao;

    @Autowired
    private ResourceDao resourceDao;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        LocalDate date100 = LocalDate.of(2020, Month.AUGUST, 1);
        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderThree)
                .setKey("aug2020")
                .setName("aug2020")
                .setId(100L)
                .setStartDate(date100)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderThree.getId(),
                        date100)))
                .build());

        LocalDate date142 = LocalDate.of(2021, Month.FEBRUARY, 1);
        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderTwo)
                .setKey("feb2021")
                .setName("feb2021")
                .setId(142L)
                .setStartDate(date142)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderTwo.getId(),
                        date142)))
                .build());

        LocalDate date = LocalDate.of(2021, Month.AUGUST, 1);
        Campaign campaign = campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2021")
                .setName("aug2021")
                .setId(143L)
                .setStartDate(date)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(),
                        date)))
                .build());

        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2022aggregated")
                .setName("aug2022aggregated")
                .setId(176L)
                .setStartDate(date)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(),
                        date)))
                .build());

        campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("aug2022draft")
                .setName("aug2022draft")
                .setId(177L)
                .setStartDate(date)
                .setBigOrders(Collections.singletonList(new Campaign.BigOrder(bigOrderOne.getId(),
                        date)))
                .build());

        botCampaignGroupDao.create(
                defaultCampaignGroupBuilder()
                        .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bigOrderOne.getId())))
                        .addCampaign(campaignDao.readForBot(Collections.singleton(campaign.getId())).values().iterator().next())
                        .build()
        );
        newCampaignId = campaign.getId();
    }

    @Test
    public void percentageOwningCostReturnedByAPITest() {
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

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        long id = quotaRequests.getFirst().getId();
        QuotaChangeRequest read = quotaChangeRequestDao.read(id);
        Map<QuotaChangeRequest.ChangeKey, BigDecimal> owningCostByChange = read.getChanges().stream()
                .collect(Collectors.toMap(QuotaChangeRequest.ChangeAmount::getKey, QuotaChangeRequest.Change::getOwningCost));
        quotaChangeRequestDao.update(read.copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());

        campaignOwningCostRefreshManager.refresh();
        Map<Long, BigInteger> campaignOwningCostMap = campaignOwningCostCache.getAll().stream()
                .collect(Collectors.toMap(CampaignOwningCost::getCampaignId, CampaignOwningCost::getOwningCost));

        DiQuotaChangeRequest firstRequest = dispenser().quotaChangeRequests().byId(id).get().perform();

        Assertions.assertTrue(campaignOwningCostMap.containsKey(Objects.requireNonNull(firstRequest.getCampaign()).getId()));
        Assertions.assertEquals(campaignOwningCostMap.get(firstRequest.getCampaign().getId()).toString(), firstRequest.getRequestOwningCost());
        Assertions.assertNotNull(firstRequest.getPercentageOfCampaignOwningCost());
        Assertions.assertEquals("100", firstRequest.getPercentageOfCampaignOwningCost());
        firstRequest.getChanges().forEach(change ->
        {
            Assertions.assertNotNull(change.getPercentageOfRequestOwningCost());
            Assertions.assertEquals(owningCostByChange.get(QuotaChangeRequest.ChangeKey.fromBodyValues(
                            change.getService().getKey(), change.getResource().getKey(), change.getSegmentKeys(),
                            new QuotaChangeRequest.BigOrder(Objects.requireNonNull(change.getOrder()).getId(),
                                    Objects.requireNonNull(change.getOrder()).getOrderDate(), true)))
                    .divide(new BigDecimal(read.getRequestOwningCost(), MATH_CONTEXT), MATH_CONTEXT)
                    .multiply(new BigDecimal(100, MATH_CONTEXT), MATH_CONTEXT)
                    .setScale(1, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString(), change.getPercentageOfRequestOwningCost());
        });
    }

    @Test
    public void percentageOwningCostReturnedByAPIWithoutPermissionsToSeeMoneyTest() {
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

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        long id = quotaRequests.getFirst().getId();
        QuotaChangeRequest read = quotaChangeRequestDao.read(id);
        quotaChangeRequestDao.update(read.copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());

        campaignOwningCostRefreshManager.refresh();
        DiQuotaChangeRequest firstRequest = createAuthorizedLocalClient(LOTREK) // can't view money
                .path("/v1/quota-requests/" + id)
                .get()
                .readEntity(DiQuotaChangeRequest.class);

        Assertions.assertNull(firstRequest.getRequestOwningCost());
        Assertions.assertNotNull(firstRequest.getPercentageOfCampaignOwningCost());
        firstRequest.getChanges().forEach(change ->
        {
            Assertions.assertNull(change.getOwningCost());
            Assertions.assertNotNull(change.getPercentageOfRequestOwningCost());
        });
    }

    @Test
    public void percentageOwningCostIsNullForCampaignWithoutOwningCostCalculationTest() {
        super.setUp();
        Campaign campaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
        botCampaignGroupDao.create(
                defaultCampaignGroupBuilder()
                        .addBigOrder(Objects.requireNonNull(bigOrderManager.getById(bigOrderOne.getId())))
                        .addCampaign(campaignDao.readForBot(Collections.singleton(campaign.getId())).values().iterator().next())
                        .build()
        );

        newCampaignId = campaign.getId();

        prepareCampaignResources();

        createProject(TEST_PROJECT_KEY, YANDEX, BINARY_CAT.getLogin());

        final DiListResponse<DiQuotaChangeRequest> quotaRequests = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithTypeResourcePreorder(bigOrderOne)
                        .build(), null)
                .performBy(AMOSOV_F);

        DiQuotaChangeRequest first = quotaRequests.getFirst();

        Assertions.assertNull(first.getPercentageOfCampaignOwningCost());
        Assertions.assertEquals(1, first.getChanges().size());
        Assertions.assertEquals("0", first.getChanges().get(0).getPercentageOfRequestOwningCost());
    }

    @Test
    public void percentageOwningCostCalculatedForSeveralRequestInCampaignTest() {
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

        final DiListResponse<DiQuotaChangeRequest> quotaRequests1 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        final DiListResponse<DiQuotaChangeRequest> quotaRequests2 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(2, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(2, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        final DiListResponse<DiQuotaChangeRequest> quotaRequests3 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(2, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(2, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F);

        long firstId = quotaRequests1.getFirst().getId();
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(firstId).copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());
        long secondId = quotaRequests2.getFirst().getId();
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(secondId).copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());
        long thirdId = quotaRequests3.getFirst().getId();
        quotaChangeRequestDao.update(quotaChangeRequestDao.read(thirdId).copyBuilder()
                .status(QuotaChangeRequest.Status.CONFIRMED)
                .build());

        campaignOwningCostRefreshManager.refresh();
        Map<Long, BigInteger> campaignOwningCostMap = campaignOwningCostCache.getAll().stream()
                .collect(Collectors.toMap(CampaignOwningCost::getCampaignId, CampaignOwningCost::getOwningCost));


        DiQuotaChangeRequest first = dispenser()
                .quotaChangeRequests()
                .byId(firstId)
                .get()
                .perform();
        DiQuotaChangeRequest second = dispenser()
                .quotaChangeRequests()
                .byId(secondId)
                .get()
                .perform();
        DiQuotaChangeRequest third = dispenser()
                .quotaChangeRequests()
                .byId(thirdId)
                .get()
                .perform();

        Assertions.assertEquals(campaignOwningCostMap.get(Objects.requireNonNull(first.getCampaign()).getId()),
                Stream.of(first.getRequestOwningCost(), second.getRequestOwningCost(), third.getRequestOwningCost())
                        .filter(Objects::nonNull)
                        .map(BigInteger::new)
                        .reduce(BigInteger::add)
                        .orElse(BigInteger.ZERO));

        Assertions.assertEquals("20", first.getPercentageOfCampaignOwningCost());
        Assertions.assertEquals("40", second.getPercentageOfCampaignOwningCost());
        Assertions.assertEquals("40", third.getPercentageOfCampaignOwningCost());
        Assertions.assertEquals(second.getPercentageOfCampaignOwningCost(), third.getPercentageOfCampaignOwningCost());
        Assertions.assertEquals(new BigDecimal(Objects.requireNonNull(second.getPercentageOfCampaignOwningCost()))
                        .divide(new BigDecimal(2, MATH_CONTEXT), MATH_CONTEXT)
                        .setScale(1, RoundingMode.HALF_UP)
                        .stripTrailingZeros()
                        .toPlainString(),
                first.getPercentageOfCampaignOwningCost());
    }

    @Test
    public void percentageOwningCostCalculatedShownOnlyForRequestInValidStatusesTest() {
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
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest second = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest third = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest four = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest five = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest six = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest seven = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest eight = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .importantRequest(true)
                        .build(), null)
                .performBy(AMOSOV_F).getFirst();

        DiQuotaChangeRequest nine = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
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

        Set<DiQuotaChangeRequest.Status> statuses = VALID_STATUSES.stream()
                .map(QuotaChangeRequest.Status::toView)
                .collect(Collectors.toSet());

        Stream.of(first, second, third, four, five, six, seven, eight, nine)
                .map(DiQuotaChangeRequest::getId)
                .forEach(id -> {
                    DiQuotaChangeRequest perform = dispenser()
                            .quotaChangeRequests()
                            .byId(id)
                            .get()
                            .perform();
                    Assertions.assertTrue(perform.getPercentageOfCampaignOwningCost() != null && statuses.contains(perform.getStatus())
                            || perform.getPercentageOfCampaignOwningCost() == null && !statuses.contains(perform.getStatus()));
                });
    }
}
