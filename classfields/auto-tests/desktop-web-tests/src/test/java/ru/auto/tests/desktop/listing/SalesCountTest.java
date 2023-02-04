package ru.auto.tests.desktop.listing;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mobile.page.ListingPage.SALES_PER_PAGE;
import static ru.auto.tests.desktop.mobile.page.ListingPage.SALES_PER_PAGE_OLD;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Листинг - количество объявлений")
@Feature(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SalesCountTest {

    private int salesCount;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS},
                {TRUCK},
                {MOTORCYCLE}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(USED).open();
        salesCount = basePageSteps.onListingPage().filter().getFoundOffers();
        basePageSteps.onListingPage().salesList().waitUntil(anyOf(hasSize(SALES_PER_PAGE_OLD),
                hasSize(SALES_PER_PAGE_OLD + 1)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Количество страниц должно соответствовать количеству найденных объявлений на кнопке «Показать»")
    public void shouldSeeTheSameSalesCount() {
        basePageSteps.onListingPage().pager().lastPage().should(hasText(getLastPage()));
    }

    @Step("Получаем последнюю страницу")
    private String getLastPage() {
        int lastPage = (int) Math.ceil((double) salesCount / SALES_PER_PAGE_OLD);
        return lastPage > 99 ? "99" : String.valueOf(lastPage);
    }
}
