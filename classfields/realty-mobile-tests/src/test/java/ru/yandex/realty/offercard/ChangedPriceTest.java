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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;

@DisplayName("Карточка оффера")
@Feature(OFFER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
@Issue("VERTISTEST-1351")
public class ChangedPriceTest {

    //from size "mock/card/prices.json"
    public static final int PRICES_SIZE = 3;

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

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        offer = mockOffer(SELL_APARTMENT);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Цена оффера поменялася стрелка вверх")
    public void shouldSeeIncreasedPrice() {
        offer.setIncreasedPrice();
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().priceIncreased().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Цена оффера поменялася стрелка вниз")
    public void shouldSeeDecreasedPrice() {
        offer.setDecreasedPrice();
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().priceDecreased().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Истотрия изменений содержит несколько записей")
    public void shouldSeePriceHistory() {
        offer.setDecreasedPrice();
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().priceHistoryButton().click();
        basePageSteps.onOfferCardPage().priceHistoryModal().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().priceHistoryModal().priceHistoryItems().should(hasSize(PRICES_SIZE));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Закрываем модуль истотрии изменений цены")
    public void shouldNotSeePriceHistory() {
        offer.setDecreasedPrice();
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().priceHistoryButton().click();
        basePageSteps.onOfferCardPage().priceHistoryModal().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().priceHistoryModal().closeCrossHeader().click();
        basePageSteps.onOfferCardPage().priceHistoryModal().should(not(exists()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скрин истории изменений")
    public void shouldSeePriceHistoryScreenShot() {
        offer.setIncreasedPrice();
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();
        compareSteps.resize(380, 2000);
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().priceHistoryButton().click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().pageRoot());
        urlSteps.setMobileProductionHost().open();
        basePageSteps.onOfferCardPage().priceHistoryButton().click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().pageRoot());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
