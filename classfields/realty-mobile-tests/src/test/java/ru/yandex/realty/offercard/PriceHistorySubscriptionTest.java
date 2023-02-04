package ru.yandex.realty.offercard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.SUBSCRIPTIONS;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mobile.element.offercard.PriceHistoryModal.BUTTON_DISABLED;
import static ru.yandex.realty.mobile.element.offercard.PriceHistoryModal.EMAIL;
import static ru.yandex.realty.mobile.element.offercard.PriceHistoryModal.SUBSCRIBE;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;

@DisplayName("Карточка оффера")
@Feature(OFFER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
@Issue("VERTISTEST-1351")
public class PriceHistorySubscriptionTest {

    private MockOffer offer;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        offer = mockOffer(SELL_APARTMENT);
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим пустое поле почты и выключенную кнопку")
    public void shouldSeeEmptyEmail() {
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().priceHistoryButton());
        basePageSteps.scrollElementToCenter(basePageSteps.onOfferCardPage().priceHistoryButton());
        basePageSteps.onOfferCardPage().priceHistoryButton().click();
        basePageSteps.onOfferCardPage().priceHistoryModal().button(SUBSCRIBE)
                .should(hasClass(containsString(BUTTON_DISABLED)));
        basePageSteps.onOfferCardPage().priceHistoryModal().input(EMAIL)
                .should(hasValue(""));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим выключенную кнопку")
    public void shouldSeeEnabledButton() {
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().priceHistoryButton().click();
        basePageSteps.onOfferCardPage().priceHistoryModal().button(SUBSCRIBE)
                .waitUntil(hasClass(containsString(BUTTON_DISABLED)));
        basePageSteps.onOfferCardPage().priceHistoryModal().input(EMAIL, getRandomEmail());
        basePageSteps.onOfferCardPage().priceHistoryModal().button(SUBSCRIBE)
                .should(hasClass(not(containsString(BUTTON_DISABLED))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим сообщение о подписке, ссылка ведет в подписки")
    public void shouldSeeSubscriptionMessage() {
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().priceHistoryButton());
        basePageSteps.scrollElementToCenter(basePageSteps.onOfferCardPage().priceHistoryButton());
        basePageSteps.onOfferCardPage().priceHistoryButton().click();
        basePageSteps.onOfferCardPage().priceHistoryModal().input(EMAIL, getRandomEmail());
        basePageSteps.onOfferCardPage().priceHistoryModal().button(SUBSCRIBE)
                .should(hasClass(not(containsString(BUTTON_DISABLED)))).click();
        basePageSteps.onOfferCardPage().priceHistoryModal().subscriptionMessage().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().priceHistoryModal().subscriptionMessage()
                .link().should(hasHref(containsString(urlSteps.testing().path(SUBSCRIPTIONS).toString())));
    }
}
