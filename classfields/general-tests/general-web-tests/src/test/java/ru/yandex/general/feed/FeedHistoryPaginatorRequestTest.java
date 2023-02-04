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
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.beans.feed.FeedTasks.feedTasks;
import static ru.yandex.general.consts.GeneralFeatures.FEEDS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mock.MockFeed.feedTemplate;
import static ru.yandex.general.mock.MockFeedErrors.feedErrorsAndWarnings;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockTaskInfo.taskSuccess;
import static ru.yandex.general.mock.MockTasks.feedTasksExample;
import static ru.yandex.general.page.FeedPage.DOWNLOAD_HISTORY;
import static ru.yandex.general.step.AjaxProxySteps.GET_FEED_TASKS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FEEDS_FEATURE)
@Feature("Модалка истории загрузок. Пагинатор.")
@DisplayName("Модалка истории загрузок. Пагинатор. Тесты на запрос при переключении.")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class FeedHistoryPaginatorRequestTest {

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

    @Inject
    private AjaxProxySteps ajaxProxySteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(FEED);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("По клику на пейдж «2» отправляется запрос getFeedTasks со вторым пейджом")
    public void shouldSeePaginatorSeconPageRequest() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample()
                        .setTotalPageCount(2)
                        .setCurrentPage(1).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed());
        basePageSteps.wait500MS();
        ajaxProxySteps.clearHar();
        basePageSteps.onFeedPage().feedHistoryModal().paginator().pageNumber(2).click();

        ajaxProxySteps.setAjaxHandler(GET_FEED_TASKS).withRequestText(feedTasks().setPage(2)).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("По клику на «Вперёд» отправляется запрос getFeedTasks со следующим пейджом")
    public void shouldSeePaginatorNextPageRequest() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample()
                        .setTotalPageCount(2)
                        .setCurrentPage(1).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed());
        basePageSteps.wait500MS();
        ajaxProxySteps.clearHar();
        basePageSteps.onFeedPage().feedHistoryModal().paginator().next().click();

        ajaxProxySteps.setAjaxHandler(GET_FEED_TASKS).withRequestText(feedTasks().setPage(2)).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("По клику на «Назад» отправляется запрос getFeedTasks с предыдущим пейджом")
    public void shouldSeePaginatorPrevPageRequest() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample()
                        .setTotalPageCount(2)
                        .setCurrentPage(2).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed());
        basePageSteps.wait500MS();
        ajaxProxySteps.clearHar();
        basePageSteps.onFeedPage().feedHistoryModal().paginator().prev().click();

        ajaxProxySteps.setAjaxHandler(GET_FEED_TASKS).withRequestText(feedTasks().setPage(1)).shouldExist();
    }

}
