package ru.yandex.general.feed;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.beans.feed.FeedStatistics.feedStatistics;
import static ru.yandex.general.consts.GeneralFeatures.FEEDS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mock.MockFeed.feedTemplate;
import static ru.yandex.general.mock.MockFeedErrors.feedErrorsAndWarnings;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockTaskInfo.taskSuccess;
import static ru.yandex.general.mock.MockTasks.failedTask;
import static ru.yandex.general.mock.MockTasks.feedTasksExample;
import static ru.yandex.general.mock.MockTasks.feedTasksTemplate;
import static ru.yandex.general.mock.MockTasks.inProgressTask;
import static ru.yandex.general.mock.MockTasks.succeedTask;
import static ru.yandex.general.page.FeedPage.DOWNLOAD_HISTORY;
import static ru.yandex.general.page.FeedPage.FAILED;
import static ru.yandex.general.page.FeedPage.PROCESSED;
import static ru.yandex.general.page.FeedPage.PROCESSING;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FEEDS_FEATURE)
@Feature("Модалка истории загрузок")
@DisplayName("Модалка истории загрузок")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class FeedHistoryTest {

    private static final String DATE = "2021-08-02T12:41:58.859722Z";
    private static final String DATE_RU = "2 августа 15:41";
    private static final String DASH = "—";

    private int count;
    private MockResponse mockResponse = mockResponse()
            .setCurrentUserExample()
            .setCategoriesTemplate()
            .setRegionsTemplate();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(FEED);
        count = getRandomIntInRange(1, 1000);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «В обработке», отображается статус")
    public void shouldSeeInProgressTaskInFeedHistory() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksTemplate().setTasks(inProgressTask()).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();

        basePageSteps.onFeedPage().feedHistoryModal().rows().should(hasSize(1));
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).status().should(hasText(PROCESSING));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «В обработке», отображаются прочерки в остальных столбцах")
    public void shouldSeeInProgressTaskInFeedHistoryDashes() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksTemplate().setTasks(inProgressTask()).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();

        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).time().waitUntil(isDisplayed()).should(hasText(DASH));
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).totalOffers().should(hasText(DASH));
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).activeOffers().should(hasText(DASH));
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).errorOffers().should(hasText(DASH));
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).criticalErrorOffers().should(hasText(DASH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Отклонён», отображается статус")
    public void shouldSeeFailedTaskInFeedHistory() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksTemplate().setTasks(failedTask()).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();

        basePageSteps.onFeedPage().feedHistoryModal().rows().should(hasSize(1));
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).status().should(hasText(FAILED));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Отклонён», отображается время")
    public void shouldSeeFailedTaskInFeedHistoryTime() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksTemplate().setTasks(
                        failedTask().setFinishedAt(DATE)).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();

        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).time().waitUntil(isDisplayed()).should(hasText(DATE_RU));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Отклонён», отображаются прочерки в остальных столбцах")
    public void shouldSeeFailedTaskInFeedHistoryDashes() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksTemplate().setTasks(failedTask()).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();

        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).totalOffers().waitUntil(isDisplayed())
                .should(hasText(DASH));
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).activeOffers().should(hasText(DASH));
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).errorOffers().should(hasText(DASH));
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).criticalErrorOffers().should(hasText(DASH));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Обработан», отображается статус")
    public void shouldSeeActiveTaskInFeedHistory() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksTemplate().setTasks(succeedTask()).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();

        basePageSteps.onFeedPage().feedHistoryModal().rows().should(hasSize(1));
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).status().should(hasText(PROCESSED));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Обработан», отображается время")
    public void shouldSeeActiveTaskInFeedHistoryTime() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksTemplate().setTasks(
                        succeedTask().setFinishedAt(DATE)).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();

        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).time().waitUntil(isDisplayed())
                .should(hasText(DATE_RU));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Обработан», отображается общее кол-во объявлений")
    public void shouldSeeActiveTaskInFeedHistoryTotalOffers() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksTemplate().setTasks(succeedTask().setFeedStatistics(
                        feedStatistics().setTotalOfferCount(count))).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();

        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).totalOffers().waitUntil(isDisplayed())
                .should(hasText(String.valueOf(count)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Обработан», отображается кол-во активных объявлений")
    public void shouldSeeActiveTaskInFeedHistoryActiveOffers() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksTemplate().setTasks(succeedTask().setFeedStatistics(
                        feedStatistics().setActiveOfferCount(count))).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();

        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).activeOffers().waitUntil(isDisplayed())
                .should(hasText(String.valueOf(count)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Обработан», отображается кол-во объявлений с ошибками")
    public void shouldSeeActiveTaskInFeedHistoryErrorOffers() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksTemplate().setTasks(succeedTask().setFeedStatistics(
                        feedStatistics().setErrorCount(count))).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();

        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).errorOffers().waitUntil(isDisplayed())
                .should(hasText(String.valueOf(count)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Обработан», отображается кол-во объявлений с ошибками")
    public void shouldSeeActiveTaskInFeedHistoryCriticalErrorOffers() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksTemplate().setTasks(succeedTask().setFeedStatistics(
                        feedStatistics().setCriticalErrorCount(count))).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();

        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).criticalErrorOffers().waitUntil(isDisplayed())
                .should(hasText(String.valueOf(count)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается 3 таска, каждого типа")
    public void shouldSeeThreeTasksEveryTypeInFeedHistory() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksTemplate().setTasks(
                        succeedTask(), inProgressTask(), failedTask()).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();

        basePageSteps.onFeedPage().feedHistoryModal().rows().should(hasSize(3));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается кнопка «Назад»")
    public void shouldNotSeeBackButtonInFeedHistory() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();

        basePageSteps.onFeedPage().feedHistoryModal().backButton().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка закрывается по крестику")
    public void shouldCloseFeedHistoryModal() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().waitUntil(isDisplayed());
        basePageSteps.onFeedPage().feedHistoryModal().close().click();

        basePageSteps.onFeedPage().feedHistoryModal().should(not(isDisplayed()));
    }

}
