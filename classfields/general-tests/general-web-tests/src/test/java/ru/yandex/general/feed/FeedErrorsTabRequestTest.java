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

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.beans.ajaxRequests.FeedErrors.feedErrors;
import static ru.yandex.general.consts.GeneralFeatures.FEEDS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mock.MockFeed.feedTemplate;
import static ru.yandex.general.mock.MockFeedErrors.feedErrorsAndWarnings;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockTaskInfo.taskSuccess;
import static ru.yandex.general.mock.MockTasks.feedTasksExample;
import static ru.yandex.general.page.FeedPage.ALL;
import static ru.yandex.general.page.FeedPage.CRITICAL;
import static ru.yandex.general.page.FeedPage.WARNINGS;
import static ru.yandex.general.step.AjaxProxySteps.GET_FEED_ERRORS;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;

@Epic(FEEDS_FEATURE)
@Feature("Запрос списка ошибок при переключении таба")
@DisplayName("Запрос списка ошибок при переключении таба")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class FeedErrorsTabRequestTest {

    private String taskId;
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
        taskId = String.valueOf(getRandomIntInRange(1, 50));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Запрос таба с критическими ошибками")
    public void shouldSeeFeedCriticalErrorsRequest() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().setTaskId(taskId).build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().tab(CRITICAL).click();

        ajaxProxySteps.setAjaxHandler(GET_FEED_ERRORS).withRequestText(
                feedErrors().setTaskId(taskId).setPage(1).setLimit(25).setFilter("Criticals")).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Запрос таба с предупреждениями")
    public void shouldSeeFeedWarningsRequest() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().setTaskId(taskId).build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().tab(WARNINGS).click();

        ajaxProxySteps.setAjaxHandler(GET_FEED_ERRORS).withRequestText(
                feedErrors().setTaskId(taskId).setPage(1).setLimit(25).setFilter("Warnings")).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Запрос таба со всеми ошибками")
    public void shouldSeeFeedAllErrorsRequest() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().setTaskId(taskId).build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().tab(WARNINGS).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        ajaxProxySteps.clearHar();
        basePageSteps.onFeedPage().tab(ALL).click();

        ajaxProxySteps.setAjaxHandler(GET_FEED_ERRORS).withRequestText(
                feedErrors().setTaskId(taskId).setPage(1).setLimit(25).setFilter("All")).shouldExist();
    }

}
