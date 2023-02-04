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
import ru.yandex.general.page.FeedPage;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FEEDS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.mock.MockFeed.feedTemplate;
import static ru.yandex.general.mock.MockFeedErrors.errorTemplate;
import static ru.yandex.general.mock.MockFeedErrors.feedErrorsAndWarnings;
import static ru.yandex.general.mock.MockFeedErrors.feedErrorsTemplate;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockTaskInfo.taskFailed;
import static ru.yandex.general.mock.MockTaskInfo.taskInProgress;
import static ru.yandex.general.mock.MockTaskInfo.taskSuccess;
import static ru.yandex.general.mock.MockTasks.feedTasksExample;
import static ru.yandex.general.page.FeedPage.ALL;
import static ru.yandex.general.page.FeedPage.CRITICAL;
import static ru.yandex.general.page.FeedPage.EDIT;
import static ru.yandex.general.page.FeedPage.FEED_URL_INPUT;
import static ru.yandex.general.page.FeedPage.WARNINGS;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FEEDS_FEATURE)
@Feature("Проверка базовых контролов")
@DisplayName("Проверка базовых контролов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class FeedFieldsTest {

    private static final String TERMS_LINK = "https://yandex.ru/legal/classified_termsofuse";

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

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(FEED);
        count = getRandomIntInRange(1, 1000);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается общее кол-во объявлений в общей сводке")
    public void shouldSeeFeedTotalOfferCount() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().setTotalOfferCount(count).build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().statsColumn(FeedPage.OFFERS).should(hasText(String.valueOf(count)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кол-во активных объявлений в общей сводке")
    public void shouldSeeFeedActiveOfferCount() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().setActiveOfferCount(count).build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().statsColumn(FeedPage.ACTIVE).should(hasText(String.valueOf(count)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кол-во ошибок в общей сводке")
    public void shouldSeeFeedErrorCount() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().setErrorCount(count).build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().statsColumn(FeedPage.ERRORS).should(hasText(String.valueOf(count)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кол-во критических ошибок в общей сводке")
    public void shouldSeeFeedCriticalErrorCount() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().setCriticalErrorCount(count).build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().statsColumn(CRITICAL).should(hasText(String.valueOf(count)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кол-во критических ошибок в табе")
    public void shouldSeeFeedCriticalErrorCountInTab() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().setCriticalErrorCount(count).build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().tab(CRITICAL).count().should(hasText(String.valueOf(count)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается кол-во предупреждений в табе")
    public void shouldSeeFeedWarningsCountInTab() {
        int errorCount = getRandomIntInRange(100, 200);
        int criticalErrorCount = getRandomIntInRange(50, 99);
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().setErrorCount(errorCount)
                        .setCriticalErrorCount(criticalErrorCount).build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().tab(WARNINGS).count().should(hasText(String.valueOf(errorCount - criticalErrorCount)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается общее кол-во ошибок в табе")
    public void shouldSeeFeedErrorCountInTab() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().setErrorCount(count).build())
                .setFeedErrors(feedErrorsTemplate().setErrors(errorTemplate()).build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().tab(ALL).count().should(hasText(String.valueOf(count)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается статус «Обрабатывается»")
    public void shouldSeeFeedInProgressStatus() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskInProgress().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().feedStatus().should(hasText("Обрабатывается"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается статус «Обработан»")
    public void shouldSeeFeedActiveStatus() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().setTotalOfferCount(count).build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().feedStatus().should(hasText("Обработан"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается статус «Отклонён»")
    public void shouldSeeFeedFailedStatus() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskFailed().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().feedStatus().should(hasText("Отклонён"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрывается модалка редактирования фида")
    public void shouldSeeCloseFeedEditModal() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(EDIT).click();
        basePageSteps.onFeedPage().modal().input(FEED_URL_INPUT).waitUntil(isDisplayed());
        basePageSteps.onFeedPage().modal().close().click();

        basePageSteps.onFeedPage().modal().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на условия в модалке редактирования фида")
    public void shouldSeeTermsLinkInFeedEditModal() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(EDIT).click();
        basePageSteps.onFeedPage().modal().input(FEED_URL_INPUT).waitUntil(isDisplayed());

        basePageSteps.onFeedPage().modal().link("условиями").should(hasAttribute(HREF, TERMS_LINK));
    }

}
