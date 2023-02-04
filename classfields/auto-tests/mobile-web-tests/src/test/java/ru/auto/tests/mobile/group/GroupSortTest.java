package ru.auto.tests.mobile.group;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.element.listing.SortBar;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.DISCOUNT_DESC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.FRESH_DESC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.PRICE_ASC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.PRICE_DESC;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Групповая карточка - сортировки")
@Feature(AutoruFeatures.GROUP)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GroupSortTest {

    private static final String PATH = "/kia/optima/21342050-21342121/";

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

    @Inject
    public CookieSteps cookieSteps;

    @Parameterized.Parameter
    public SortBar.SortBy sort;

    @Parameterized.Parameter(1)
    public String searchMock;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {PRICE_ASC, "mobile/SearchCarsGroupContextGroupPriceAsc"},
                {PRICE_DESC, "mobile/SearchCarsGroupContextGroupPriceDesc"},
                {DISCOUNT_DESC, "mobile/SearchCarsGroupContextGroupMaxDiscount"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("mobile/SearchCarsBreadcrumbsMarkModelGroup",
                "mobile/SearchCarsGroupContextGroup",
                "mobile/SearchCarsGroupContextListing",
                searchMock).post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }


    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Проверка сортировок")
    public void shouldSort() {
        String firstSaleUrl = basePageSteps.onGroupPage().getSale(0).url().getAttribute("href");
        basePageSteps.onGroupPage().sortBar().sort(format("По %s", FRESH_DESC.getName())).click();
        basePageSteps.onGroupPage().sortPopup().sort(sort.getName()).click();
        basePageSteps.onGroupPage().sortPopup().waitUntil(not(isDisplayed()));
        urlSteps.addParam("sort", sort.getAlias()).shouldNotSeeDiff();
        basePageSteps.onGroupPage().salesList().waitUntil(hasSize(12));
        basePageSteps.onGroupPage().salesList().get(0).should(not(hasAttribute("href", firstSaleUrl)));
        cookieSteps.shouldNotSeeCookie("listing_view_session");
    }
}
