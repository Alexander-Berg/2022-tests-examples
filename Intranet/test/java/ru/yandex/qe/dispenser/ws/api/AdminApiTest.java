package ru.yandex.qe.dispenser.ws.api;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.bot.settings.BotCampaignGroupDao;
import ru.yandex.qe.dispenser.domain.dao.goal.Goal;
import ru.yandex.qe.dispenser.domain.dao.goal.OkrAncestors;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.ws.logic.BaseQuotaRequestTest;
import ru.yandex.qe.dispenser.ws.quota.request.owning_cost.QuotaChangeOwningCostRefreshManager;

import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignGroupBuilder;

/**
 * Admin API tests.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
public class AdminApiTest extends BaseQuotaRequestTest {

    @Autowired
    QuotaChangeOwningCostRefreshManager quotaChangeOwningCostRefreshManager;
    @Autowired
    private BotCampaignGroupDao botCampaignGroupDao;
    @Autowired
    private ResourceDao resourceDao;

    String SSD = "ssd_segmented";
    String HDD = "hdd_segmented";
    String CPU = "cpu_segmented";
    String RAM = "ram_segmented";
    String GPU = "gpu_segmented";
    String IO_HDD = "io_hdd";
    String IO_SSD = "io_ssd";

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        LocalDate date = LocalDate.of(2021, Month.AUGUST, 1);
        Campaign campaign = campaignDao.insertForTest(defaultCampaignBuilder(bigOrderOne)
                .setKey("august_campaign")
                .setName("august_campaign")
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
    public void refreshOwningCostTest() {
        createProject(TEST_PROJECT_KEY, YANDEX, AMOSOV_F.getLogin());

        Service service = Hierarchy.get().getServiceReader().read(YP);

        resourceDao.create(new Resource.Builder(SSD, service).name(SSD).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(HDD, service).name(HDD).type(DiResourceType.STORAGE).build());
        resourceDao.create(new Resource.Builder(CPU, service).name(CPU).type(DiResourceType.PROCESSOR).build());
        resourceDao.create(new Resource.Builder(RAM, service).name(RAM).type(DiResourceType.MEMORY).build());
        resourceDao.create(new Resource.Builder(GPU, service).name(GPU).type(DiResourceType.ENUMERABLE).build());
        resourceDao.create(new Resource.Builder(IO_HDD, service).name(IO_HDD).type(DiResourceType.BINARY_TRAFFIC).build());
        resourceDao.create(new Resource.Builder(IO_SSD, service).name(IO_SSD).type(DiResourceType.BINARY_TRAFFIC).build());

        goalDao.clear();
        goalDao.create(new Goal(77L, "Do it!", ru.yandex.inside.goals.model.Goal.Importance.COMPANY, ru.yandex.inside.goals.model.Goal.Status.PLANNED, OkrAncestors.EMPTY));

        updateHierarchy();

        prepareCampaignResources();

        for (int i = 0; i < 200; i++) {
            makeRequest();
            makeGoalRequest();
        }

        quotaChangeOwningCostRefreshManager.refresh();
    }

    private void makeRequest() {
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
                .performBy(AMOSOV_F);
    }

    private void makeGoalRequest() {
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
                        .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                        .goalId(77L)
                        .build(), null)
                .performBy(AMOSOV_F);
    }
}
