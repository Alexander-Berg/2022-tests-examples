package ru.auto.tests.mobile.my;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("ЛК - активация объявления мото")
@Epic(LK)
@Feature(SALES)
@Story("Активация объявления")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ActivateSaleMotoTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/User",
                "desktop/SessionAuthUser",
                "desktop/UserOffersMotoInactive",
                "desktop/UserOffersMotoActivate").post();

        urlSteps.testing().path(MY).path(MOTO).open();

        mockRule.overwriteStub(1, "desktop/UserOffersMotoActive");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Активировать»")
    public void shouldClickActivateButton() {
        basePageSteps.onLkPage().getSale(0).button("Активировать").click();
        basePageSteps.onLkPage().notifier().waitUntil(isDisplayed()).should(hasText("Статус объявления изменен"));
        basePageSteps.onLkPage().getSale(0).button("Активировать").waitUntil(not(isDisplayed()));
        basePageSteps.onLkPage().getSale(0).button("Снять с продажи").waitUntil(isDisplayed());
        basePageSteps.onLkPage().vas().waitUntil(isDisplayed());
    }
}
