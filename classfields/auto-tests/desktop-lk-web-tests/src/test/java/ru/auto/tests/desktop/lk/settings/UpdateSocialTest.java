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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROFILE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Соцсети")
@Feature(LK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class UpdateSocialTest {

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
                stub("desktop/SessionAuthUser"),
                stub("desktop-lk/UserWithYaSocial"),
                stub("desktop-lk/UserProfile"),
                stub("desktop-lk/ServerConfig")
        ).create();

        urlSteps.testing().path(MY).path(PROFILE).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Добавить еще")
    public void shouldAddMore() {
        basePageSteps.onSettingsPage().addMoreButton().click();
        basePageSteps.onSettingsPage().unlinkedNetworksBlock().should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Удалить")
    public void shouldDeleteYa() {
        mockRule.setStubs(stub("desktop-lk/UserSocialProfilesYandexDelete")).update();

        basePageSteps.onSettingsPage().deleteNetworkButton().click();
        basePageSteps.acceptAlert();
        basePageSteps.onSettingsPage().linkProfileBlock().should(isDisplayed());
    }
}
