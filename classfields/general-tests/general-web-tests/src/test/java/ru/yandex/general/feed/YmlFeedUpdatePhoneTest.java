package ru.yandex.general.feed;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.json.simple.JSONArray;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mock.MockTaskInfo;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.FeedSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.beans.ajaxRequests.UpdateUser.updateUser;
import static ru.yandex.general.beans.ajaxRequests.User.user;
import static ru.yandex.general.beans.feed.FatalError.fatalError;
import static ru.yandex.general.beans.feed.FeedTask.feedTask;
import static ru.yandex.general.consts.GeneralFeatures.FEEDS_FEATURE;
import static ru.yandex.general.consts.Notifications.CHANGES_SAVED;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FEED;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.element.Button.ARIA_DISABLED;
import static ru.yandex.general.element.Button.FALSE;
import static ru.yandex.general.element.Button.TRUE;
import static ru.yandex.general.element.Popup.ADD;
import static ru.yandex.general.element.Popup.CANCEL;
import static ru.yandex.general.mock.MockFeed.feedTemplate;
import static ru.yandex.general.mock.MockFeedErrors.feedErrorsTemplate;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockTasks.getRandomDateTimeInPast;
import static ru.yandex.general.page.FeedPage.ADD_PHONE_NUMBER;
import static ru.yandex.general.page.FeedPage.FAILED;
import static ru.yandex.general.page.FeedPage.PHONE_INPUT;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_USER;
import static ru.yandex.general.utils.Utils.formatPhone;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FEEDS_FEATURE)
@Feature("YML фид")
@DisplayName("Добавляем телефон в YML фиде с ошибками")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class YmlFeedUpdatePhoneTest {

    private static final String PHONE_NUMBER = "+79119214431";
    private static final String PHONE_NUMBER_WITHOUT_SEVEN = "9119214431";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private FeedSteps feedSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private AjaxProxySteps ajaxProxySteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setFeed(feedTemplate().build())
                .setTaskInfo(new MockTaskInfo(
                        feedTask().setTaskId(String.valueOf(getRandomIntInRange(1, 50)))
                                .setStatus(FAILED)
                                .setFinishedAt(getRandomDateTimeInPast())
                                .setFeedStatistics(null)
                                .setFatalErrors(asList(
                                        fatalError().setMessage("Нет номера телефона")
                                                .setDescription("<a href=\"#\" data-action=\"" +
                                                        "set-yml-phone\">Добавить номер телефона</a>")
                                                .setRequiredAction("SetYmlPhone")))).build())
                .setFeedErrors(feedErrorsTemplate().build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();
        urlSteps.testing().path(MY).path(FEED).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками - добавляем телефон")
    public void shouldSeeYmlFeedWithPhoneErrorAddPhone() {
        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_PHONE_NUMBER).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().input(PHONE_INPUT).sendKeys(PHONE_NUMBER);
        feedSteps.onFeedPage().modal().button(ADD).click();
        feedSteps.onFeedPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(UPDATE_USER).withRequestText(
                updateUser().setUser(user().setYmlPhone(formatPhone(PHONE_NUMBER))
                        .setAddresses(new JSONArray()))).shouldExist();
    }


    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками - добавляем телефон, номер начинается не с 7")
    public void shouldSeeYmlFeedWithPhoneErrorAddPhoneWithoutSeven() {
        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_PHONE_NUMBER).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().input(PHONE_INPUT).sendKeys(PHONE_NUMBER_WITHOUT_SEVEN);
        feedSteps.onFeedPage().modal().button(ADD).click();
        feedSteps.onFeedPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(UPDATE_USER).withRequestText(
                updateUser().setUser(user().setYmlPhone(formatPhone(PHONE_NUMBER))
                        .setAddresses(new JSONArray()))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками, кнопка «Добавить» задизейблена без телефона")
    public void shouldSeeYmlFeedWithPhoneErrorAddButtonDisabled() {
        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_PHONE_NUMBER).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());

        feedSteps.onFeedPage().modal().button(ADD).should(hasAttribute(ARIA_DISABLED, TRUE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками, кнопка «Добавить» задизейблена с частичным телефоном, номер начинается с 7")
    public void shouldSeeYmlFeedWithPhoneErrorAddButtonDisabledWithNotFullNumberStartWith7() {
        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_PHONE_NUMBER).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().input(PHONE_INPUT).sendKeys("7911955887");
        feedSteps.wait500MS();

        feedSteps.onFeedPage().modal().button(ADD).should(hasAttribute(ARIA_DISABLED, TRUE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками, кнопка «Добавить» задизейблена с частичным телефоном, номер начинается не с 7")
    public void shouldSeeYmlFeedWithPhoneErrorAddButtonDisabledWithNotFullNumber() {
        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_PHONE_NUMBER).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().input(PHONE_INPUT).sendKeys("911955887");
        feedSteps.wait500MS();

        feedSteps.onFeedPage().modal().button(ADD).should(hasAttribute(ARIA_DISABLED, TRUE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками, добавление телефона, модалка закрывается по кнопке «Отмена»")
    public void shouldSeeYmlFeedWithPhoneErrorAddButtonCloseModal() {
        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_PHONE_NUMBER).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().button(CANCEL).click();
        feedSteps.wait500MS();

        feedSteps.onFeedPage().modal().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками, добавление телефона, модалка закрывается по кнопке «Отмена» с введенным телефоном")
    public void shouldSeeYmlFeedWithPhoneErrorAddButtonCloseModalWithPhone() {
        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_PHONE_NUMBER).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().input(PHONE_INPUT).sendKeys(PHONE_NUMBER);
        feedSteps.wait500MS();
        feedSteps.onFeedPage().modal().button(CANCEL).click();
        feedSteps.wait500MS();

        feedSteps.onFeedPage().modal().should(not(isDisplayed()));
        ajaxProxySteps.setAjaxHandler(UPDATE_USER).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками, добавление телефона, очистка инпута телефона")
    public void shouldSeeYmlFeedWithPhoneErrorClearPhoneInput() {
        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_PHONE_NUMBER).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().input(PHONE_INPUT).sendKeys(PHONE_NUMBER);
        feedSteps.wait500MS();
        feedSteps.onFeedPage().modal().input(PHONE_INPUT).clearInput().click();
        feedSteps.wait500MS();

        feedSteps.onFeedPage().modal().input(PHONE_INPUT).should(hasValue(""));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("YML фид с ошибками, добавление телефона, кнопка «Добавить» дизейблится после очистки")
    public void shouldSeeYmlFeedWithPhoneErrorAddButtonDisabledAfterClearPhoneInput() {
        feedSteps.onFeedPage().errorsTable().get(0).spanLink(ADD_PHONE_NUMBER).click();
        feedSteps.onFeedPage().modal().waitUntil(isDisplayed());
        feedSteps.onFeedPage().modal().input(PHONE_INPUT).sendKeys(PHONE_NUMBER);
        feedSteps.onFeedPage().modal().button(ADD).waitUntil(hasAttribute(ARIA_DISABLED, FALSE));
        feedSteps.onFeedPage().modal().input(PHONE_INPUT).clearInput().click();
        feedSteps.wait500MS();

        feedSteps.onFeedPage().modal().button(ADD).should(hasAttribute(ARIA_DISABLED, TRUE));
    }

}
