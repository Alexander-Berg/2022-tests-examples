package ru.auto.tests.mobile.dealers;

import com.carlosbecker.guice.GuiceModules;
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

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@DisplayName("Карточка дилера - базовые фильтры, все/новые/с пробегом")
@Feature(DEALERS)
public class CardSectionsNotOfficialTest {

    private static final String DEALER_CODE = "/inchcape_certified_moskva/";

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
    public String dealerStatus;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameter(2)
    public String sectionTitle;

    @Parameterized.Parameter(3)
    public String expectedSection;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2} {3}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {DILER, ALL, "С пробегом", USED},
                {DILER, ALL, "Новые", NEW},
                {DILER, USED, "Все", ALL},
                {DILER, USED, "Новые", NEW},
                {DILER, NEW, "Все", ALL},
                {DILER, NEW, "С пробегом", USED}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SalonNotOfficial",
                "desktop/SearchCarsCountDealerIdNotOfficial",
                "desktop/SearchCarsMarkModelFiltersAllDealerIdSeveralMarks",
                "desktop/SearchCarsMarkModelFiltersNewDealerIdSeveralMarks",
                "desktop/SearchCarsMarkModelFiltersUsedDealerIdSeveralMarks",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(dealerStatus).path(CARS).path(section).path(DEALER_CODE).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по секции")
    public void shouldClickSection() {
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().section(sectionTitle));
        urlSteps.testing().path(dealerStatus).path(CARS).path(expectedSection).path(DEALER_CODE).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().info().waitUntil(isDisplayed());
    }
}
