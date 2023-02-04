package ru.auto.tests.desktop.dealers;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER_MARK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.QueryParams.SORT;
import static ru.auto.tests.desktop.element.SortBar.SortBy;
import static ru.auto.tests.desktop.element.SortBar.SortBy.DATE_DESC;
import static ru.auto.tests.desktop.element.SortBar.SortBy.NAME_ASC;
import static ru.auto.tests.desktop.element.SortBar.SortBy.PRICE_ASC;
import static ru.auto.tests.desktop.element.SortBar.SortBy.PRICE_DESC;
import static ru.auto.tests.desktop.element.SortBar.SortBy.RUN_ASC;
import static ru.auto.tests.desktop.element.SortBar.SortBy.YEAR_ASC;
import static ru.auto.tests.desktop.element.SortBar.SortBy.YEAR_DESC;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(DEALERS)
@DisplayName("Сортировки")
@Story(DEALER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardSortTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public SortBy sort;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {DATE_DESC},
                {PRICE_ASC},
                {PRICE_DESC},
                {YEAR_DESC},
                {YEAR_ASC},
                {RUN_ASC},
                {NAME_ASC}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/SearchCarsBreadcrumbsMercedes"),
                stub("desktop/Salon"),
                stub("desktop/SearchCarsCountDealerId"),
                stub("desktop/SearchCarsMarkModelFiltersAllDealerIdOneMark"),
                stub("desktop/SearchCarsAllDealerId")
        ).create();

        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path(CARS_OFFICIAL_DEALER).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Проверка сортировок (кроме дефолтной)")
    public void shouldSeeSortTypeInUrl() {
        basePageSteps.onListingPage().sortBar().selectItem("Сортировка", sort.getName());

        urlSteps.path(CARS_OFFICIAL_DEALER_MARK).path(SLASH)
                .replaceParam(SORT, sort.getAlias().toLowerCase())
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().sortBar().select(sort.getName()).should(isDisplayed());
    }
}