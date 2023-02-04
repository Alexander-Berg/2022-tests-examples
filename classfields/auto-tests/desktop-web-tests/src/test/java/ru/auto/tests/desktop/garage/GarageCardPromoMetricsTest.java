package ru.auto.tests.desktop.garage;

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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMOS_BLOCK;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockGarageCard.CURRENT_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockGarageCards.garageCards;
import static ru.auto.tests.desktop.mock.MockPromos.promos;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARDS;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_PROMOS;
import static ru.auto.tests.desktop.page.GarageAllPromoPage.COPY_PROMOCODE;
import static ru.auto.tests.desktop.page.GarageAllPromoPage.OGO_INTERESTING;
import static ru.auto.tests.desktop.utils.Utils.getRandomId;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Метрики на разделе «Акции и скидки» в карточке гаража")
@Epic(AutoruFeatures.GARAGE)
@Feature(PROMOS_BLOCK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class GarageCardPromoMetricsTest {

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

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Before
    public void before() {

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(garageCardOffer()
                                .setId(GARAGE_CARD_ID)
                                .setCardType(CURRENT_CAR).getBody()),
                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().setCards(garageCardOffer()).build()),
                stub().withGetDeepEquals(GARAGE_USER_PROMOS)
                        .withResponseBody(
                                promos().getBody())
        ).create();

        urlSteps.testing().path(GARAGE).path(GARAGE_CARD_ID).path(SLASH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика на показ попапа акции")
    public void shouldSeeShowPromoPopupMetric() {
        basePageSteps.onGarageCardPage().promos().superPromoList().get(0).button(OGO_INTERESTING).click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasSiteInfo("{\"garage\":{\"banner\":{\"yandex_market\":{\"popup\":" +
                        "{\"show\":{}}}}}}")
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика на переход по кнопке в попапе акции")
    public void shouldSeeShowPromoPopupButtonClickMetric() {
        basePageSteps.onGarageCardPage().promos().superPromoList().get(0).button(OGO_INTERESTING).click();
        basePageSteps.onGarageCardPage().popup().button("Смотреть подборку").waitUntil(isDisplayed()).click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasSiteInfo("{\"garage\":{\"banner\":{\"yandex_market\":{\"popup\":" +
                        "{\"button_click\":{}}}}}}")
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика на кнопку «Подробнее» в попапа акции")
    public void shouldSeeDetailsPromoPopupMetric() {
        basePageSteps.onGarageCardPage().promos().regularPromoList().get(1).click();
        basePageSteps.onGarageCardPage().popup().button("Подробнее").waitUntil(isDisplayed()).click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasSiteInfo("{\"garage\":{\"banner\":{\"ramk_xs\":{\"popup\":" +
                        "{\"details_click\":{}}}}}}")
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика на кнопку «Скопировать промокод» в попапа акции")
    public void shouldSeeCopyPromoPopupMetric() {
        basePageSteps.onGarageCardPage().promos().regularPromoList().get(1).click();
        basePageSteps.onGarageCardPage().popup().button(COPY_PROMOCODE).waitUntil(isDisplayed()).click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasSiteInfo("{\"garage\":{\"banner\":{\"ramk_xs\":{\"popup\":" +
                        "{\"copy_click\":{}}}}}}")
        ));
    }

}
