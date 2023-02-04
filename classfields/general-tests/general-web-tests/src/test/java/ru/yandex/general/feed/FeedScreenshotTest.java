package ru.yandex.general.feed;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.FEEDS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SCREENSHOT_TESTS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mock.MockFeed.feedTemplate;
import static ru.yandex.general.mock.MockFeedErrors.feedErrorsAndWarnings;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockTaskInfo.taskFailed;
import static ru.yandex.general.mock.MockTaskInfo.taskInProgress;
import static ru.yandex.general.mock.MockTaskInfo.taskSuccess;
import static ru.yandex.general.mock.MockTasks.feedTasksExample;
import static ru.yandex.general.page.FeedPage.DOWNLOAD_HISTORY;
import static ru.yandex.general.page.FeedPage.EDIT;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_THEME;
import static ru.yandex.general.step.BasePageSteps.DARK_THEME;
import static ru.yandex.general.step.BasePageSteps.LIGHT_THEME;

@Epic(FEEDS_FEATURE)
@Feature(SCREENSHOT_TESTS)
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FeedScreenshotTest {

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

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private CompareSteps compareSteps;

    @Parameterized.Parameter
    public String theme;

    @Parameterized.Parameters(name = "{index}. Тема «{0}»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {LIGHT_THEME},
                {DARK_THEME}
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        basePageSteps.setCookie(CLASSIFIED_USER_THEME, theme);
        compareSteps.resize(1920, 1500);
        urlSteps.testing().path(MY).path(FEED);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот страницы фидов с активной выгрузкой и ошибками, светлая/тёмная темы")
    public void shouldSeeFeedPageActiveScreenshot() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();

        urlSteps.open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFeedPage().pageMain());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFeedPage().pageMain());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот страницы фидов с выгрузкой в статусе «Обрабатывается», светлая/тёмная темы")
    public void shouldSeeFeedPageInProgressScreenshot() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskInProgress().build()).build()).withDefaults().create();

        urlSteps.open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFeedPage().pageMain());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFeedPage().pageMain());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот страницы фидов с выгрузкой в статусе «Отклонён», светлая/тёмная темы")
    public void shouldSeeFeedPageFailedScreenshot() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskFailed().build()).build()).withDefaults().create();

        urlSteps.open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFeedPage().pageMain());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFeedPage().pageMain());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот истории загрузок фидов, светлая/тёмная темы")
    public void shouldSeeFeedDownloadHistoryScreenshot() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();

        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFeedPage().modal().content());

        urlSteps.setProductionHost().open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFeedPage().modal().content());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот попапа обновления фида, светлая/тёмная темы")
    public void shouldSeeFeedUpdatePopupScreenshot() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();

        urlSteps.open();
        basePageSteps.onFeedPage().button(EDIT).click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onFeedPage().modal().content());

        urlSteps.setProductionHost().open();
        basePageSteps.onFeedPage().button(EDIT).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onFeedPage().modal().content());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
