package ru.auto.tests.mobile.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mock.MockGarageCard;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockGarageCard.CURRENT_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.DREAM_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.EX_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockGarageCards.garageCards;
import static ru.auto.tests.desktop.mock.MockOffer.CAR_DEALER;
import static ru.auto.tests.desktop.mock.MockOffer.mockOffer;
import static ru.auto.tests.desktop.mock.MockStub.sessionAuthUserStub;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARDS;
import static ru.auto.tests.desktop.mock.Paths.OFFER_CARS;
import static ru.auto.tests.desktop.utils.Utils.formatPrice;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.auto.tests.desktop.utils.Utils.getRandomId;
import static ru.auto.tests.desktop.utils.Utils.getRandomOfferId;
import static ru.auto.tests.desktop.utils.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(AutoruFeatures.SALES)
@Feature("Попап цены, обмен на авто из гаража")
@DisplayName("Попап цены, обмен на авто из гаража")
@GuiceModules(MobileEmulationTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class PricePopupExchangeForGarageCarTest {

    private static final String PRICE_POPUP_TEXT_TEMPLATE = "Цена\n%s\nХорошая цена\n23 446 $\n · \n22 047 €\n" +
            "от 21 269 ₽ / мес.\nС учётом вашего автомобиля\nВаш %s %s\n%s\n" +
            "Останется доплатить\n%s\nО скидках и акциях узнавайте по телефону\nСтоимость этого автомобиля " +
            "соответствует средней рыночной относительно похожих автомобилей\nПодробней про хорошую цену\nПозвонить";

    private static final String PRICE_POPUP_WITHOUT_CURRENT_CAR_TEMPLATE = "Цена\n%s\nХорошая цена\n23 446 $\n · \n" +
            "22 047 €\nот 21 269 ₽ / мес.\nУзнайте цену с учётом вашего автомобиля\nО скидках и акциях узнавайте по " +
            "телефону\nСтоимость этого автомобиля соответствует средней рыночной относительно похожих автомобилей\n" +
            "Подробней про хорошую цену\nПозвонить";


    private static final String SALE_ID = getRandomOfferId();
    private final int offerPrice = getRandomBetween(1000000, 2000000);

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/User"),
                sessionAuthUserStub(),
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(
                                mockOffer(CAR_DEALER).setId(SALE_ID)
                                        .setPrice(offerPrice)
                                        .getResponse())
        );

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).path(SLASH);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст попапа цены, в гараже авто мечты, бывшая и 1 текущая")
    public void shouldSeePricePopupTextDreamExAndOneCurrentCars() {
        String markName = getRandomString();
        String modelName = getRandomString();
        int tradeinPrice = getRandomBetween(300000, 600000);

        mockRule.setStubs(
                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().setCards(
                                        getGarageCar().setCardType(DREAM_CAR),
                                        getGarageCar().setCardType(EX_CAR),
                                        getGarageCar().setCardType(CURRENT_CAR)
                                                .setMarkName(markName)
                                                .setModelName(modelName)
                                                .setTradeinPrice(tradeinPrice)
                                ).build())
        ).create();

        urlSteps.open();

        basePageSteps.onCardPage().price().greatDealBadge().click();

        basePageSteps.onCardPage().popup().should(hasText(format(PRICE_POPUP_TEXT_TEMPLATE,
                formatPrice(offerPrice),
                markName,
                modelName,
                formatPrice(tradeinPrice),
                formatPrice(offerPrice - tradeinPrice))));
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
        int tradeinPrice = getRandomBetween(300000, 600000);

        mockRule.setStubs(
                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().setCards(
                                        getGarageCar().setCardType(DREAM_CAR),
                                        getGarageCar().setCardType(EX_CAR),
                                        getGarageCar().setCardType(CURRENT_CAR)
                                                .setMarkName(firstMarkName)
                                                .setModelName(firstModelName)
                                                .setTradeinPrice(tradeinPrice),
                                        getGarageCar().setCardType(CURRENT_CAR)
                                                .setMarkName(secondMarkName)
                                                .setModelName(secondModelName)
                                ).build())
        ).create();

        urlSteps.open();

        basePageSteps.onCardPage().price().greatDealBadge().click();

        basePageSteps.onCardPage().popup().should(hasText(format(PRICE_POPUP_TEXT_TEMPLATE,
                formatPrice(offerPrice),
                firstMarkName,
                firstModelName,
                formatPrice(tradeinPrice),
                formatPrice(offerPrice - tradeinPrice))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст попапа цены, в гараже только авто мечты")
    public void shouldSeePricePopupTextDreamGarageCar() {
        mockRule.setStubs(
                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().setCards(
                                        getGarageCar().setCardType(DREAM_CAR)
                                ).build())
        ).create();

        urlSteps.open();

        basePageSteps.onCardPage().price().greatDealBadge().click();

        basePageSteps.onCardPage().popup().should(hasText(format(PRICE_POPUP_WITHOUT_CURRENT_CAR_TEMPLATE,
                formatPrice(offerPrice))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст попапа цены, в гараже только бывшая")
    public void shouldSeePricePopupTextExGarageCar() {
        mockRule.setStubs(
                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().setCards(
                                        getGarageCar().setCardType(EX_CAR).setId(getRandomId())
                                ).build())
        ).create();

        urlSteps.open();

        basePageSteps.onCardPage().price().greatDealBadge().click();

        basePageSteps.onCardPage().popup().should(hasText(format(PRICE_POPUP_WITHOUT_CURRENT_CAR_TEMPLATE,
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
        int tradeinPrice = getRandomBetween(300000, 600000);

        mockRule.setStubs(
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
                                                .setTradeinPrice(tradeinPrice)
                                ).build())
        ).create();

        urlSteps.open();

        basePageSteps.onCardPage().price().greatDealBadge().click();

        basePageSteps.onCardPage().popup().button(format("%s %s", firstMarkName, firstModelName))
                .waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().exchangeGarageModal().car(format("%s %s", secondMarkName, secondModelName))
                .waitUntil(isDisplayed()).click();

        basePageSteps.onCardPage().popup().should(hasText(format(PRICE_POPUP_TEXT_TEMPLATE,
                formatPrice(offerPrice),
                secondMarkName,
                secondModelName,
                formatPrice(tradeinPrice),
                formatPrice(offerPrice - tradeinPrice))));
    }

    private MockGarageCard getGarageCar() {
        return garageCardOffer().setId(getRandomId());
    }

}
