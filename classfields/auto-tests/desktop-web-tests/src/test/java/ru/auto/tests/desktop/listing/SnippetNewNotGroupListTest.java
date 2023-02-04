package ru.auto.tests.desktop.listing;

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

import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("Листинг - сниппет нового объявления без группировки")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SnippetNewNotGroupListTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsNewNotGroup").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).addParam("output_type", "list").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение сниппета")
    public void shouldSeeSnippet() {
        basePageSteps.onListingPage().getSale(0).should(hasText("Kia Optima IV Рестайлинг\n2.4 л / 188 л.с." +
                " / Бензин\nавтомат\nседан\nпередний\nLuxe\n54 опции\nот 1 689 900 ₽\nдо 1 739 900 ₽\n" +
                "2019\nНовый\nВ наличии\nMajor KIA Тушино\n Сходненская\nМосква"));
        basePageSteps.onListingPage().getSale(0).photo().should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по сниппету")
    public void shouldClickSnippet() {
        basePageSteps.onListingPage().getSale(0).nameLink().should(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path("/kia/optima/21342127/21342368/1076842087-f1e84/")
                .shouldNotSeeDiff();
    }
}