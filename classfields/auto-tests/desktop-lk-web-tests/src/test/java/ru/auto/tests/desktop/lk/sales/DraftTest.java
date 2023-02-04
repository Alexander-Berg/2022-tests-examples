package ru.auto.tests.desktop.lk.sales;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.FROM_LK;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Черновики")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DraftTest {

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

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionUnauth"),
                stub("desktop-lk/UserDraftCars")
        ).create();

        urlSteps.testing().path(MY).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение черновика")
    public void shouldSeeDraft() {
        basePageSteps.onLkSalesPage().getSale(0).should(hasText("Subaru Forester\nЧерновик"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по заголовку")
    public void shouldClickTitle() {
        basePageSteps.onLkSalesPage().getSale(0).title().hover().click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(USED).path(ADD).path(SLASH).addParam(FROM_LK,"true").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Продолжить заполнение»")
    public void shouldClickContinueButton() {
        basePageSteps.onLkSalesPage().getSale(0).hover();
        basePageSteps.onLkSalesPage().getSale(0).button("Продолжить заполнение").waitUntil(isDisplayed())
                .click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(USED).path(ADD).path(SLASH).addParam(FROM_LK,"true").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Удалить»")
    public void shouldClickDeleteButton() {
        mockRule.setStubs(stub("desktop-lk/UserDraftCarsDelete")).update();

        basePageSteps.onLkSalesPage().getSale(0).hover();
        basePageSteps.onLkSalesPage().getSale(0).button("Удалить").waitUntil(isDisplayed()).click();
        basePageSteps.acceptAlert();
        basePageSteps.onLkSalesPage().notifier().waitUntil(isDisplayed()).should(hasText("Черновик удалён"));
        basePageSteps.onLkSalesPage().salesList().waitUntil(hasSize(0));
    }
}
