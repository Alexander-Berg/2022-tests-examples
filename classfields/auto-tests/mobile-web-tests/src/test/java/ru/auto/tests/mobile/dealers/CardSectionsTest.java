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
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER_MARK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@DisplayName("Карточка дилера - базовые фильтры, все/новые/с пробегом")
@Feature(DEALERS)
public class CardSectionsTest {

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
                {DILER_OFICIALNIY, ALL, "С пробегом", USED},
                {DILER_OFICIALNIY, ALL, "Новые", NEW},
                {DILER_OFICIALNIY, USED, "Все", ALL},
                {DILER_OFICIALNIY, USED, "Новые", NEW},
                {DILER_OFICIALNIY, NEW, "Все", ALL},
                {DILER_OFICIALNIY, NEW, "С пробегом", USED}
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

        urlSteps.testing().path(dealerStatus).path(CARS).path(section).path(CARS_OFFICIAL_DEALER).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по секции")
    public void shouldClickSection() {
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().section(sectionTitle));
        urlSteps.testing().path(dealerStatus).path(CARS).path(expectedSection).path(CARS_OFFICIAL_DEALER)
                .path(CARS_OFFICIAL_DEALER_MARK).path("/").shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().info().waitUntil(isDisplayed());
    }
}
