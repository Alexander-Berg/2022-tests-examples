package ru.auto.tests.mobile.main;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.MAIN)
@Story(FILTERS)
@DisplayName("Главная - фильтры")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class FiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void open() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty")
        ).create();

        urlSteps.testing().path("/").open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Параметры»")
    @Owner(DSVICHIHIN)
    public void shouldClickParamsButton() {
        basePageSteps.onMainPage().filters().button("Параметры").should(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onMainPage().paramsPopup().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Марка, модель»")
    @Owner(DSVICHIHIN)
    public void shouldClickMarkModelButton() {
        basePageSteps.onMainPage().filters().button("Марка, модель").should(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onMainPage().mmmPopup().waitUntil(isDisplayed());
    }
}
