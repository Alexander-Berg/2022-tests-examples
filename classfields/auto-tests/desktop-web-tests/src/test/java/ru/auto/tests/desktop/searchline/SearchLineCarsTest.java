package ru.auto.tests.desktop.searchline;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SEARCHLINE;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Строка поиска в разных категориях")
@Feature(SEARCHLINE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SearchLineCarsTest {

    private String searchText;
    private String suggestText;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsAll").post();

        urlSteps.testing().path(CARS).path(ALL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поиск новых авто")
    public void shouldSearchNewCars() {
        mockRule.with("desktop/SearchlineSuggestCarsNew",
                "desktop/SearchCarsNew").update();

        searchText = "Новые";
        suggestText = "Автомобили\nНовые";
        basePageSteps.onListingPage().header().searchLine().input("Поиск по объявлениям", searchText);
        basePageSteps.onListingPage().header().searchLine().suggest().getItem(0).waitUntil(isDisplayed())
                .should(hasText(suggestText)).hover().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).addParam("query", searchText)
                .addParam("from", "searchline").shouldNotSeeDiff();
        basePageSteps.onListingPage().header().searchLine().input(searchText).should(isDisplayed());
        basePageSteps.onListingPage().salesList().should(hasSize(2));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поиск марки")
    public void shouldSearchMark() {
        mockRule.with("desktop/SearchlineSuggestCarsToyota",
                "desktop/SearchCarsAllToyota").update();

        searchText = "toyota";
        suggestText = "Toyota\nАвтомобили";
        basePageSteps.onListingPage().header().searchLine().input("Поиск по объявлениям", searchText);
        basePageSteps.onListingPage().header().searchLine().suggest().getItem(0).waitUntil(isDisplayed())
                .should(hasText(suggestText)).hover().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(searchText).path(ALL).addParam("query", searchText)
                .addParam("from", "searchline").shouldNotSeeDiff();
        basePageSteps.onListingPage().header().searchLine().input(searchText).should(isDisplayed());
        basePageSteps.onListingPage().salesList().should(hasSize(1));
    }
}