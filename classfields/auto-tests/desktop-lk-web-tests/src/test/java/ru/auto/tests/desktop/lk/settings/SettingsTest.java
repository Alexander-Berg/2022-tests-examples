package ru.auto.tests.desktop.lk.settings;

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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CHANGE;
import static ru.auto.tests.desktop.consts.Pages.EMAIL;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROFILE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Настройки")
@Feature(LK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SettingsTest {

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
        mockRule.setStubs(stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserWithAuthTypes"),
                stub("desktop-lk/UserProfile"),
                stub("desktop-lk/UserPhones"),
                stub("desktop-lk/UserConfirm"),
                stub("desktop-lk/UserConfirmError"),
                stub("desktop-lk/UserPhonesDelete")).create();

        urlSteps.testing().path(MY).path(PROFILE).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Сохранение настроек")
    public void shouldSaveSettings() {
        basePageSteps.onSettingsPage().button("Сохранить").hover().click();
        basePageSteps.onSettingsPage().notifier().waitUntil(hasText("Информация сохранена"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Изменить e-mail»")
    public void shouldClickChangeEmailButton() {
        String currentUrl = urlSteps.getCurrentUrl();
        basePageSteps.onSettingsPage().button("Изменить").click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(EMAIL).path(CHANGE).addParam("r", encode(currentUrl))
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление телефонов")
    public void shouldAddPhones() {
        basePageSteps.onSettingsPage().addPhoneButton().should(isDisplayed()).click();
        basePageSteps.onSettingsPage().input("Телефон").sendKeys("1111111111");
        basePageSteps.onSettingsPage().input("Код из смс").waitUntil(isDisplayed()).sendKeys("1234");
        basePageSteps.onSettingsPage().phonesList().waitUntil(hasSize(2));
        basePageSteps.onSettingsPage().getPhone(1).waitUntil(hasValue("+7 111 111-11-11"));

        basePageSteps.onSettingsPage().addPhoneButton().should(isDisplayed()).click();
        basePageSteps.onSettingsPage().input("Телефон").sendKeys("2222222222");
        basePageSteps.onSettingsPage().input("Код из смс").waitUntil(isDisplayed()).sendKeys("1234");
        basePageSteps.onSettingsPage().phonesList().waitUntil(hasSize(3));
        basePageSteps.onSettingsPage().getPhone(1).waitUntil(hasValue("+7 222 222-22-22"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление телефона, ввод некорректного кода из смс")
    public void shouldNotAddPhone() {
        basePageSteps.onSettingsPage().addPhoneButton().click();
        basePageSteps.onSettingsPage().input("Телефон").sendKeys("1111111111");
        basePageSteps.onSettingsPage().input("Код из смс").waitUntil(isDisplayed()).sendKeys("1111");
        basePageSteps.onSettingsPage().inputError("Код из смс").waitUntil(isDisplayed());
        basePageSteps.onSettingsPage().phonesList().should(hasSize(1));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление телефона")
    public void shouldDeletePhone() {
        basePageSteps.onSettingsPage().getPhone(0).deleteIcon().click();
        basePageSteps.acceptAlert();
        basePageSteps.onSettingsPage().phonesList().waitUntil(hasSize(0));
    }
}
