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
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMOS_PAGE;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.QueryParams.SOURCE;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockGarageCard.CURRENT_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.DREAM_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockGarageCards.garageCards;
import static ru.auto.tests.desktop.mock.MockPromos.promos;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARDS;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_PROMOS;
import static ru.auto.tests.desktop.page.GarageAllPromoPage.COPY_PROMOCODE;
import static ru.auto.tests.desktop.page.GarageAllPromoPage.OGO_INTERESTING;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Метрики на страничке «Акции и скидки»")
@Epic(AutoruFeatures.GARAGE)
@Feature(PROMOS_PAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class GarageAllPromoMetricsTest {

    private static final String GARAGE_SOURCE_TEMPLATE =
            "{\"garage\":{\"promo_all\":{\"open\":{\"source\":{\"%s\":{}}}}}}";
    private static final String ORGANIC = "organic";

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
    public SeleniumMockSteps browserMockSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().setCards(garageCardOffer()).build()),
                stub().withGetDeepEquals(GARAGE_USER_PROMOS)
                        .withResponseBody(
                                promos().getBody())
        ).create();

        urlSteps.testing().path(GARAGE).path(PROMO).path(ALL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика на показ попапа акции")
    public void shouldSeeShowPromoPopupMetric() {
        basePageSteps.onGarageAllPromoPage().superPromoList().get(0).button(OGO_INTERESTING).click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasSiteInfo("{\"garage\":{\"promo_all\":{\"yandex_market\":" +
                        "{\"popup\":{\"show\":{}}}}}}")
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика на переход по кнопке в попапе акции")
    public void shouldSeeShowPromoPopupButtonClickMetric() {
        basePageSteps.onGarageAllPromoPage().superPromoList().get(0).button(OGO_INTERESTING).click();
        basePageSteps.onGarageAllPromoPage().popup().button("Смотреть подборку").waitUntil(isDisplayed()).click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasSiteInfo("{\"garage\":{\"promo_all\":{\"yandex_market\":" +
                        "{\"popup\":{\"button_click\":{}}}}}}")
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика на кнопку «Подробнее» в попапе акции")
    public void shouldSeeDetailsPromoPopupMetric() {
        basePageSteps.onGarageAllPromoPage().regularPromoList().get(1).click();
        basePageSteps.onGarageAllPromoPage().popup().button("Подробнее").waitUntil(isDisplayed()).click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasSiteInfo("{\"garage\":{\"promo_all\":{\"ramk_xs\":" +
                        "{\"popup\":{\"details_click\":{}}}}}}")
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика на кнопку «Скопировать промокод» в попапе акции")
    public void shouldSeeCopyPromoPopupMetric() {
        basePageSteps.onGarageAllPromoPage().regularPromoList().get(1).click();
        basePageSteps.onGarageAllPromoPage().popup().button(COPY_PROMOCODE).waitUntil(isDisplayed()).click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasSiteInfo("{\"garage\":{\"promo_all\":{\"ramk_xs\":" +
                        "{\"popup\":{\"copy_click\":{}}}}}}")
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика с типом юзера при открытии странички «Акции и скидки»")
    public void shouldSeePromoOpenUserAuthMetric() {
        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasSiteInfo("{\"garage\":{\"promo_all\":{\"open\":{\"user\":{\"auth\":{}}}}}}")
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «source = current_car» при открытии странички «Акции и скидки» с параметром source в URL")
    public void shouldSeeSourceCurrentCarMetric() {
        urlSteps.testing().path(GARAGE).path(PROMO).path(ALL).addParam(SOURCE, CURRENT_CAR.toLowerCase()).open();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasSiteInfo(format(GARAGE_SOURCE_TEMPLATE, CURRENT_CAR.toLowerCase()))
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «source = dream_car» при открытии странички «Акции и скидки» с параметром source в URL")
    public void shouldSeeSourceDreamCarMetric() {
        urlSteps.testing().path(GARAGE).path(PROMO).path(ALL).addParam(SOURCE, DREAM_CAR.toLowerCase()).open();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasSiteInfo(format(GARAGE_SOURCE_TEMPLATE, DREAM_CAR.toLowerCase()))
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Метрика «source = organic» при открытии странички «Акции и скидки» без параметра source в URL")
    public void shouldSeeSourceOrganicMetric() {
        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasSiteInfo(format(GARAGE_SOURCE_TEMPLATE, ORGANIC))
        ));
    }

}
