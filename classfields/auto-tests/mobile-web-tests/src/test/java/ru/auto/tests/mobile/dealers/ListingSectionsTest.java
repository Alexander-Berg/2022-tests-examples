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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;

@DisplayName("Листинг дилеров - секции")
@Feature(DEALERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingSectionsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String startUrl;

    @Parameterized.Parameter(1)
    public String startSectionTitle;

    @Parameterized.Parameter(2)
    public String sectionTitle;

    @Parameterized.Parameter(3)
    public String sectionUrl;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {NEW, "Новые", "Все", ALL},
                {NEW, "Новые", "С пробегом", USED},
                {ALL, "Все", "Новые", NEW},
                {ALL, "Все", "С пробегом", USED},
                {USED, "С пробегом", "Все", ALL},
                {USED, "С пробегом", "Новые", NEW}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(DILERY).path(CARS).path(startUrl).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор секции")
    public void shouldSelectSection() {
        basePageSteps.onDealersListingPage().filters().section(sectionTitle).click();
        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(sectionUrl).ignoreParam("cookiesync")
                .shouldNotSeeDiff();
        basePageSteps.onDealersListingPage().dealersList().waitUntil(hasSize(greaterThan(0)));
    }
}
