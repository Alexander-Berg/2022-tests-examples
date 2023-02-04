package ru.yandex.qe.dispenser.ws.logic;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.inside.goals.model.Goal.Importance;
import ru.yandex.inside.goals.model.Goal.Status;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiQuotaChangeRequest;
import ru.yandex.qe.dispenser.api.v1.DiResourcePreorderReasonType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.quota.Body;
import ru.yandex.qe.dispenser.domain.Campaign;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.goal.Goal;
import ru.yandex.qe.dispenser.domain.dao.goal.GoalDao;
import ru.yandex.qe.dispenser.domain.dao.goal.OkrAncestors;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;

import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class BaseQuotaRequestTest extends BusinessLogicTestBase {
    public static final LocalDate TEST_BIG_ORDER_DATE = LocalDate.of(2020, Month.JANUARY, 1);
    public static final LocalDate TEST_BIG_ORDER_DATE_2 = LocalDate.of(2020, Month.JUNE, 1);
    public static final LocalDate TEST_BIG_ORDER_DATE_3 = LocalDate.of(2020, Month.DECEMBER, 1);
    public static final String TEST_DESCRIPTION = "For needs.";
    public static final String TEST_CALCULATIONS = "Description for how we calculated required amounts";
    public static final String TEST_CAMPAIGN_KEY = "testCamp";
    public static final String TEST_CAMPAIGN_NAME = "Тестовая компания";
    public final static Long TEST_GOAL_ID = 1L;
    public static final String TEST_GOAL_NAME = "Test Super Goal";
    public static final String TEST_PROJECT_KEY = "Test";
    public static final String TEST_SUMMARY = "test";

    protected Goal newGoal;
    protected Long newCampaignId;

    @Autowired
    protected BigOrderManager bigOrderManager;

    @Autowired
    protected GoalDao goalDao;

    protected BigOrder bigOrderOne;
    protected BigOrder bigOrderTwo;
    protected BigOrder bigOrderThree;

    @BeforeAll
    public void beforeClass() {
        bigOrderManager.clear();
        bigOrderOne = bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE));
        bigOrderTwo = bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE_2));
        bigOrderThree = bigOrderManager.create(BigOrder.builder(TEST_BIG_ORDER_DATE_3));

        campaignDao.clear();
        final Campaign newCampaign = campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
        newCampaignId = newCampaign.getId();

        goalDao.clear();
        newGoal = goalDao.create(new Goal(TEST_GOAL_ID, TEST_GOAL_NAME, Importance.DEPARTMENT, Status.NEW, OkrAncestors.EMPTY));
    }

    public Campaign createDefaultCampaign() {
        return campaignDao.create(defaultCampaignBuilder(bigOrderOne).build());
    }

    @NotNull
    public static Body.BodyBuilder requestBodyBuilderWithDefaultFields() {
        return new Body.BodyBuilder()
                .summary(TEST_SUMMARY)
                .description(TEST_DESCRIPTION)
                .calculations(TEST_CALCULATIONS);
    }

    @NotNull
    public static Body.BodyBuilder requestBodyBuilderWithChanges() {
        return requestBodyBuilderWithDefaultFields()
                .projectKey(TEST_PROJECT_KEY)
                .changes(NIRVANA, YT_CPU, null, Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT));
    }

    @NotNull
    public static Body.BodyBuilder requestBodyBuilderWithTypeResourcePreorder(BigOrder bigOrder) {
        return requestBodyBuilderWithDefaultFields()
                .projectKey(TEST_PROJECT_KEY)
                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                .changes(NIRVANA, YT_CPU, bigOrder.getId(), Collections.emptySet(), DiAmount.of(100L, DiUnit.COUNT))
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GROWTH);
    }

    @NotNull
    public Body.BodyBuilder requestBodyBuilderWithReasonTypeGoalAndGoal() {
        return requestBodyBuilderWithTypeResourcePreorder(bigOrderOne)
                .type(DiQuotaChangeRequest.Type.RESOURCE_PREORDER)
                .resourcePreorderReasonType(DiResourcePreorderReasonType.GOAL)
                .goalId(TEST_GOAL_ID);
    }
}
