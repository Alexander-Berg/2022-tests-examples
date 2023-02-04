package ru.auto.tests.desktopcompare;

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
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.TestData.OWNER_USER_2_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.COMPARE;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_MODELS;
import static ru.auto.tests.desktop.page.auth.AuthPage.CONTINUE_BUTTON;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сравние моделей - сохранение добавленного сравнения при логине")
@Feature(COMPARE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SaveModelsAfterLoginTest {

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
        urlSteps.testing().path(COMPARE_MODELS).open();

        basePageSteps.onComparePage().stub().button("Добавить модель").click();
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onComparePage().addModelPopup().markOrModel("Kia").click();
        basePageSteps.onComparePage().addModelPopup().button("Все модели").click();
        basePageSteps.onComparePage().addModelPopup().markOrModel("K5").click();
        basePageSteps.onComparePage().addModelPopup().generation("2020–2022").click();
        basePageSteps.onComparePage().addModelPopup().body("Седан").click();

    }

    @Test
    @Owner(DENISKOROBOV)
    @DisplayName("Сохранение добавленной в сравнение модели при логине")
    @Category({Regression.class, Testing.class})
    public void shouldSeeSavedModelsAfterLogin() {
        String firstModelLink = basePageSteps.onComparePage().getModel(0).url().getAttribute("href");
        basePageSteps.onComparePage().header().button("Войти").click();
        basePageSteps.onAuthPage().input("Телефон или электронная почта", OWNER_USER_2_PROVIDER.get().getLogin());
        basePageSteps.onAuthPage().button(CONTINUE_BUTTON).click();
        basePageSteps.onAuthPage().input("Пароль", "autoru");
        basePageSteps.onAuthPage().button("Войти").click();
        basePageSteps.onComparePage().getModel(0).url()
                .waitUntil(hasAttribute("href", firstModelLink));
        basePageSteps.onComparePage().getModel(0).hover();
        basePageSteps.onComparePage().getModel(0).deleteButton().waitUntil(isDisplayed()).click();
    }
}