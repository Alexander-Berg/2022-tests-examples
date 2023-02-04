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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.beans.feed.FatalError.fatalError;
import static ru.yandex.general.consts.GeneralFeatures.FEEDS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.mock.MockFeed.feedTemplate;
import static ru.yandex.general.mock.MockFeedErrors.errorTemplate;
import static ru.yandex.general.mock.MockFeedErrors.feedErrorsTemplate;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockTaskInfo.taskFailed;
import static ru.yandex.general.mock.MockTaskInfo.taskSuccess;
import static ru.yandex.general.mock.MockTasks.failedTask;
import static ru.yandex.general.mock.MockTasks.feedTasksExample;
import static ru.yandex.general.mock.MockTasks.feedTasksTemplate;
import static ru.yandex.general.page.FeedPage.DOWNLOAD_HISTORY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FEEDS_FEATURE)
@Feature("Проверка ошибок")
@DisplayName("Проверка ошибок")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class FeedErrorsTest {

    private static final String TITLE = "Название объявления";
    private static final String MESSAGE = "Сообщение ошибки";
    private static final String DETAILED_DESCRIPTION = "Подробное описание";
    private static final String ID = "4214T214";
    private static final String DETAILED_DESCRIPTION_WITH_URL = "Сделать всё правильно помогут наши <a href=\"https://yandex.ru/support/o-desktop/rules.html#rules__name\" target=\"_blank\">советы</a>.";
    private static final String DETAILED_DESCRIPTION_URL = "https://yandex.ru/support/o-desktop/rules.html#rules__name";
    private static final String FATAL_ERROR = "Ошибки в структуре файла – строка 13, столбец – 2.";
    private static final String FATAL_ERROR_DESCRIPTION_NO_TAGS = "Сверьтесь с нашими требованиями или правилами";


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
    @DisplayName("Id ошибки")
    public void shouldSeeFeedErrorId() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsTemplate().setErrors(
                        errorTemplate().setExternalOfferId(ID)).build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().errorsTable().get(0).id().should(hasText(format("id: %s", ID)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тайтл ошибки")
    public void shouldSeeFeedErrorTitle() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsTemplate().setErrors(
                        errorTemplate().setTitle(TITLE)).build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().errorsTable().get(0).cell(1).should(hasText(TITLE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сообщение ошибки")
    public void shouldSeeFeedErrorMessage() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsTemplate().setErrors(
                        errorTemplate().setMessage(MESSAGE)).build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().errorsTable().get(0).cell(2).should(hasText(MESSAGE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Подробное описание ошибки")
    public void shouldSeeFeedErrorDetailedDescription() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsTemplate().setErrors(
                        errorTemplate().setDetailedDescription(DETAILED_DESCRIPTION)).build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().errorsTable().get(0).cell(3).should(hasText(DETAILED_DESCRIPTION));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка в подробном описании ошибки")
    public void shouldSeeFeedErrorDetailedDescriptionLink() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsTemplate().setErrors(
                        errorTemplate().setDetailedDescription(DETAILED_DESCRIPTION_WITH_URL)).build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().errorsTable().get(0).cell(3).link("советы")
                .should(hasAttribute(HREF, DETAILED_DESCRIPTION_URL));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Несколько ошибок")
    public void shouldSeeFeedSeveralErrors() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsTemplate().setErrors(
                        errorTemplate(),
                        errorTemplate(),
                        errorTemplate()).build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().errorsTable().should(hasSize(3));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Отклонён», текст в столбце «Ошибка файла»")
    public void shouldSeeFailedTaskFatalError() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskFailed().build())
                .setFeedErrors(feedErrorsTemplate().build())
                .setTasks(feedTasksTemplate().setTasks(
                        failedTask().setFatalErrors(asList(fatalError().setMessage(FATAL_ERROR)))).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().errorsTable().get(0).fatalErrorCell(1).should(hasText(FATAL_ERROR));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Отклонён», текст в столбце «Что делать»")
    public void shouldSeeFailedTaskFatalErrorDescription() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskFailed().build())
                .setFeedErrors(feedErrorsTemplate().build())
                .setTasks(feedTasksExample().build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onFeedPage().errorsTable().get(0).fatalErrorCell(2)
                .should(hasText(FATAL_ERROR_DESCRIPTION_NO_TAGS));
    }

}
