package ru.yandex.realty.mappage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.mock.PointStatisticSearchTemplate.pointStatisticSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.NEW_FLAT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.NO_VALUE;

@DisplayName("Карта. Общее")
@Feature(MAP)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MapApartmentSnippetScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String dealType;

    @Parameterized.Parameter(2)
    public String realtyType;

    @Parameterized.Parameters(name = "Клик по сниппету на {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Купить квартиру", KUPIT, KVARTIRA},
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по сниппету -> скриншот боковой панели")
    public void shouldSeeSidebarScreenshot() {
        MockOffer offer = mockOffer(SELL_APARTMENT);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .pointStatisticSearchStub(pointStatisticSearchTemplate().build())
                .createWithDefaults();
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(MOSKVA_I_MO).path(dealType).path(realtyType).path(KARTA)
                .queryParam(NEW_FLAT_URL_PARAM, NO_VALUE).open();
        basePageSteps.onMapPage().offerPieChartList().waitUntil(hasSize(greaterThan(0)), 30);
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).waitUntil(isDisplayed(), 30);

        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMapPage().sidebar());
        urlSteps.setProductionHost().open();

        basePageSteps.onMapPage().offerPieChartList().waitUntil(hasSize(greaterThan(0)), 30);
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        basePageSteps.onMapPage().sidebar().snippetOffer(FIRST).waitUntil(isDisplayed(), 30);
        basePageSteps.onMapPage().sidebar().waitUntil(isDisplayed());
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMapPage().sidebar());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
