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
import org.openqa.selenium.Keys;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SEARCHLINE;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Строка поиска")
@Feature(SEARCHLINE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SearchLineTest {

    public static final String MARK = "BMW";

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
                "desktop/SearchlineSuggest").post();

        urlSteps.testing().open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поиск")
    public void shouldSearch() {
        basePageSteps.onMainPage().header().searchLine().input("Поиск по объявлениям", MARK);
        basePageSteps.onMainPage().header().searchLine().input("Поиск по объявлениям").sendKeys(Keys.ENTER);
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK.toLowerCase()).path(ALL).addParam("query", MARK)
                .addParam("from", "searchline").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поиск через подсказки")
    public void shouldSearchViaSuggest() {
        basePageSteps.onMainPage().header().searchLine().input("Поиск по объявлениям", MARK);
        basePageSteps.onMainPage().header().searchLine().suggest().itemsList().should(hasSize(3));
        basePageSteps.onMainPage().header().searchLine().suggest().getItem(1)
                .should(hasText(format("%s\nМотоциклы", MARK)));
        basePageSteps.onMainPage().header().searchLine().suggest().getItem(2)
                .should(hasText(format("%s\nСкутеры", MARK)));
        basePageSteps.onMainPage().header().searchLine().suggest().getItem(0)
                .should(hasText(format("%s\nАвтомобили", MARK))).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK.toLowerCase()).path(ALL).addParam("query", MARK)
                .addParam("from", "searchline").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке очистки поля ввода")
    public void shouldClearSearchLine() {
        basePageSteps.onMainPage().header().searchLine().input("Поиск по объявлениям", MARK);
        basePageSteps.onMainPage().header().searchLine().clearTextButton().hover().click();
        basePageSteps.onMainPage().header().searchLine().input("Поиск по объявлениям").waitUntil(hasText(""));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по популярному запросу")
    public void shouldClickPopularRequest() {
        basePageSteps.onMainPage().header().searchLine().input("Поиск по объявлениям").click();
        basePageSteps.onMainPage().header().searchLine().suggest().itemsList().should(hasSize(5)).get(0).click();

        mockRule.with("desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").update();

        basePageSteps.onListingPage().filter().should(isDisplayed());
    }
}