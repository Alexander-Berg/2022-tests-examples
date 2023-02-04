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

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMOS_BLOCK;
import static ru.auto.tests.desktop.consts.Notifications.PROMO_COPIED_SUCCESSFULLY;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Urls.M_MARKET_SPECIAL_AUTO_SALE_URL;
import static ru.auto.tests.desktop.mobile.page.GarageAllPromoPage.COPY_PROMOCODE;
import static ru.auto.tests.desktop.mock.MockGarageCard.CURRENT_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockPromos.promos;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_PROMOS;
import static ru.auto.tests.desktop.utils.Utils.getRandomId;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок «Акции и скидки»")
@Epic(AutoruFeatures.GARAGE)
@Feature(PROMOS_BLOCK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GarageCardPromoTest {

    private static final String GARAGE_CARD_ID = getRandomId();

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
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/ReferenceCatalogCarsSuggest"),
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(garageCardOffer()
                                .setId(GARAGE_CARD_ID)
                                .setCardType(CURRENT_CAR).getBody()),
                stub().withGetDeepEquals(GARAGE_USER_PROMOS)
                        .withResponseBody(
                                promos().setUrlForItem(0, M_MARKET_SPECIAL_AUTO_SALE_URL)
                                        .getBody())
        ).create();

        urlSteps.testing().path(GARAGE).path(GARAGE_CARD_ID).path(SLASH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по кнопке в попапе акции")
    public void shouldSeeSuperPromoPopupButtonClick() {
        basePageSteps.onGarageCardPage().promos().items().get(0).click();
        basePageSteps.onGarageCardPage().popup().button("Смотреть подборку").click();

        urlSteps.fromUri(M_MARKET_SPECIAL_AUTO_SALE_URL).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Копируем промокод")
    public void shouldSeeCopyPromo() {
        basePageSteps.onGarageCardPage().promos().items().get(3).click();
        basePageSteps.onGarageCardPage().popup().button(COPY_PROMOCODE).waitUntil(isDisplayed()).click();

        basePageSteps.onGarageAllPromoPage().notifier(PROMO_COPIED_SUCCESSFULLY).should(isDisplayed());
    }

}
