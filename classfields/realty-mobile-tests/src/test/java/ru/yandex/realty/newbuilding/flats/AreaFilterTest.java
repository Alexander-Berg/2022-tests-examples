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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.element.newbuilding.NewBuildingCardFilters.AREA_FROM_ID;
import static ru.yandex.realty.mobile.element.newbuilding.NewBuildingCardFilters.AREA_TO_ID;
import static ru.yandex.realty.utils.UtilsWeb.getNormalArea;

@DisplayName("Фильтры квартир новостройки")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class AreaFilterTest {

    private static final String AREA_MIN = "areaMin";
    private static final String AREA_MAX = "areaMax";

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
    @DisplayName("При переходе по урлу «площадь от» содержит значение")
    public void shouldSeeAreaMinInput() {
        String areaMin = valueOf(getNormalArea());
        urlSteps.testing().newbuildingSiteMock().queryParam(AREA_MIN, areaMin).open();
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingCardPage().sitePlansModal());
        basePageSteps.onNewBuildingCardPage().openExtFiltersV2();
        basePageSteps.onNewBuildingCardPage().extFilters().inputId(AREA_FROM_ID)
                .should(hasValue(areaMin));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("При переходе по урлу  «площадь до» содержит значение")
    public void shouldSeeAreaMaxInput() {
        String areaMax = valueOf(getNormalArea());
        urlSteps.testing().newbuildingSiteMock().queryParam(AREA_MAX, areaMax).open();
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingCardPage().sitePlansModal());
        basePageSteps.onNewBuildingCardPage().openExtFiltersV2();
        basePageSteps.onNewBuildingCardPage().extFilters().inputId(AREA_TO_ID)
                .should(hasValue(areaMax));
    }
}
