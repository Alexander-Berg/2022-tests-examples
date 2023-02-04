package ru.auto.tests.mobile.listing;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.consts.Regions;
import ru.auto.tests.desktop.mobile.element.SaleListItem;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.OMSK;
import static ru.auto.tests.desktop.consts.Pages.USED;

@DisplayName("Листинг - разные регионы в куке и урле")
@Feature(AutoruFeatures.DEALERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingRegionTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS},
                {LCV},
                {MOTORCYCLE}
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Должны отображаться объявления региона из урла, а не из куки")
    public void shouldSeeSalesFromUrlRegion() {
        cookieSteps.setCookie("gradius", "0", format(".%s", urlSteps.getConfig().getBaseDomain()));
        urlSteps.testing().path(OMSK).path(category).path(USED).open();
        shouldSeeCorrectRegion();
    }

    @Step("Проверяем, какой регион у большинства объявлений в списке")
    private void shouldSeeCorrectRegion() {
        List<SaleListItem> regions = basePageSteps.onListingPage().salesList().stream()
                .filter(sale -> sale.region().getText().contains(Regions.OMSK))
                .collect(toList());
        assertThat("Неправильный регион у объявлений в списке", regions.size(), greaterThan(10));
    }
}
