package ru.yandex.realty.map;

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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.mock.PointStatisticSearchTemplate.pointStatisticSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Сниппеты на карте.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ClickSnippetTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        MockOffer offer = mockOffer(SELL_APARTMENT);
        mockRuleConfigurable.
                offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .pointStatisticSearchStub(pointStatisticSearchTemplate().build()).createWithDefaults();

        compareSteps.resize(380, 950);
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMobileMapPage().paranja().clickIf(isDisplayed());
        basePageSteps.onMobileMapPage().paranja().waitUntil(not(isDisplayed()));
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().pin(FIRST));
    }

    @Test
    @Category({Regression.class, Mobile.class})
    @Owner(KANTEMIROV)
    @DisplayName("Клик по сниппету")
    public void shouldSeeMapOfferClick() {
        basePageSteps.onMobileMapPage().offer(FIRST).offerLink().should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Mobile.class})
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот сниппета")
    public void shouldSeeMapOfferScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMobileMapPage().pageRoot());
        urlSteps.production().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().pin(FIRST));
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMobileMapPage().pageRoot());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
