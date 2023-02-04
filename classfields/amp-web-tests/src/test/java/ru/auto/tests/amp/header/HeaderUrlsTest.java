package ru.auto.tests.amp.header;

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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.FROM_WEB_TO_APP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.AMP)
@DisplayName("AMP - шапка")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HeaderUrlsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "mobile/SearchCarsAll").post();

        urlSteps.testing().path(AMP).path(CARS).path(ALL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по логотипу")
    @Owner(DSVICHIHIN)
    public void shouldClickLogo() {
        basePageSteps.onListingPage().header().logo().should(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path("/").ignoreParam("_gl").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Продать авто»")
    @Owner(DSVICHIHIN)
    public void shouldClickAddSaleButton() {
        basePageSteps.onListingPage().header().addSaleButton().should(isDisplayed()).click();
        urlSteps.testing().path(PROMO).path(FROM_WEB_TO_APP).ignoreParam("_gl").shouldNotSeeDiff();
    }
}
