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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FEEDS_FEATURE)
@Feature("Модалка истории загрузок. Пагинатор.")
@DisplayName("Модалка истории загрузок. Пагинатор.")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class FeedHistoryPaginatorTest {

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
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет пагинатора с одним пейджом тасков")
    public void shouldSeeNoPaginator() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample()
                        .setTotalPageCount(1)
                        .setCurrentPage(1).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().waitUntil(isDisplayed());

        basePageSteps.onFeedPage().feedHistoryModal().paginator().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Есть пагинатор с двумя пейджами тасков")
    public void shouldSeePaginatorTwoPages() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample()
                        .setTotalPageCount(2)
                        .setCurrentPage(1).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().waitUntil(isDisplayed());

        basePageSteps.onFeedPage().feedHistoryModal().paginator().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается активный первый пейдж в пагинаторе")
    public void shouldSeePaginatorFirstPageActive() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample()
                        .setTotalPageCount(2)
                        .setCurrentPage(1).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().waitUntil(isDisplayed());

        basePageSteps.onFeedPage().feedHistoryModal().paginator().activePage().should(hasText("1"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается активный второй пейдж в пагинаторе")
    public void shouldSeePaginatorSecondPageActive() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample()
                        .setTotalPageCount(2)
                        .setCurrentPage(2).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().waitUntil(isDisplayed());

        basePageSteps.onFeedPage().feedHistoryModal().paginator().activePage().should(hasText("2"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка назад задизейблена на первом пейдже в пагинаторе")
    public void shouldSeePaginatorPrevButtonDisabledOnFirstPage() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample()
                        .setTotalPageCount(2)
                        .setCurrentPage(1).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().waitUntil(isDisplayed());

        basePageSteps.onFeedPage().feedHistoryModal().paginator().prev().disabledLink().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка вперед активна на первом пейдже в пагинаторе, при наличии пейджей")
    public void shouldSeePaginatorNextButtonActiveOnFirstPage() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample()
                        .setTotalPageCount(2)
                        .setCurrentPage(1).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().waitUntil(isDisplayed());

        basePageSteps.onFeedPage().feedHistoryModal().paginator().next().disabledLink().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка назад активна на втором пейдже в пагинаторе")
    public void shouldSeePaginatorPrevButtonActiveOnSecondPage() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample()
                        .setTotalPageCount(2)
                        .setCurrentPage(2).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().waitUntil(isDisplayed());

        basePageSteps.onFeedPage().feedHistoryModal().paginator().prev().disabledLink().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка вперед задизейблена на последнем пейдже в пагинаторе")
    public void shouldSeePaginatorNextButtonDisabledOnLastPage() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample()
                        .setTotalPageCount(3)
                        .setCurrentPage(3).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().waitUntil(isDisplayed());

        basePageSteps.onFeedPage().feedHistoryModal().paginator().next().disabledLink().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопки назад и вперед активны на среднем пейдже в пагинаторе")
    public void shouldSeePaginatorPrevAndNextButtosActiveOnMiddlePage() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample()
                        .setTotalPageCount(3)
                        .setCurrentPage(2).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().waitUntil(isDisplayed());

        basePageSteps.onFeedPage().feedHistoryModal().paginator().prev().disabledLink().should(not(isDisplayed()));
        basePageSteps.onFeedPage().feedHistoryModal().paginator().next().disabledLink().should(not(isDisplayed()));
    }

}
