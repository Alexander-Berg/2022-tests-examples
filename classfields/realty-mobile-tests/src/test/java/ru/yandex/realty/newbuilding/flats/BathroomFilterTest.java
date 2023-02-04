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

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.mobile.page.NewBuildingCardPage.SHOW;
import static ru.yandex.realty.step.UrlSteps.RGID;

@DisplayName("Фильтры квартир новостройки")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class BathroomFilterTest {

    private static final String BATHROOM_UNIT = "bathroomUnit";
    private static final String MATCHED = "MATCHED";
    private static final String MATCHED_BATHROOM = "совмещенный";
    private static final String SEPARATED_BATHROOM = "раздельный";
    private static final String SEVERAL = "Несколько";
    private static final String SEPARATED = "SEPARATED";
    private static final String TWO_AND_MORE = "TWO_AND_MORE";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private MockRuleConfigurable mockRuleConfigurable;

    @Before
    public void before() {
        mockRuleConfigurable.mockNewBuilding().createWithDefaults();
        basePageSteps.resize(390, 2000);
    }




    @Test
    @Owner(KANTEMIROV)
    @DisplayName("При переходе по урлу «санузел совмещённый» чекнут")
    public void shouldSeeFloorExceptFirstTumbler() {
        urlSteps.testing().newbuildingSiteMock().queryParam(BATHROOM_UNIT, MATCHED).open();
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingCardPage().sitePlansModal());
        basePageSteps.onNewBuildingCardPage().openExtFiltersV2();
        basePageSteps.onNewBuildingCardPage().extFilters().button(MATCHED_BATHROOM).should(isChecked());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("При переходе по урлу «санузел раздельный» чекнут")
    public void shouldSeeLastFloorTumbler() {
        urlSteps.testing().newbuildingSiteMock().queryParam(BATHROOM_UNIT, SEPARATED).open();
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingCardPage().sitePlansModal());
        basePageSteps.onNewBuildingCardPage().openExtFiltersV2();
        basePageSteps.onNewBuildingCardPage().extFilters().button(SEPARATED_BATHROOM).should(isChecked());
    }
}
