package ru.auto.tests.mobile.credits;

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
import ru.auto.tests.desktop.consts.QueryParams;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CREDITS;
import static ru.auto.tests.desktop.consts.Owners.NIKOVCHARENKO;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.ON_CREDIT;
import static ru.auto.tests.desktop.consts.QueryParams.TRUE;
import static ru.auto.tests.desktop.mobile.page.ListingPage.IN_CREDIT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - Чекбокс «В кредит»")
@Feature(CREDITS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ListingOnCreditCheckboxTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Выставляем чекбокс «В кредит»")
    public void shouldTurnCheckboxOn() {
        basePageSteps.onListingPage().filters().checkbox(IN_CREDIT).click();
        urlSteps.path(ON_CREDIT).shouldNotSeeDiff();
        basePageSteps.onListingPage().waitForListingReload();

        basePageSteps.onListingPage().filters().should(isDisplayed());
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Снимаем чекбокс «В кредит»")
    public void shouldTurnCheckboxOff() {
        urlSteps.addParam(QueryParams.ON_CREDIT, TRUE).open();
        basePageSteps.onListingPage().filters().checkbox(IN_CREDIT).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).shouldNotSeeDiff();
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class})
    @DisplayName("Сброс параметра при переходе в новые")
    public void shouldResetParamInNew() {
        urlSteps.addParam(QueryParams.ON_CREDIT, TRUE).open();
        basePageSteps.onListingPage().filters().section("Новые").click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).shouldNotSeeDiff();
    }
}
