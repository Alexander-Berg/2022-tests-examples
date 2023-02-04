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

import static ru.auto.tests.desktop.TestData.OWNER_USER_2_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.COMPARE;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.COMPARE_OFFERS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.page.auth.AuthPage.CONTINUE_BUTTON;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сравние объявлений - сохранение добавленного  сравнения при логине")
@Feature(COMPARE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SaveSaleAfterLoginTest {

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
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
        basePageSteps.onListingPage().getSale(0).waitUntil(isDisplayed()).hover();
        basePageSteps.onListingPage().getSale(0).toolBar().compareButton().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(COMPARE_OFFERS).open();
    }

    @Test
    @Owner(DENISKOROBOV)
    @DisplayName("Сохранение добавленного в сравнение объявления при логине")
    @Category({Regression.class, Testing.class})
    public void shouldSeeSavedSaleAfterLogin() {
        String firstOfferLink = basePageSteps.onComparePage().getSale(0).title().getAttribute("href");
        basePageSteps.onComparePage().header().button("Войти").click();
        basePageSteps.onAuthPage().input("Телефон или электронная почта", OWNER_USER_2_PROVIDER.get().getLogin());
        basePageSteps.onAuthPage().button(CONTINUE_BUTTON).click();
        basePageSteps.onAuthPage().input("Пароль", "autoru");
        basePageSteps.onAuthPage().button("Войти").click();
        basePageSteps.onComparePage().getSale(0).title().waitUntil(hasAttribute("href", firstOfferLink));
        basePageSteps.onComparePage().getSale(0).hover();
        basePageSteps.onComparePage().getSale(0).deleteButton().waitUntil(isDisplayed()).click();
    }
}