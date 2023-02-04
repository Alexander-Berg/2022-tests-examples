package ru.auto.tests.desktop.lk.salesNew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.Pages.SAFE_DEAL;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_19219;

@DisplayName("Безопасная сделка")
@Epic(AutoruFeatures.LK_NEW)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@Ignore
public class SafeDealHeaderLinkUnauthTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_19219);

        mockRule.setStubs(stub("desktop/SessionUnauth")).create();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие промо при клике в шапку когда не залогинен")
    public void shouldOpenPromoWhenUnauth() {
        basePageSteps.onLkSalesNewPage().navigationLink("Безопасная сделка").click();

        urlSteps.testing().path(PROMO).path(SAFE_DEAL).shouldNotSeeDiff();
    }

}
