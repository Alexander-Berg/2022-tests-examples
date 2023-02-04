package ru.auto.tests.mobile.garage;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMOS_PAGE;
import static ru.auto.tests.desktop.consts.Notifications.PROMO_COPIED_SUCCESSFULLY;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.Urls.FITAUTO_URL;
import static ru.auto.tests.desktop.consts.Urls.M_MARKET_SPECIAL_AUTO_SALE_URL;
import static ru.auto.tests.desktop.mobile.page.GarageAllPromoPage.COPY_PROMOCODE;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockGarageCards.garageCards;
import static ru.auto.tests.desktop.mock.MockPromos.promos;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARDS;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_PROMOS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Страничка «Акции и скидки»")
@Epic(AutoruFeatures.GARAGE)
@Feature(PROMOS_PAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GarageAllPromoTest {

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
                stub("desktop/SessionAuthUser"),
                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().setCards(garageCardOffer()).build()),
                stub().withGetDeepEquals(GARAGE_USER_PROMOS)
                        .withResponseBody(
                                promos().setUrlForItem(0, M_MARKET_SPECIAL_AUTO_SALE_URL)
                                        .setUrlForItem(2, FITAUTO_URL)
                                        .getBody())
        ).create();

        urlSteps.testing().path(GARAGE).path(PROMO).path(ALL).open();
    }


    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по кнопке в попапе акции, супер промо")
    public void shouldSeeSuperPromoPopupButtonClick() {
        basePageSteps.onGarageAllPromoPage().superPromoList().get(0).click();
        basePageSteps.onGarageAllPromoPage().popup().button("Смотреть подборку").waitUntil(isDisplayed()).click();

        urlSteps.fromUri(M_MARKET_SPECIAL_AUTO_SALE_URL).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по кнопке в попапе акции, обычное промо")
    public void shouldSeeRegularPromoPopupButtonClick() {
        basePageSteps.onGarageAllPromoPage().regularPromoList().get(0).click();
        basePageSteps.onGarageAllPromoPage().popup().button("Записаться").waitUntil(isDisplayed()).click();

        urlSteps.fromUri(FITAUTO_URL).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Копируем промокод")
    public void shouldSeeCopyPromo() {
        basePageSteps.onGarageAllPromoPage().regularPromoList().get(1).click();
        basePageSteps.onGarageAllPromoPage().popup().button(COPY_PROMOCODE).waitUntil(isDisplayed()).click();

        basePageSteps.onGarageAllPromoPage().notifier(PROMO_COPIED_SUCCESSFULLY).should(isDisplayed());
    }

}
