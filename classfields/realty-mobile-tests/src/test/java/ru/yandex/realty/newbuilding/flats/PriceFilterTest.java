package ru.yandex.realty.newbuilding.flats;

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
import ru.yandex.realty.mobile.page.NewBuildingCardPage;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.mobile.page.NewBuildingCardPage.FROM;
import static ru.yandex.realty.mobile.page.NewBuildingCardPage.TO;
import static ru.yandex.realty.step.UrlSteps.RGID;

@DisplayName("Фильтры квартир новостройки")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class PriceFilterTest {

    private static final String PRICE_MIN = "priceMin";
    private static final String PRICE_MAX = "priceMax";
    private static final String PER_METER = "PER_METER";
    private static final String PRICE_TYPE = "priceType";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRuleConfigurable.mockNewBuilding().createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("При переходе по урлу «цена от» содержит значение")
    public void shouldSeePriceMinInput() {
        String priceMin = valueOf(getRandomShortInt());
        urlSteps.testing().newbuildingSiteMock().queryParam(PRICE_MIN, priceMin).open();
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingCardPage().filters());
        basePageSteps.onNewBuildingCardPage().filters().input(FROM).should(hasValue(priceMin));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("При переходе по урлу  «цена до» содержит значение")
    public void shouldSeePriceMaxInput() {
        String priceMax = valueOf(getRandomShortInt());
        urlSteps.testing().newbuildingSiteMock().queryParam(PRICE_MAX, priceMax).open();
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingCardPage().filters());
        basePageSteps.onNewBuildingCardPage().filters().input(TO).should(hasValue(priceMax));
    }

}
