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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.beans.feed.FatalError.fatalError;
import static ru.yandex.general.beans.feed.FeedStatistics.feedStatistics;
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
import static ru.yandex.general.mock.MockTasks.failedTask;
import static ru.yandex.general.mock.MockTasks.feedTasksExample;
import static ru.yandex.general.mock.MockTasks.feedTasksTemplate;
import static ru.yandex.general.mock.MockTasks.inProgressTask;
import static ru.yandex.general.mock.MockTasks.succeedTask;
import static ru.yandex.general.page.FeedPage.ALL;
import static ru.yandex.general.page.FeedPage.CRITICAL;
import static ru.yandex.general.page.FeedPage.DOWNLOAD_HISTORY;
import static ru.yandex.general.page.FeedPage.WARNINGS;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FEEDS_FEATURE)
@Feature("Модалка истории загрузок. Подробная инфа по таске.")
@DisplayName("Модалка истории загрузок. Подробная инфа по таске.")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class FeedHistoryTaskDetailsTest {

    private static final String NOT_FOUND = "Не найдено ни одной записи\nПожалуйста, используйте другие параметры";
    private static final String FATAL_ERROR = "Ошибки в структуре файла – строка 13, столбец – 2.";
    private static final String FATAL_ERROR_DESCRIPTION = "Сверьтесь с <a href=\"https://yandex.ru/support/o-desktop/price-list-requirements.html\" target=\"_blank\">нашими требованиями</a>";
    private static final String FATAL_ERROR_DESCRIPTION_NO_TAGS = "Сверьтесь с нашими требованиями";
    private static final String TITLE = "Название объявления";
    private static final String MESSAGE = "Сообщение ошибки";
    private static final String DETAILED_DESCRIPTION = "Подробное описание";
    private static final String ID = "4214T214";
    private static final String DETAILED_DESCRIPTION_WITH_URL = "Сделать всё правильно помогут наши <a href=\"https://yandex.ru/support/o-desktop/rules.html#rules__name\" target=\"_blank\">советы</a>.";
    private static final String DETAILED_DESCRIPTION_URL = "https://yandex.ru/support/o-desktop/rules.html#rules__name";


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
    @DisplayName("Таск в статусе «В обработке», в подробной информации нет записей")
    public void shouldSeeInProgressTaskDetailsInFeedHistory() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskInProgress().build())
                .setFeedErrors(feedErrorsTemplate().build())
                .setTasks(feedTasksTemplate().setTasks(inProgressTask()).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed()).click();

        basePageSteps.onFeedPage().feedHistoryModal().message().should(hasText(NOT_FOUND));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Отклонён», текст в столбце «Ошибка файла»")
    public void shouldSeeFailedTaskDetailsInFeedHistoryFatalError() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskFailed().build())
                .setFeedErrors(feedErrorsTemplate().build())
                .setTasks(feedTasksTemplate().setTasks(
                        failedTask().setFatalErrors(asList(fatalError().setMessage(FATAL_ERROR)))).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed()).click();

        basePageSteps.onFeedPage().feedHistoryModal().errorsTable().get(0).fatalErrorCell(1).should(hasText(FATAL_ERROR));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Отклонён», текст в столбце «Что делать»")
    public void shouldSeeFailedTaskDetailsInFeedHistoryFatalErrorDescription() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskFailed().build())
                .setFeedErrors(feedErrorsTemplate().build())
                .setTasks(feedTasksTemplate().setTasks(
                        failedTask().setFatalErrors(asList(fatalError().setDescription(FATAL_ERROR_DESCRIPTION)))).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed()).click();

        basePageSteps.onFeedPage().feedHistoryModal().errorsTable().get(0).fatalErrorCell(2)
                .should(hasText(FATAL_ERROR_DESCRIPTION_NO_TAGS));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Обработан», Id ошибки")
    public void shouldSeeFeedErrorId() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsTemplate().setErrors(
                        errorTemplate().setExternalOfferId(ID)).build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed()).click();

        basePageSteps.onFeedPage().feedHistoryModal().errorsTable().get(0).id().should(hasText(format("id: %s", ID)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Обработан», Тайтл ошибки")
    public void shouldSeeFeedErrorTitle() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsTemplate().setErrors(
                        errorTemplate().setTitle(TITLE)).build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed()).click();

        basePageSteps.onFeedPage().errorsTable().get(0).cell(1).should(hasText(TITLE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Обработан», Сообщение ошибки")
    public void shouldSeeFeedErrorMessage() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsTemplate().setErrors(
                        errorTemplate().setMessage(MESSAGE)).build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed()).click();

        basePageSteps.onFeedPage().errorsTable().get(0).cell(2).should(hasText(MESSAGE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Обработан», Подробное описание ошибки")
    public void shouldSeeFeedErrorDetailedDescription() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsTemplate().setErrors(
                        errorTemplate().setDetailedDescription(DETAILED_DESCRIPTION)).build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed()).click();

        basePageSteps.onFeedPage().errorsTable().get(0).cell(3).should(hasText(DETAILED_DESCRIPTION));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Обработан», Ссылка в подробном описании ошибки")
    public void shouldSeeFeedErrorDetailedDescriptionLink() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsTemplate().setErrors(
                        errorTemplate().setDetailedDescription(DETAILED_DESCRIPTION_WITH_URL)).build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed()).click();

        basePageSteps.onFeedPage().errorsTable().get(0).cell(3).link("советы")
                .should(hasAttribute(HREF, DETAILED_DESCRIPTION_URL));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Обработан», Отображается кол-во критических ошибок в табе")
    public void shouldSeeFeedCriticalErrorCountInTab() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksTemplate().setTasks(succeedTask().setFeedStatistics(
                        feedStatistics().setCriticalErrorCount(count))).build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed()).click();

        basePageSteps.onFeedPage().feedHistoryModal().tab(CRITICAL).count().should(hasText(String.valueOf(count)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Обработан», Отображается кол-во предупреждений в табе")
    public void shouldSeeFeedWarningsCountInTab() {
        int errorCount = getRandomIntInRange(100, 200);
        int criticalErrorCount = getRandomIntInRange(50, 99);
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksTemplate().setTasks(succeedTask().setFeedStatistics(
                        feedStatistics().setErrorCount(errorCount).setCriticalErrorCount(criticalErrorCount)))
                        .build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed()).click();

        basePageSteps.onFeedPage().feedHistoryModal().tab(WARNINGS).count().should(hasText(String.valueOf(errorCount - criticalErrorCount)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Обработан», Отображается общее кол-во ошибок в табе")
    public void shouldSeeFeedErrorCountInTab() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsTemplate().setErrors(errorTemplate()).build())
                .setTasks(feedTasksTemplate().setTasks(succeedTask().setFeedStatistics(
                        feedStatistics().setErrorCount(count)))
                        .build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed()).click();

        basePageSteps.onFeedPage().feedHistoryModal().tab(ALL).count().should(hasText(String.valueOf(count)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Таск в статусе «Обработан», несколько ошибок")
    public void shouldSeeTaskSeveralErrors() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsTemplate().setErrors(
                        errorTemplate(),
                        errorTemplate(),
                        errorTemplate()).build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed()).click();

        basePageSteps.onFeedPage().feedHistoryModal().errorsTable().should(hasSize(3));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на историю из подробной информации по таску")
    public void shouldSeeBackToHistoryFromDetailedDescription() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed()).click();
        basePageSteps.onFeedPage().feedHistoryModal().errorsTable().get(0).waitUntil(isDisplayed());
        basePageSteps.onFeedPage().feedHistoryModal().backButton().click();

        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка на разделе подробной информации закрывается по крестику")
    public void shouldCloseFeedHistoryModal() {
        mockRule.graphqlStub(mockResponse.setFeed(feedTemplate().build())
                .setTaskInfo(taskSuccess().build())
                .setFeedErrors(feedErrorsAndWarnings().build())
                .setTasks(feedTasksExample().build()).build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onFeedPage().button(DOWNLOAD_HISTORY).click();
        basePageSteps.onFeedPage().feedHistoryModal().rows().get(0).waitUntil(isDisplayed()).click();
        basePageSteps.onFeedPage().feedHistoryModal().errorsTable().get(0).waitUntil(isDisplayed());
        basePageSteps.onFeedPage().feedHistoryModal().close().click();

        basePageSteps.onFeedPage().feedHistoryModal().should(not(isDisplayed()));
    }

}
