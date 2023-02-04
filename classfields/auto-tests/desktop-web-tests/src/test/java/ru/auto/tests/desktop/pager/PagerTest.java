package ru.auto.tests.desktop.pager;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.DesktopConfig.MAX_PAGES;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PAGER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Листинг - пагинация")
@Feature(PAGER)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PagerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    //@Parameter("Категория ТС")
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

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход на следующую страницу по кнопке «Следующая»")
    public void shouldClickNextButton() {
        basePageSteps.onListingPage().pager().next().click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().should(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход на предыдущую страницу по кнопке «Предыдущая»")
    public void shouldClickPrevButton() {
        urlSteps.addParam("page", "2").open();
        basePageSteps.onListingPage().pager().prev().click();
        urlSteps.replaceQuery("").shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().should(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход на страницу через пагинатор")
    public void shouldClickPageInPager() {
        basePageSteps.onListingPage().pager().page("2").click();
        urlSteps.addParam("page", "2").ignoreParam("output_type").shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().should(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Показать ещё»")
    public void shouldClickMoreButton() {
        basePageSteps.onListingPage().pager().button("Показать ещё").click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().should(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «...»")
    public void shouldClickSkipButton() {
        basePageSteps.onListingPage().pager().threeDotsFirst().click();
        basePageSteps.onListingPage().pager().currentPage().waitUntil(hasText("10"));
        urlSteps.addParam("page", "10").shouldNotSeeDiff();
        basePageSteps.onListingPage().pager().threeDotsLast().click();
        basePageSteps.onListingPage().pager().currentPage().waitUntil(hasText("14"));
        basePageSteps.onListingPage().salesList().should(hasSize(greaterThan(0)));
        urlSteps.replaceParam("page", "14").shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().should(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Проверяем ограничение по количеству страниц")
    public void shouldNotSeeMoreThanMaxPages() {
        int page = MAX_PAGES + 1;
        urlSteps.addParam("page", String.valueOf(page)).open();
        basePageSteps.onListingPage().pager().currentPage().should(hasText("1"));
        basePageSteps.onListingPage().salesList().should(hasSize(greaterThan(0)));
    }
}