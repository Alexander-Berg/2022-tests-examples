package ru.auto.tests.bem;

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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.BRAND_NEW_WIDGET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Виджет новинок")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class BrandNewWidgetTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(BRAND_NEW_WIDGET).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Виджет новинок")
    public void shouldSeeBrandNewWidget() {
        basePageSteps.onMainPage().brandNewWidget().should(isDisplayed());
    }
}
