package ru.auto.tests.desktop.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

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
import static ru.auto.tests.desktop.element.SortBar.SortBy.FRESH_DESC;
import static ru.auto.tests.desktop.element.SortBar.SortBy.PRICE_DESC;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(DEALERS)
@DisplayName("Карточка дилера - Сортировки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CardSortDefaultTest {

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

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/SearchCarsBreadcrumbsMercedes"),
                stub("desktop/Salon"),
                stub("desktop/SearchCarsCountDealerId"),
                stub("desktop/SearchCarsMarkModelFiltersAllDealerIdOneMark"),
                stub("desktop/SearchCarsAllDealerId"),
                stub("desktop/SearchCarsAllDealerIdSortPriceDesc")
        ).create();

        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL)
                .path(CARS_OFFICIAL_DEALER).addParam(SORT, PRICE_DESC.getAlias())
                .open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Проверка дефолтной сортировки")
    public void shouldSeeDefaultSortTypeInUrl() {
        basePageSteps.onListingPage().sortBar().selectItem(PRICE_DESC.getName(), FRESH_DESC.getName());

        urlSteps.path(CARS_OFFICIAL_DEALER_MARK).path(SLASH)
                .replaceParam(SORT, FRESH_DESC.getAlias().toLowerCase())
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().sortBar().select("Сортировка").should(isDisplayed());
    }
}