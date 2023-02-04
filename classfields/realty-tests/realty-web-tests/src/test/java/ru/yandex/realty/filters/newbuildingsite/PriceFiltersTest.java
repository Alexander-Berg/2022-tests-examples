package ru.yandex.realty.filters.newbuildingsite;

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
import org.openqa.selenium.Keys;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.NewBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.newbuildingsite.MainFiltersBlock.FROM;
import static ru.yandex.realty.element.newbuildingsite.MainFiltersBlock.TO;

@DisplayName("Базовые фильтры на странице новостройки")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PriceFiltersTest {

    private static final String PRICE_MIN = "priceMin";
    private static final String PRICE_MAX = "priceMax";
    private static final String PRICE_TYPE = "priceType";
    private static final String PER_METER = "PER_METER";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private NewBuildingSteps newBuildingSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        newBuildingSteps.resize(1400, 1600);
        mockRuleConfigurable.mockNewBuilding().createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр цена «от»")
    public void shouldSeePriceMinInUrl() {
        urlSteps.testing().newbuildingSiteMock().open();
        String priceMin = valueOf(getRandomShortInt());
        newBuildingSteps.onNewBuildingSitePage().mainFiltersBlock().priceFilter().input(FROM, priceMin + Keys.ENTER);
        urlSteps.queryParam(PRICE_MIN, priceMin).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр цена «до»")
    public void shouldSeePriceMaxInUrl() {
        urlSteps.testing().newbuildingSiteMock().open();
        String priceMax = valueOf(getRandomShortInt());
        newBuildingSteps.onNewBuildingSitePage().mainFiltersBlock().priceFilter().input(TO, priceMax + Keys.ENTER);
        urlSteps.queryParam(PRICE_MAX, priceMax).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр цена «за м²»")
    public void shouldSeePriceByMeterInUrl() {
        urlSteps.testing().newbuildingSiteMock().open();
        newBuildingSteps.onNewBuildingSitePage().mainFiltersBlock().button("м²").click();
        urlSteps.queryParam(PRICE_TYPE, PER_METER).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка цена «от»")
    public void shouldSeePriceMinButton() {
        String priceMin = valueOf(getRandomShortInt());
        urlSteps.testing().newbuildingSiteMock().queryParam(PRICE_MIN, priceMin).open();
        newBuildingSteps.onNewBuildingSitePage().mainFiltersBlock().priceFilter().input(FROM)
                .should(hasValue(priceMin));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка цена «до»")
    public void shouldSeePriceMaxButton() {
        String priceMax = valueOf(getRandomShortInt());
        urlSteps.testing().newbuildingSiteMock().queryParam(PRICE_MAX, priceMax).open();
        newBuildingSteps.onNewBuildingSitePage().mainFiltersBlock().priceFilter().input(TO).
                should(hasValue(priceMax));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка цена за «м²»")
    public void shouldSeePriceByMeterButton() {
        urlSteps.testing().newbuildingSiteMock().queryParam(PRICE_TYPE, PER_METER).open();
        newBuildingSteps.onNewBuildingSitePage().mainFiltersBlock().button("м²").should(isDisplayed());
    }
}
