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
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Сниппеты на карте.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ClickNewBuildingSnippetTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {

        compareSteps.resize(380, 950);
        urlSteps.testing().path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        basePageSteps.onMobileMapPage().paranja().clickIf(isDisplayed());
        basePageSteps.onMobileMapPage().paranja().waitUntil(not(isDisplayed()));
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().pin(FIRST));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по сниппету новостройки")
    public void shouldSeeNbMapOfferClick() {
        basePageSteps.onMobileMapPage().newBuildingMapOffer(FIRST).offerLink().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот сниппета новостройки")
    public void shouldSeeNbMapOfferScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMobileMapPage().newBuildingMapOffer(FIRST));
        urlSteps.production().path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().pin(FIRST));
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMobileMapPage().newBuildingMapOffer(FIRST));
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
