package ru.auto.tests.mobile.vas;

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
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;

@DisplayName("Листинг - отсутствие услуги «Топ» при сортировке «по году»")
@Feature(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VasTopListingAbsentTest {

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
    public String breadcrumbsMock;

    @Parameterized.Parameter(2)
    public String listingMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/SearchCarsBreadcrumbsEmpty", "mobile/SearchCarsAllSortYearAsc"},
                {TRUCK, "desktop/SearchTrucksBreadcrumbsEmpty", "mobile/SearchTrucksAllSortYearAsc"},
                {MOTORCYCLE, "desktop/SearchMotoBreadcrumbsEmpty", "mobile/SearchMotoAllSortYearAsc"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with(breadcrumbsMock,
                listingMock).post();

        urlSteps.testing().path(MOSKVA).path(category).path(ALL).addParam("sort", "year-asc").open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отсутствие услуги «Топ» при сортировке «по году: старше»")
    public void shouldNotSeeVasSortYear() {
        basePageSteps.onListingPage().topSalesList().should(hasSize(0));
    }
}
