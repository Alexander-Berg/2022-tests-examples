package ru.yandex.qe.dispenser.ws.owning_cost;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

/**
 * Campaign owning cost.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
public class CampaignOwningCostTest extends BaseQuotaRequestTest {

    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;

    @Autowired
    private CampaignOwningCostCache campaignOwningCostCache;

    @Autowired
    private CampaignOwningCostRefreshManager campaignOwningCostRefreshManager;

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
    public void calculatingCampaignOwningCostTest() {
        campaignOwningCostRefreshManager.refresh();
        Set<CampaignOwningCost> all = campaignOwningCostCache.getAll();
        Assertions.assertEquals(5, all.size());
        Assertions.assertEquals(Set.of(
                CampaignOwningCost.builder().campaignId(100L).owningCost(BigInteger.ZERO).build(),
                CampaignOwningCost.builder().campaignId(142L).owningCost(BigInteger.ZERO).build(),
                CampaignOwningCost.builder().campaignId(143L).owningCost(BigInteger.ZERO).build(),
                CampaignOwningCost.builder().campaignId(176L).owningCost(BigInteger.ZERO).build(),
                CampaignOwningCost.builder().campaignId(177L).owningCost(BigInteger.ZERO).build()
        ), all);

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
                        .build(), 143L)
                .performBy(AMOSOV_F);

        long firstRequestId = quotaRequests.getFirst().getId();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests2 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(200L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), 143L)
                .performBy(AMOSOV_F);

        long secondRequestId = quotaRequests2.getFirst().getId();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests3 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(300L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), 143L)
                .performBy(AMOSOV_F);

        long thirdRequestId = quotaRequests3.getFirst().getId();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests4 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(400L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), 143L)
                .performBy(AMOSOV_F);

        long fourRequestId = quotaRequests4.getFirst().getId();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests5 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(500L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), 143L)
                .performBy(AMOSOV_F);

        long fiveRequestId = quotaRequests5.getFirst().getId();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests6 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(600L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), 143L)
                .performBy(AMOSOV_F);

        long sixRequestId = quotaRequests6.getFirst().getId();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests7 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(700L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), 143L)
                .performBy(AMOSOV_F);

        long sevenRequestId = quotaRequests7.getFirst().getId();

        final DiListResponse<DiQuotaChangeRequest> quotaRequests8 = dispenser().quotaChangeRequests()
                .create(requestBodyBuilderWithDefaultFields()
                        .projectKey(TEST_PROJECT_KEY)
                        .changes(YP, CPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(800L, DiUnit.CORES))
                        .changes(YP, SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10L, DiUnit.GIBIBYTE))
                        .changes(YP, HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20L, DiUnit.GIBIBYTE))
                        .changes(YP, RAM, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(1, DiUnit.GIBIBYTE))
                        .changes(YP, GPU, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(3, DiUnit.COUNT))
                        .changes(YP, IO_HDD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(10, DiUnit.MIBPS))
                        .changes(YP, IO_SSD, bigOrderOne.getId(), Collections.emptySet(), DiAmount.of(20, DiUnit.MIBPS))
                        .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH)
                        .build(), 143L)
                .performBy(AMOSOV_F);

        long eightRequestId = quotaRequests8.getFirst().getId();

        Map<Long, QuotaChangeRequest> read = quotaChangeRequestDao.read(List.of(firstRequestId, secondRequestId,
                thirdRequestId, fourRequestId, fiveRequestId, sixRequestId, sevenRequestId, eightRequestId));

        Assertions.assertEquals(8, read.size());

        read.compute(firstRequestId, (k,v) -> Objects.requireNonNull(v).copyBuilder().status(QuotaChangeRequest.Status.APPROVED).build());
        read.compute(secondRequestId, (k,v) -> Objects.requireNonNull(v).copyBuilder().status(QuotaChangeRequest.Status.CONFIRMED).build());
        read.compute(thirdRequestId, (k,v) -> Objects.requireNonNull(v).copyBuilder().status(QuotaChangeRequest.Status.COMPLETED).build());
        read.compute(fourRequestId, (k,v) -> Objects.requireNonNull(v).copyBuilder().status(QuotaChangeRequest.Status.NEW).build());
        read.compute(sevenRequestId, (k,v) -> Objects.requireNonNull(v).copyBuilder().status(QuotaChangeRequest.Status.READY_FOR_REVIEW).build());
        read.compute(eightRequestId, (k,v) -> Objects.requireNonNull(v).copyBuilder().status(QuotaChangeRequest.Status.NEED_INFO).build());

        read.compute(fiveRequestId, (k,v) -> Objects.requireNonNull(v).copyBuilder().status(QuotaChangeRequest.Status.CANCELLED).build());
        read.compute(sixRequestId, (k,v) -> Objects.requireNonNull(v).copyBuilder().status(QuotaChangeRequest.Status.REJECTED).build());

        boolean update = quotaChangeRequestDao.update(read.values());

        Assertions.assertTrue(update);

        campaignOwningCostRefreshManager.refresh();
        all = campaignOwningCostCache.getAll();
        Assertions.assertEquals(5, all.size());
        Assertions.assertEquals(Set.of(
                CampaignOwningCost.builder().campaignId( 100L).owningCost(BigInteger.ZERO).build(),
                CampaignOwningCost.builder().campaignId( 142L).owningCost(BigInteger.ZERO).build(),
                CampaignOwningCost.builder().campaignId( 143L).owningCost(
                        BigInteger.valueOf(read.get(firstRequestId).getRequestOwningCost())
                                .add(BigInteger.valueOf(read.get(secondRequestId).getRequestOwningCost()))
                                .add(BigInteger.valueOf(read.get(thirdRequestId).getRequestOwningCost()))
                                .add(BigInteger.valueOf(read.get(fourRequestId).getRequestOwningCost()))
                                .add(BigInteger.valueOf(read.get(sevenRequestId).getRequestOwningCost()))
                                .add(BigInteger.valueOf(read.get(eightRequestId).getRequestOwningCost()))
                ).build(),
                CampaignOwningCost.builder().campaignId(176L).owningCost(BigInteger.ZERO).build(),
                CampaignOwningCost.builder().campaignId(177L).owningCost(BigInteger.ZERO).build()
        ), all);

        read.compute(fourRequestId, (k,v) -> Objects.requireNonNull(v).copyBuilder().status(QuotaChangeRequest.Status.COMPLETED).build());
        read.compute(fiveRequestId, (k,v) -> Objects.requireNonNull(v).copyBuilder().status(QuotaChangeRequest.Status.COMPLETED).build());
        read.compute(sixRequestId, (k,v) -> Objects.requireNonNull(v).copyBuilder().status(QuotaChangeRequest.Status.COMPLETED).build());
        read.compute(sevenRequestId, (k,v) -> Objects.requireNonNull(v).copyBuilder().status(QuotaChangeRequest.Status.COMPLETED).build());
        read.compute(eightRequestId, (k,v) -> Objects.requireNonNull(v).copyBuilder().status(QuotaChangeRequest.Status.COMPLETED).build());

        update = quotaChangeRequestDao.update(read.values());

        Assertions.assertTrue(update);

        campaignOwningCostRefreshManager.refresh();
        all = campaignOwningCostCache.getAll();
        Assertions.assertEquals(5, all.size());
        Assertions.assertEquals(Set.of(
                CampaignOwningCost.builder().campaignId( 100L).owningCost(BigInteger.ZERO).build(),
                CampaignOwningCost.builder().campaignId( 142L).owningCost(BigInteger.ZERO).build(),
                CampaignOwningCost.builder().campaignId( 143L).owningCost(
                        read.values().stream()
                                .reduce(BigInteger.ZERO, (a,b) -> a.add(BigInteger.valueOf(b.getRequestOwningCost())), BigInteger::add)
                ).build(),
                CampaignOwningCost.builder().campaignId(176L).owningCost(BigInteger.ZERO).build(),
                CampaignOwningCost.builder().campaignId(177L).owningCost(BigInteger.ZERO).build()
        ), all);
    }

}
