package ru.auto.tests.desktop.group;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.element.SortBar.SortBy;
import static ru.auto.tests.desktop.element.SortBar.SortBy.DISCOUNT_DESC;
import static ru.auto.tests.desktop.element.SortBar.SortBy.PRICE_ASC;
import static ru.auto.tests.desktop.element.SortBar.SortBy.PRICE_DESC;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сортировки на группе")
@Feature(AutoruFeatures.GROUP)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
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
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    //@Parameter("Сортировка")
    @Parameterized.Parameter
    public SortBy sort;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {PRICE_ASC},
                {PRICE_DESC},
                {DISCOUNT_DESC}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsMarkModelGroup",
                "desktop/SearchCarsGroupContextGroup",
                "desktop/SearchCarsGroupContextGroupPriceDesc",
                "desktop/SearchCarsGroupContextGroupPriceAsc",
                "desktop/SearchCarsGroupContextGroupMaxDiscount",
                "desktop/SearchCarsGroupContextListing",
                "desktop/SearchCarsGroupComplectations",
                "desktop/ReferenceCatalogCarsComplectations",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsTechParam").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сортировка предложений")
    public void shouldSortOffers() {
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onGroupPage().select("Сортировка"), 0, 100);
        basePageSteps.onGroupPage().selectItem("Сортировка", sort.getName());
        urlSteps.addParam("sort", sort.getAlias().toLowerCase()).shouldNotSeeDiff();
        basePageSteps.onGroupPage().select(sort.getName()).waitUntil(isDisplayed());
        basePageSteps.onGroupPage().groupOffersList().waitUntil(hasSize(greaterThan(0)));
    }
}
