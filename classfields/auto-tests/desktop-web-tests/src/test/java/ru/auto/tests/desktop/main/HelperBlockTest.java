package ru.auto.tests.desktop.main;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - помощник")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class HelperBlockTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().marksBlock().switcher("Помощник").click();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Сброс кузова")
    public void shouldResetBody() {
        basePageSteps.onMainPage().marksBlock().body("Седан").click();
        basePageSteps.onMainPage().marksBlock().body("Седан").click();
        basePageSteps.onMainPage().marksBlock().resultsButton().click();
        urlSteps.path(CARS).path(ALL).addParam("from", "old_guru").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Сброс пресета")
    public void shouldResetPreset() {
        basePageSteps.onMainPage().marksBlock().preset("Вместительный").click();
        basePageSteps.onMainPage().marksBlock().selectedPreset("Вместительный").waitUntil(isDisplayed()).click();
        basePageSteps.onMainPage().marksBlock().resultsButton().click();
        urlSteps.path(CARS).path(ALL).addParam("from", "old_guru").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Переключение на блок марок")
    public void shouldSwitchToMarksBlock() {
        basePageSteps.onMainPage().marksBlock().switcher("Марки").click();
        basePageSteps.onMainPage().marksBlock().getMark(0).waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Сохранение состояния переключателя Марки/Помощник")
    public void shouldSaveSwitcherState() {
        basePageSteps.onMainPage().marksBlock().resultsButton().click();
        basePageSteps.driver().navigate().back();
        basePageSteps.onMainPage().marksBlock().selectedSwitcher("Помощник").should(isDisplayed());
        basePageSteps.onMainPage().marksBlock().body("Седан").should(isDisplayed());

        basePageSteps.onMainPage().marksBlock().switcher("Марки").click();
        basePageSteps.onMainPage().marksBlock().resultsButton().click();
        basePageSteps.driver().navigate().back();
        basePageSteps.onMainPage().marksBlock().selectedSwitcher("Марки").should(isDisplayed());
        basePageSteps.onMainPage().marksBlock().getMark(0).waitUntil(isDisplayed());
    }
}