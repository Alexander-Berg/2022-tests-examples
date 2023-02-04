package ru.yandex.general.contacts;

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
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.general.utils.Utils;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.beans.ajaxRequests.UpdateUser.updateUser;
import static ru.yandex.general.beans.ajaxRequests.User.user;
import static ru.yandex.general.consts.GeneralFeatures.CONTACTS_FEATURE;
import static ru.yandex.general.consts.Notifications.CHANGES_SAVED;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.mock.MockCurrentUser.currentUserTemplate;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.ContactsPage.PHONE_FOR_YML;
import static ru.yandex.general.page.ContactsPage.PHONE_FOR_YML_TOOLTIP;
import static ru.yandex.general.step.AjaxProxySteps.UPDATE_USER;
import static ru.yandex.general.utils.Utils.formatPhone;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasValue;

@Epic(CONTACTS_FEATURE)
@Feature("Телефон YML фида")
@DisplayName("Телефон YML фида")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class ContactsYmlPhoneTest {

    private static final String PHONE_1 = "+79119214431";
    private static final String PHONE_2 = "+79219581758";
    private static final String PHONE_CITY = "8 (495) 640-64-04";
    private static final String HINT = "Укажите номер телефона, чтобы вам могли позвонить";

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

    @Inject
    private AjaxProxySteps ajaxProxySteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(CONTACTS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается блок ввода телефона для YML фидов c ymlPhone.enabled = true")
    public void shouldSeeYmlPhoneBlock() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserExample().ymlEnabled(true).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onContactsPage().field(PHONE_FOR_YML).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается блок ввода телефона для YML фидов c ymlPhone.enabled = false")
    public void shouldNotSeeYmlPhoneBlock() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserExample().ymlEnabled(false).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onContactsPage().field(PHONE_FOR_YML).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается блок ввода телефона для YML фидов c ymlPhone.enabled = true")
    public void shouldSeeYmlPhoneTooltip() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserExample().ymlEnabled(true).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().ymlPhoneInfoIcon().hover();

        basePageSteps.onContactsPage().popup().should(hasText(PHONE_FOR_YML_TOOLTIP));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается телефон для YML фидов")
    public void shouldSeeYmlPhone() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserExample().ymlEnabled(true).setYmlPhone(PHONE_1).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onContactsPage().field(PHONE_FOR_YML).input().should(hasValue(Utils.formatPhone(PHONE_1)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("При вводе 7 в поле телефона, она заменяется на +7")
    public void shouldSeeSevenChangeToPlusSeven() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserExample().ymlEnabled(true).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().field(PHONE_FOR_YML).input().sendKeys("79");

        basePageSteps.onContactsPage().field(PHONE_FOR_YML).input().should(hasValue("+7 9"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("При вводе 8 в поле телефона, она заменяется на +7")
    public void shouldSeeEightChangeToPlusSeven() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserExample().ymlEnabled(true).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().field(PHONE_FOR_YML).input().sendKeys("89");

        basePageSteps.onContactsPage().field(PHONE_FOR_YML).input().should(hasValue("+7 9"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("При вводе не 7 в поле телефона, перед цифрой подставляется +7")
    public void shouldSeeNotSevenAddPlusSeven() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserExample().ymlEnabled(true).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().field(PHONE_FOR_YML).input().sendKeys("9");

        basePageSteps.onContactsPage().field(PHONE_FOR_YML).input().should(hasValue("+79"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем телефон для YML фидов, проверяем запрос /updateUser")
    public void shouldSetYmlPhone() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserTemplate().ymlEnabled(true).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().field(PHONE_FOR_YML).input().sendKeys(PHONE_1);
        basePageSteps.onContactsPage().ymlPhoneInfoIcon().click();
        basePageSteps.onContactsPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(UPDATE_USER).withRequestText(updateUser().setUser(
                user().setYmlPhone(formatPhone(PHONE_1)).setAddresses(new JSONArray()))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем городской телефон для YML фидов, проверяем запрос /updateUser")
    public void shouldSetCityYmlPhone() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserTemplate().ymlEnabled(true).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().field(PHONE_FOR_YML).input().sendKeys(PHONE_CITY);
        basePageSteps.onContactsPage().ymlPhoneInfoIcon().click();
        basePageSteps.onContactsPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(UPDATE_USER).withRequestText(updateUser().setUser(
                user().setYmlPhone(formatPhone(PHONE_CITY).replace("8", "+7"))
                        .setAddresses(new JSONArray()))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Изменяем телефон для YML фидов, проверяем запрос /updateUser")
    public void shouldChangeYmlPhone() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserTemplate().ymlEnabled(true).setYmlPhone(PHONE_1).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().field(PHONE_FOR_YML).input().clearInput().click();
        basePageSteps.onContactsPage().field(PHONE_FOR_YML).input().sendKeys(PHONE_2);
        basePageSteps.onContactsPage().ymlPhoneInfoIcon().click();
        basePageSteps.onContactsPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(UPDATE_USER).withRequestText(updateUser().setUser(
                user().setYmlPhone(formatPhone(PHONE_2)).setAddresses(new JSONArray()))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Очищаем телефон для YML фидов, проверяем запрос /updateUser")
    public void shouldClearYmlPhone() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserTemplate().ymlEnabled(true).setYmlPhone(PHONE_1).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().field(PHONE_FOR_YML).input().clearInput().click();
        basePageSteps.onContactsPage().ymlPhoneInfoIcon().click();
        basePageSteps.onContactsPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(UPDATE_USER).withRequestText(updateUser().setUser(
                user().setYmlPhone("").setAddresses(new JSONArray()))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем не полный телефон для YML фидов, /updateUser не отправляется")
    public void shouldSetNotFullYmlPhone() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserTemplate().ymlEnabled(true).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().field(PHONE_FOR_YML).input().sendKeys("749522");
        basePageSteps.onContactsPage().ymlPhoneInfoIcon().click();

        ajaxProxySteps.setAjaxHandler(UPDATE_USER).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем не полный телефон для YML фидов, отображается ошибка")
    public void shouldSeeErrorAfterSetNotFullYmlPhone() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserTemplate().ymlEnabled(true).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().field(PHONE_FOR_YML).input().sendKeys("749522");
        basePageSteps.onContactsPage().ymlPhoneInfoIcon().click();

        basePageSteps.onContactsPage().field(PHONE_FOR_YML).inputHint(HINT).should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем телефон для YML фидов с кол-вом цифр на 1 больше, проверяем запрос /updateUser")
    public void shouldSetYmlPhoneWithMoreDigits() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserTemplate().ymlEnabled(true).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().field(PHONE_FOR_YML).input().sendKeys(PHONE_1 + "2");
        basePageSteps.onContactsPage().ymlPhoneInfoIcon().click();
        basePageSteps.onContactsPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(UPDATE_USER).withRequestText(updateUser().setUser(
                user().setYmlPhone(formatPhone(PHONE_1)).setAddresses(new JSONArray()))).shouldExist();
    }

}
