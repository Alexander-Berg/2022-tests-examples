package ru.auto.tests.mobile.garage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mobile.page.GarageCardPage.FOR_SALE;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockOffer.ownerCar;
import static ru.auto.tests.desktop.mock.MockStub.sessionAuthUserStub;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD;
import static ru.auto.tests.desktop.mock.Paths.OFFER_CARS;

@DisplayName("Переход с карточки в гараже на карточку объявления")
@Feature(AutoruFeatures.GARAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GarageGoToSaleTest {

    private static final String SALE_ID = "1076842087-f1e85";
    private static final String GARAGE_CARD_ID = "790117861";
    private static final int PRICE = 520000;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                sessionAuthUserStub(),
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(garageCardOffer()
                                .setId(GARAGE_CARD_ID)
                                .setOfferId(SALE_ID)
                                .setPrice(PRICE).getBody()),
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(ownerCar().getResponse())
        ).create();

        urlSteps.testing().path(GARAGE).path(GARAGE_CARD_ID).path(SLASH).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход с карточки в гараже на карточку объявления")
    public void shouldGoToSaleFromGarageCard() {
        basePageSteps.onGarageCardPage().badge(FOR_SALE).click();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path("mitsubishi").path("lancer").path(SALE_ID).path(SLASH)
                .shouldNotSeeDiff();
    }

}
