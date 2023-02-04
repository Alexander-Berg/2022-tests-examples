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
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.mobile.element.newbuilding.NewBuildingCardFilters.FLOR_FROM_ID;
import static ru.yandex.realty.mobile.element.newbuilding.NewBuildingCardFilters.FLOR_TO_ID;

@DisplayName("Фильтры квартир новостройки")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class FloorFilterTest {

    private static final String FLOOR_MIN = "floorMin";
    private static final String FLOOR_MAX = "floorMax";
    private static final String FLOOR_EXCEPT_FIRST = "floorExceptFirst";
    private static final String YES = "YES";
    private static final String NO = "NO";
    private static final String LAST_NO = "Не последний";
    private static final String LAST = "Последний";
    private static final String LAST_FLOOR = "lastFloor";
    private static final String FIRST_NO = "Не первый";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private MockRuleConfigurable mockRuleConfigurable;

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
    @DisplayName("При переходе по урлу «этаж от» содержит значение")
    public void shouldSeeFloorMinInput() {
        String floorMin = valueOf(getRandomShortInt());
        urlSteps.testing().newbuildingSiteMock().queryParam(FLOOR_MIN, floorMin).open();
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingCardPage().sitePlansModal());
        basePageSteps.onNewBuildingCardPage().openExtFiltersV2();
        basePageSteps.onNewBuildingCardPage().extFilters().inputId(FLOR_FROM_ID)
                .should(hasValue(floorMin));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("При переходе по урлу «этаж до» содержит значение")
    public void shouldSeeFloorMaxInput() {
        String floorMax = valueOf(getRandomShortInt());
        urlSteps.testing().newbuildingSiteMock().queryParam(FLOOR_MAX, floorMax).open();
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingCardPage().sitePlansModal());
        basePageSteps.onNewBuildingCardPage().openExtFiltersV2();
        basePageSteps.onNewBuildingCardPage().extFilters().inputId(FLOR_TO_ID)
                .should(hasValue(floorMax));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("При переходе по урлу «этаж не первый» чекнут")
    public void shouldSeeFloorExceptFirstButton() {
        urlSteps.testing().newbuildingSiteMock().queryParam(FLOOR_EXCEPT_FIRST, YES).open();
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingCardPage().sitePlansModal());
        basePageSteps.onNewBuildingCardPage().openExtFiltersV2();
        basePageSteps.onNewBuildingCardPage().extFilters().button(FIRST_NO).should(isChecked());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("При переходе по урлу «этаж не последний» чекнут")
    public void shouldSeeNoLastFloorButton() {
        urlSteps.testing().newbuildingSiteMock().queryParam(LAST_FLOOR, NO).open();
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingCardPage().sitePlansModal());
        basePageSteps.onNewBuildingCardPage().openExtFiltersV2();
        basePageSteps.onNewBuildingCardPage().extFilters().button(LAST_NO).should(isChecked());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("При переходе по урлу «этаж последний» чекнут")
    public void shouldSeeLastFloorButton() {
        urlSteps.testing().newbuildingSiteMock().queryParam(LAST_FLOOR, YES).open();
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingCardPage().sitePlansModal());
        basePageSteps.onNewBuildingCardPage().openExtFiltersV2();
        basePageSteps.onNewBuildingCardPage().extFilters().button(LAST).should(isChecked());
    }
}
