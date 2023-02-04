package ru.yandex.realty.map;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Сниппеты на карте.")
@Feature(OFFERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ReturnToMapOfferTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String dealType;

    @Parameterized.Parameter(2)
    public String offerType;

    @Parameterized.Parameters(name = "{index} + {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Купить квартиру", KUPIT, KVARTIRA},
                {"Снять квартиру", SNYAT, KVARTIRA},
                {"Снять коммерческую", SNYAT, COMMERCIAL},
        });
    }

    @Test
    @Category({Regression.class, Mobile.class})
    @Owner(KANTEMIROV)
    public void shouldSeeDisplayedOffer() {
        basePageSteps.resize(390, 800);
        urlSteps.testing().path(MOSKVA).path(dealType).path(offerType).path(KARTA).open();
        basePageSteps.onMobileMapPage().paranja().clickIf(isDisplayed());
        basePageSteps.onMobileMapPage().paranja().waitUntil(not(isDisplayed()));
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().pin(FIRST));
        String href = basePageSteps.onMobileMapPage().offer(FIRST).offerLink().getAttribute("href");
        basePageSteps.onMobileMapPage().offer(FIRST).click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.shouldNotDiffWith(href);
        basePageSteps.onOfferCardPage().navButtonBack().click();
        basePageSteps.waitUntilSeeTabsCount(1);
        basePageSteps.switchToTab(0);
        basePageSteps.onMobileMapPage().mapOffersList().should(hasSize(greaterThan(0)));
    }
}
