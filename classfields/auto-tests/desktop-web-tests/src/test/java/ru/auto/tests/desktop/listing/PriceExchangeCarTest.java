package ru.auto.tests.desktop.listing;

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
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mock.MockGarageCard;
import ru.auto.tests.desktop.mock.MockStub;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.mock.MockGarageCard.CURRENT_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.DREAM_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.EX_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockGarageCards.garageCards;
import static ru.auto.tests.desktop.mock.MockSearchCars.searchOffersCarsExample;
import static ru.auto.tests.desktop.mock.MockStub.sessionAuthUserStub;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARDS;
import static ru.auto.tests.desktop.mock.Paths.SEARCH_CARS;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_22049;
import static ru.auto.tests.desktop.utils.Utils.formatPrice;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.auto.tests.desktop.utils.Utils.getRandomId;
import static ru.auto.tests.desktop.utils.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Попап цены с авто на обмен")
@Epic(LISTING)
@Feature("Попап цены с авто на обмен")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class PriceExchangeCarTest {

    private static final String PRICE_POPUP_TEXT_TEMPLATE = "%s\nХорошая цена\n6 665 $\n · \n5 935 €\n" +
            "19 июня 2020\nНачальная цена\n500 000 ₽\n28 июня 2020\n- 30 000 ₽\n470 000 ₽\nС учётом вашего " +
            "автомобиля\nВаш %s %s\n%s\nОстанется доплатить\n%s\nСтоимость этого " +
            "автомобиля соответствует средней рыночной относительно похожих автомобилей\nПодробней про хорошую цену";

    private static final String PRICE_POPUP_WITHOUT_CURRENT_CAR_TEMPLATE = "%s\nХорошая цена\n6 665 $\n · \n5 935 €\n" +
            "19 июня 2020\nНачальная цена\n500 000 ₽\n28 июня 2020\n- 30 000 ₽\n470 000 ₽\nУзнайте цену с учётом " +
            "вашего автомобиля\nСтоимость этого автомобиля соответствует средней рыночной относительно похожих " +
            "автомобилей\nПодробней про хорошую цену";

    private static final String PRICE_POPUP_WITHOUT_EXCHANGE_TEMPLATE = "%s\nХорошая цена\n6 665 $\n · \n5 935 €\n" +
            "19 июня 2020\nНачальная цена\n500 000 ₽\n28 июня 2020\n- 30 000 ₽\n470 000 ₽\nСтоимость этого автомобиля " +
            "соответствует средней рыночной относительно похожих автомобилей\nПодробней про хорошую цену";

    private final int offerPrice = getRandomBetween(1000000, 2000000);

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_22049);

        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/User"),
                sessionAuthUserStub()
        );

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст попапа цены, в гараже авто мечты, бывшая и 1 текущая")
    public void shouldSeePricePopupTextDreamExAndOneCurrentCars() {
        String markName = getRandomString();
        String modelName = getRandomString();
        int marketPrice = getRandomBetween(300000, 600000);

        mockRule.setStubs(
                searchCarsWithExchange(true),

                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().setCards(
                                        getGarageCar().setCardType(DREAM_CAR),
                                        getGarageCar().setCardType(EX_CAR),
                                        getGarageCar().setCardType(CURRENT_CAR)
                                                .setMarkName(markName)
                                                .setModelName(modelName)
                                                .setMarketPrice(marketPrice)
                                ).build())
        ).create();

        urlSteps.open();

        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.onListingPage().getSale(0).price().priceDownIcon().hover();

        basePageSteps.onListingPage().pricePopup().should(hasText(format(PRICE_POPUP_TEXT_TEMPLATE,
                formatPrice(offerPrice),
                markName,
                modelName,
                formatPrice(marketPrice),
                formatPrice(offerPrice - marketPrice))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст попапа цены, в гараже авто мечты, бывшая и 2 текущие")
    public void shouldSeePricePopupTextDreamExAndTwoCurrentCars() {
        String firstMarkName = getRandomString();
        String firstModelName = getRandomString();
        String secondMarkName = getRandomString();
        String secondModelName = getRandomString();
        int marketPrice = getRandomBetween(300000, 600000);

        mockRule.setStubs(
                searchCarsWithExchange(true),

                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().setCards(
                                        getGarageCar().setCardType(DREAM_CAR),
                                        getGarageCar().setCardType(EX_CAR),
                                        getGarageCar().setCardType(CURRENT_CAR)
                                                .setMarkName(firstMarkName)
                                                .setModelName(firstModelName)
                                                .setMarketPrice(marketPrice),
                                        getGarageCar().setCardType(CURRENT_CAR)
                                                .setMarkName(secondMarkName)
                                                .setModelName(secondModelName)
                                ).build())
        ).create();

        urlSteps.open();

        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.onListingPage().getSale(0).price().priceDownIcon().hover();

        basePageSteps.onListingPage().pricePopup().should(hasText(format(PRICE_POPUP_TEXT_TEMPLATE,
                formatPrice(offerPrice),
                firstMarkName,
                firstModelName,
                formatPrice(marketPrice),
                formatPrice(offerPrice - marketPrice))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст попапа цены, в гараже только авто мечты")
    public void shouldSeePricePopupTextDreamGarageCar() {
        mockRule.setStubs(
                searchCarsWithExchange(true),

                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().setCards(
                                        getGarageCar().setCardType(DREAM_CAR)
                                ).build())
        ).create();

        urlSteps.open();

        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.onListingPage().getSale(0).price().priceDownIcon().hover();

        basePageSteps.onListingPage().pricePopup().should(hasText(format(PRICE_POPUP_WITHOUT_CURRENT_CAR_TEMPLATE,
                formatPrice(offerPrice))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст попапа цены, в гараже только бывшая")
    public void shouldSeePricePopupTextExGarageCar() {
        mockRule.setStubs(
                searchCarsWithExchange(true),

                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().setCards(
                                        getGarageCar().setCardType(EX_CAR).setId(getRandomId())
                                ).build())
        ).create();

        urlSteps.open();

        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.onListingPage().getSale(0).price().priceDownIcon().hover();

        basePageSteps.onListingPage().pricePopup().should(hasText(format(PRICE_POPUP_WITHOUT_CURRENT_CAR_TEMPLATE,
                formatPrice(offerPrice))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст попапа цены, выбираем другое авто из гаража")
    public void shouldChangeCurrentGarageCar() {
        String firstMarkName = getRandomString();
        String firstModelName = getRandomString();
        String secondMarkName = getRandomString();
        String secondModelName = getRandomString();
        int marketPrice = getRandomBetween(300000, 600000);

        mockRule.setStubs(
                searchCarsWithExchange(true),

                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().setCards(
                                        getGarageCar().setCardType(DREAM_CAR),
                                        getGarageCar().setCardType(EX_CAR),
                                        getGarageCar().setCardType(CURRENT_CAR)
                                                .setMarkName(firstMarkName)
                                                .setModelName(firstModelName),
                                        getGarageCar().setCardType(CURRENT_CAR)
                                                .setMarkName(secondMarkName)
                                                .setModelName(secondModelName)
                                                .setMarketPrice(marketPrice)
                                ).build())
        ).create();

        urlSteps.open();

        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.onListingPage().getSale(0).price().priceDownIcon().hover();

        basePageSteps.onListingPage().pricePopup().button(format("%s %s", firstMarkName, firstModelName))
                .waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().exchangeCarPopup().button(format("%s %s", secondMarkName, secondModelName))
                .waitUntil(isDisplayed()).click();

        basePageSteps.onListingPage().pricePopup().should(hasText(format(PRICE_POPUP_TEXT_TEMPLATE,
                formatPrice(offerPrice),
                secondMarkName,
                secondModelName,
                formatPrice(marketPrice),
                formatPrice(offerPrice - marketPrice))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст попапа цены, с машинами в гараже, но без обмена")
    public void shouldSeePricePopupTextWithoutExchange() {
        mockRule.setStubs(
                searchCarsWithExchange(false),

                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().setCards(
                                        getGarageCar().setCardType(DREAM_CAR),
                                        getGarageCar().setCardType(EX_CAR),
                                        getGarageCar().setCardType(CURRENT_CAR)
                                ).build())
        ).create();

        urlSteps.open();

        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.onListingPage().getSale(0).price().priceDownIcon().hover();

        basePageSteps.onListingPage().pricePopup().should(hasText(format(PRICE_POPUP_WITHOUT_EXCHANGE_TEMPLATE,
                formatPrice(offerPrice))));
    }

    private MockGarageCard getGarageCar() {
        return garageCardOffer().setId(getRandomId());
    }

    private MockStub searchCarsWithExchange(boolean isExchange) {
        return stub().withGetDeepEquals(SEARCH_CARS)
                .withResponseBody(
                        searchOffersCarsExample()
                                .setRurPrice(offerPrice)
                                .setExchange(isExchange).getBody());
    }

}
