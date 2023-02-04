package ru.auto.tests.mobile.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER_MARK;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка дилера - параметры - тумблеры")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardParamsTogglesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameter(2)
    public String paramName;

    @Parameterized.Parameter(3)
    public String paramQueryName;

    @Parameterized.Parameter(4)
    public String paramQueryValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {3}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "В кредит", "on_credit", "true"},
                {CARS, USED, "В кредит", "on_credit", "true"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/Salon",
                "mobile/SearchCarsCountDealerId",
                "mobile/SearchCarsMarkModelFiltersAllDealerIdOneMark",
                "mobile/SearchCarsMarkModelFiltersNewDealerIdOneMark",
                "mobile/SearchCarsMarkModelFiltersUsedDealerIdOneMark",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(DILER).path(category).path(section).path(CARS_OFFICIAL_DEALER).open();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().paramsButton());
        basePageSteps.hideApplyFiltersButton();
    }

    @Test
    @DisplayName("Тумблеры")
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    public void shouldClickToggle() {
        basePageSteps.onDealerCardPage().paramsPopup().inactiveToggle(paramName).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onDealerCardPage().paramsPopup().applyFiltersButton().click();
        urlSteps.path(CARS_OFFICIAL_DEALER_MARK).path("/").addParam(paramQueryName, paramQueryValue).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filters().should(isDisplayed());
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().paramsButton());
        basePageSteps.onDealerCardPage().paramsPopup().activeToggle(paramName).click();
        basePageSteps.onDealerCardPage().paramsPopup().applyFiltersButton().click();
        urlSteps.testing().path(DILER).path(category).path(section).path(CARS_OFFICIAL_DEALER)
                .path(CARS_OFFICIAL_DEALER_MARK).path("/").shouldNotSeeDiff();
    }
}
