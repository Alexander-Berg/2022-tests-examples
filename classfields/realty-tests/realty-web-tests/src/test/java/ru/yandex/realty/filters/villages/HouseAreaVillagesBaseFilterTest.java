package ru.yandex.realty.filters.villages;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Фильтр поиска по коттеджным поселкам.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class HouseAreaVillagesBaseFilterTest {

    private static final String HOUSE_AREA = "Площадь дома";
    private static final String TO = "до";
    private static final String FROM = "от";
    private static final String HOUSE_AREA_MAX = "houseAreaMax";
    private static final String HOUSE_AREA_MIN = "houseAreaMin";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Площадь дома от»")
    public void shouldSeeHouseAreaFrom() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).open();
        basePageSteps.onVillageListing().openExtFilter();
        String spaceMin = valueOf(getRandomShortInt());
        basePageSteps.onOffersSearchPage().extendFilters().byName(HOUSE_AREA).input(FROM).sendKeys(spaceMin);
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam(HOUSE_AREA_MIN, spaceMin).shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Площадь дома от» при переходе по урлу")
    public void shouldSeeHouseAreaFromField() {
        String spaceMin = valueOf(getRandomShortInt());
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).queryParam(HOUSE_AREA_MIN, spaceMin)
                .open();
        basePageSteps.onVillageListing().openExtFilter();
        basePageSteps.onOffersSearchPage().extendFilters().byName(HOUSE_AREA).input(FROM).should(hasValue(spaceMin));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Площадь дома до»")
    public void shouldSeeHouseAreaTo() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).open();
        basePageSteps.onVillageListing().openExtFilter();
        String spaceMax = valueOf(getRandomShortInt() + 10);
        basePageSteps.onOffersSearchPage().extendFilters().byName(HOUSE_AREA).input(TO).sendKeys(spaceMax);
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam(HOUSE_AREA_MAX, spaceMax).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Площадь дома до» при переходе по урлу")
    public void shouldSeeHouseAreaToField() {
        String spaceMin = valueOf(getRandomShortInt());
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).queryParam(HOUSE_AREA_MAX, spaceMin)
                .open();
        basePageSteps.onVillageListing().openExtFilter();
        basePageSteps.onOffersSearchPage().extendFilters().byName(HOUSE_AREA).input(TO).should(hasValue(spaceMin));
    }
}
