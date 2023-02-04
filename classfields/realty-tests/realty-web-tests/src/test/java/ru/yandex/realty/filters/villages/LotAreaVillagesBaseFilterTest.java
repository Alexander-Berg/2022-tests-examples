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
public class LotAreaVillagesBaseFilterTest {

    private static final String LOT = "Участок";
    private static final String LOT_AREA_MAX = "lotAreaMax";
    private static final String LOT_AREA_MIN = "lotAreaMin";
    private static final String TO = "до";
    private static final String FROM = "от";

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
    @DisplayName("Параметр «Участок от»")
    public void shouldSeeLotAreaFrom() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).open();
        basePageSteps.onVillageListing().openExtFilter();
        String spaceMin = valueOf(getRandomShortInt());
        basePageSteps.onOffersSearchPage().extendFilters().byName(LOT).input(FROM).sendKeys(spaceMin);
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam(LOT_AREA_MIN, spaceMin).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Участок от» при переходе по урлу")
    public void shouldSeeLotAreaFromField() {
        String spaceMin = valueOf(getRandomShortInt());
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).queryParam(LOT_AREA_MIN, spaceMin)
                .open();
        basePageSteps.onVillageListing().openExtFilter();
        basePageSteps.onOffersSearchPage().extendFilters().byName(LOT).input(FROM).should(hasValue(spaceMin));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Участок до»")
    public void shouldSeeLotAreaTo() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).open();
        basePageSteps.onVillageListing().openExtFilter();
        String spaceMax = valueOf(getRandomShortInt() + 10);
        basePageSteps.onOffersSearchPage().extendFilters().byName(LOT).input(TO).sendKeys(spaceMax);
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam(LOT_AREA_MAX, spaceMax).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Участок до» при переходе по урлу")
    public void shouldSeeLotAreaToField() {
        String spaceMax = valueOf(getRandomShortInt());
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).queryParam(LOT_AREA_MAX, spaceMax)
                .open();
        basePageSteps.onVillageListing().openExtFilter();
        basePageSteps.onOffersSearchPage().extendFilters().byName(LOT).input(TO).should(hasValue(spaceMax));
    }
}
