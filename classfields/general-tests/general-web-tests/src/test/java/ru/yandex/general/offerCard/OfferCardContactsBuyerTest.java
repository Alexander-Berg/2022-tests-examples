package ru.yandex.general.offerCard;

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
import ru.yandex.general.mock.MockCard;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature("Способы связи")
@DisplayName("Способы связи")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferCardContactsBuyerTest {

    private static final String ID = "12345";
    private static final String PHONE = "+7 999 469-46-34";
    private static final String PHONE_CALL = "PhoneCall";
    private static final String CHAT = "Chat";
    private static final String ANY = "Any";
    private static final String REDIRECT_PHONE_TITLE = "Номер под защитой";
    private static final String REDIRECT_PHONE_TEXT = "Этот номер защищён от звонков мошенников и спама. Настоящий" +
            " номер пользователя никто не видит, но все звонки переадресуются на него.";

    private MockResponse mockResponse = mockResponse().setCategoriesTemplate().setRegionsTemplate();
    private MockCard mockCard = mockCard(BASIC_CARD).setIsOwner(false);

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

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(CARD).path(ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Только «Написать»")
    public void shouldSeeOnlyChatButton() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setPreferContactWay(CHAT).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().startChat().should(isDisplayed());
        basePageSteps.onOfferCardPage().sidebar().showPhone().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Оба способа связи")
    public void shouldSeeChatAndCallButtons() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setPreferContactWay(ANY).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().startChat().should(isDisplayed());
        basePageSteps.onOfferCardPage().sidebar().showPhone().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Только «Показать телефон»")
    public void shouldSeeOnlyCallButton() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setPreferContactWay(PHONE_CALL).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onOfferCardPage().sidebar().startChat().should(not(isDisplayed()));
        basePageSteps.onOfferCardPage().sidebar().showPhone().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Номер в кнопке «Показать телефон», PreferContactWay = «PhoneCall»")
    public void shouldSeePhoneInCallButtonPreferContactWayPhoneCall() {
        mockRule.graphqlStub(mockResponse
                .setCard(
                        mockCard.setPreferContactWay(PHONE_CALL)
                                .setPhone(PHONE).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().sidebar().showPhone().click();

        basePageSteps.onOfferCardPage().sidebar().showPhone().should(hasText(PHONE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Номер в кнопке «Показать телефон», PreferContactWay = «Any»")
    public void shouldSeePhoneInCallButtonPreferContactWayAny() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setPreferContactWay(ANY).setPhone(PHONE).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().sidebar().showPhone().click();

        basePageSteps.onOfferCardPage().sidebar().showPhone().should(hasText(PHONE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается попап «Номер под защитой», PreferContactWay = «Any»")
    public void shouldSeeRedirectPopupInCallButtonPreferContactWayAny() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setPreferContactWay(ANY)
                        .setPhone(PHONE).setIsRedirectPhone(true).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().sidebar().showPhone().click();

        basePageSteps.onOfferCardPage().sidebar().redirectPhone().should(hasText(PHONE));
        basePageSteps.onOfferCardPage().popup().title().should(hasText(REDIRECT_PHONE_TITLE));
        basePageSteps.onOfferCardPage().popup().text().should(hasText(REDIRECT_PHONE_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается попап «Номер под защитой», PreferContactWay = «PhoneCall»")
    public void shouldSeeRedirectPopupInCallButtonPreferContactWayPhoneCall() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setPreferContactWay(PHONE_CALL)
                        .setPhone(PHONE).setIsRedirectPhone(true).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().sidebar().showPhone().click();

        basePageSteps.onOfferCardPage().sidebar().redirectPhone().should(hasText(PHONE));
        basePageSteps.onOfferCardPage().popup().title().should(hasText(REDIRECT_PHONE_TITLE));
        basePageSteps.onOfferCardPage().popup().text().should(hasText(REDIRECT_PHONE_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается попап «Номер под защитой» для обычного номера, PreferContactWay = «PhoneCall»")
    public void shouldNotSeeRedirectPopupPreferContactWayPhoneCall() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setPreferContactWay(PHONE_CALL)
                        .setPhone(PHONE).setIsRedirectPhone(false).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().sidebar().showPhone().click();

        basePageSteps.onOfferCardPage().popup().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается попап «Номер под защитой» для обычного номера, PreferContactWay = «Any»")
    public void shouldNotSeeRedirectPopupPreferContactWayAny() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setPreferContactWay(ANY)
                        .setPhone(PHONE).setIsRedirectPhone(false).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().sidebar().showPhone().click();

        basePageSteps.onOfferCardPage().popup().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается зелёная кнопка с телефоном для обычного номера, PreferContactWay = «PhoneCall»")
    public void shouldNotSeeGreenPhoneButtonPreferContactWayPhoneCall() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setPreferContactWay(PHONE_CALL)
                        .setPhone(PHONE).setIsRedirectPhone(false).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().sidebar().showPhone().click();

        basePageSteps.onOfferCardPage().sidebar().redirectPhone().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается зелёная кнопка с телефоном для обычного номера, PreferContactWay = «Any»")
    public void shouldNotSeeGreenPhoneButtonPreferContactWayAny() {
        mockRule.graphqlStub(mockResponse
                .setCard(mockCard.setPreferContactWay(ANY)
                        .setPhone(PHONE).setIsRedirectPhone(false).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().sidebar().showPhone().click();

        basePageSteps.onOfferCardPage().sidebar().redirectPhone().should(not(isDisplayed()));
    }

}
