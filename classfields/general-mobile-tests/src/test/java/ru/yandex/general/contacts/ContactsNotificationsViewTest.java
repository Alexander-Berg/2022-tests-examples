package ru.yandex.general.contacts;

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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.CONTACTS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.element.Checkbox.CHECKED;
import static ru.yandex.general.element.Checkbox.TRUE;
import static ru.yandex.general.mobile.element.NotificationSettings.EMAIL_ROW;
import static ru.yandex.general.mobile.element.NotificationSettings.PUSH_ROW;
import static ru.yandex.general.mobile.element.NotificationSettings.SMS_ROW;
import static ru.yandex.general.mobile.page.ContactsPage.NEWS_AND_ADS;
import static ru.yandex.general.mobile.page.ContactsPage.NOTIFICATIONS;
import static ru.yandex.general.mobile.page.ContactsPage.UNREAD_CHAT_MESSAGES;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.mock.MockNotificationsSettings.EMAIL;
import static ru.yandex.general.mock.MockNotificationsSettings.PUSH;
import static ru.yandex.general.mock.MockNotificationsSettings.SMS;
import static ru.yandex.general.mock.MockNotificationsSettings.notificationSettingsTemplate;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@Epic(CONTACTS_FEATURE)
@Feature("Подписки на нотификации")
@DisplayName("Отображение подписок на нотификации")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class ContactsNotificationsViewTest {

    private MockResponse mockResponse = mockResponse()
            .setCurrentUser(currentUserExample().build())
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
        urlSteps.testing().path(MY).path(CONTACTS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается активный чекбокс пушей «Новости и рекламные рассылки»")
    public void shouldSeeNewsAndAdsPushCheckbox() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setMarketingCampaigns(PUSH).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().spanLink(NOTIFICATIONS).click();

        basePageSteps.onContactsPage().notificationSettings(NEWS_AND_ADS).row(PUSH_ROW).checkbox()
                .should(hasAttribute(CHECKED, TRUE));
        basePageSteps.onContactsPage().wrapper(NOTIFICATIONS).checkedChecboxList().should(hasSize(1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается активный чекбокс email «Новости и рекламные рассылки»")
    public void shouldSeeNewsAndAdsEmailCheckbox() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setMarketingCampaigns(EMAIL).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().spanLink(NOTIFICATIONS).click();

        basePageSteps.onContactsPage().notificationSettings(NEWS_AND_ADS).row(EMAIL_ROW).checkbox()
                .should(hasAttribute(CHECKED, TRUE));
        basePageSteps.onContactsPage().wrapper(NOTIFICATIONS).checkedChecboxList().should(hasSize(1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается активный чекбокс email «Непрочитанные сообщения в чате»")
    public void shouldSeeChatSettingsEmailCheckbox() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setChatSettings(EMAIL).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().spanLink(NOTIFICATIONS).click();

        basePageSteps.onContactsPage().notificationSettings(UNREAD_CHAT_MESSAGES).row(EMAIL_ROW).checkbox()
                .should(hasAttribute(CHECKED, TRUE));
        basePageSteps.onContactsPage().wrapper(NOTIFICATIONS).checkedChecboxList().should(hasSize(1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается активный чекбокс СМС «Непрочитанные сообщения в чате»")
    public void shouldSeeChatSettingsSmsCheckbox() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setChatSettings(SMS).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().spanLink(NOTIFICATIONS).click();

        basePageSteps.onContactsPage().notificationSettings(UNREAD_CHAT_MESSAGES).row(SMS_ROW).checkbox()
                .should(hasAttribute(CHECKED, TRUE));
        basePageSteps.onContactsPage().wrapper(NOTIFICATIONS).checkedChecboxList().should(hasSize(1));
    }

}
