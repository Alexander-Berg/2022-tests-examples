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
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.beans.ajaxRequests.ChatSettings.chatSettings;
import static ru.yandex.general.beans.ajaxRequests.MarketingCampaignsSettings.marketingCampaignsSettings;
import static ru.yandex.general.consts.GeneralFeatures.CONTACTS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.mock.MockNotificationsSettings.EMAIL;
import static ru.yandex.general.mock.MockNotificationsSettings.PUSH;
import static ru.yandex.general.mock.MockNotificationsSettings.SMS;
import static ru.yandex.general.mock.MockNotificationsSettings.notificationSettingsTemplate;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.ContactsPage.NEWS_AND_ADS;
import static ru.yandex.general.page.ContactsPage.UNREAD_CHAT_MESSAGES;
import static ru.yandex.general.step.AjaxProxySteps.SET_CHAT_SETTINGS;
import static ru.yandex.general.step.AjaxProxySteps.SET_MARKETING_CAMPAIGNS_SETTINGS;

@Epic(CONTACTS_FEATURE)
@Feature("Подписки на нотификации")
@DisplayName("Изменение подписок на нотификации")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class ContactsNotificationsChangeTest {

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
    private AjaxProxySteps ajaxProxySteps;

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        basePageSteps.resize(1920, 1500);
        urlSteps.testing().path(MY).path(CONTACTS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Подписываемся на пуши по «Новости и рекламные рассылки»")
    public void shouldSeeNewsAndAdsPush() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(NEWS_AND_ADS).cell(1).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS)
                .withRequestText(marketingCampaignsSettings(PUSH)).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Подписываемся на email по «Новости и рекламные рассылки»")
    public void shouldSeeNewsAndAdsEmail() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(NEWS_AND_ADS).cell(2).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS)
                .withRequestText(marketingCampaignsSettings(EMAIL)).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Подписываемся на email по «Непрочитанные сообщения в чате»")
    public void shouldSeeChatSettingsEmail() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(UNREAD_CHAT_MESSAGES).cell(2).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS)
                .withRequestText(chatSettings(EMAIL)).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Подписываемся на СМС по «Непрочитанные сообщения в чате»")
    public void shouldSeeChatSettingsSms() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(UNREAD_CHAT_MESSAGES).cell(3).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS)
                .withRequestText(chatSettings(SMS)).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отписываемся от пушей «Новости и рекламные рассылки»")
    public void shouldSeeRemoveNewsAndAdsPush() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setMarketingCampaigns(PUSH).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(NEWS_AND_ADS).cell(1).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS)
                .withRequestText(marketingCampaignsSettings()).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отписываемся от email «Новости и рекламные рассылки»")
    public void shouldSeeRemoveNewsAndAdsEmail() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setMarketingCampaigns(EMAIL).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(NEWS_AND_ADS).cell(2).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS)
                .withRequestText(marketingCampaignsSettings()).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отписываемся от email «Непрочитанные сообщения в чате»")
    public void shouldSeeRemoveChatSettingsEmail() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setChatSettings(EMAIL).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(UNREAD_CHAT_MESSAGES).cell(2).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS)
                .withRequestText(chatSettings()).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отписываемся от СМС по «Непрочитанные сообщения в чате»")
    public void shouldSeeRemoveChatSettingsSms() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setChatSettings(SMS).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(UNREAD_CHAT_MESSAGES).cell(3).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS)
                .withRequestText(chatSettings()).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Подписываемся на пуши по «Новости и рекламные рассылки», при наличии email подписки")
    public void shouldSeeNewsAndAdsPushWithActiveEmail() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setMarketingCampaigns(EMAIL).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(NEWS_AND_ADS).cell(1).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS)
                .withRequestText(marketingCampaignsSettings(PUSH, EMAIL)).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Подписываемся на email по «Новости и рекламные рассылки», при наличии push подписки")
    public void shouldSeeNewsAndAdsEmailWithActivePush() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setMarketingCampaigns(PUSH).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(NEWS_AND_ADS).cell(2).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS)
                .withRequestText(marketingCampaignsSettings(PUSH, EMAIL)).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Подписываемся на email по «Непрочитанные сообщения в чате, при наличии СМС подписки»")
    public void shouldSeeChatSettingsEmailWithActiveSms() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setChatSettings(SMS).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(UNREAD_CHAT_MESSAGES).cell(2).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS)
                .withRequestText(chatSettings(EMAIL, SMS)).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Подписываемся на СМС по «Непрочитанные сообщения в чате, при наличии email подписки»")
    public void shouldSeeChatSettingsSmsWithActiveEmail() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setChatSettings(EMAIL).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(UNREAD_CHAT_MESSAGES).cell(3).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS)
                .withRequestText(chatSettings(EMAIL, SMS)).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отписываемся от пушей «Новости и рекламные рассылки», при наличии email подписки")
    public void shouldSeeRemoveNewsAndAdsPushWithActiveEmail() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setMarketingCampaigns(PUSH, EMAIL).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(NEWS_AND_ADS).cell(1).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS)
                .withRequestText(marketingCampaignsSettings(EMAIL)).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отписываемся от email «Новости и рекламные рассылки», при наличии push подписки")
    public void shouldSeeRemoveNewsAndAdsEmailWithActivePush() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setMarketingCampaigns(PUSH, EMAIL).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(NEWS_AND_ADS).cell(2).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS)
                .withRequestText(marketingCampaignsSettings(PUSH)).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отписываемся от email «Непрочитанные сообщения в чате», при наличии СМС подписки")
    public void shouldSeeRemoveChatSettingsEmailWithActiveSms() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setChatSettings(EMAIL, SMS).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(UNREAD_CHAT_MESSAGES).cell(2).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS)
                .withRequestText(chatSettings(SMS)).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS).shouldNotExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отписываемся от СМС по «Непрочитанные сообщения в чате», при наличии email подписки")
    public void shouldSeeRemoveChatSettingsSmsWithActiveEmail() {
        mockRule.graphqlStub(mockResponse
                .setNotificationSettings(notificationSettingsTemplate().setChatSettings(EMAIL, SMS).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onContactsPage().notificationsTableRow(UNREAD_CHAT_MESSAGES).cell(3).checkboxEmptyLabel().click();

        ajaxProxySteps.setAjaxHandler(SET_CHAT_SETTINGS)
                .withRequestText(chatSettings(EMAIL)).shouldExist();
        ajaxProxySteps.setAjaxHandler(SET_MARKETING_CAMPAIGNS_SETTINGS).shouldNotExist();
    }

}
