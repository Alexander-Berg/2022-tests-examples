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
import static ru.yandex.realty.mobile.element.newbuilding.NewBuildingCardFilters.KITCHENSPACE_FROM_ID;
import static ru.yandex.realty.mobile.element.newbuilding.NewBuildingCardFilters.KITCHENSPACE_TO_ID;

@DisplayName("Фильтры квартир новостройки")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class KitchenSpaceFilterTest {

    private static final String KITCHEN_SPACE_MIN = "kitchenSpaceMin";
    private static final String KITCHEN_SPACE_MAX = "kitchenSpaceMax";

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
    @DisplayName("При переходе по урлу «площадь кухни от» содержит значение")
    public void shouldSeeKitchenSpaceMinInput() {
        String kitchenSpaceMin = valueOf(getRandomShortInt());
        urlSteps.testing().newbuildingSiteMock().queryParam(KITCHEN_SPACE_MIN, kitchenSpaceMin).open();
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingCardPage().sitePlansModal());
        basePageSteps.onNewBuildingCardPage().openExtFiltersV2();
        basePageSteps.onNewBuildingCardPage().extFilters().inputId(KITCHENSPACE_FROM_ID)
                .should(hasValue(kitchenSpaceMin));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("При переходе по урлу  «площадь кухни до» содержит значение")
    public void shouldSeeKitchenSpaceMaxInput() {
        String kitchenSpaceMax = valueOf(getRandomShortInt());
        urlSteps.testing().newbuildingSiteMock().queryParam(KITCHEN_SPACE_MAX, kitchenSpaceMax).open();
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingCardPage().sitePlansModal());
        basePageSteps.onNewBuildingCardPage().openExtFiltersV2();
        basePageSteps.onNewBuildingCardPage().extFilters().inputId(KITCHENSPACE_TO_ID)
                .should(hasValue(kitchenSpaceMax));
    }
}
