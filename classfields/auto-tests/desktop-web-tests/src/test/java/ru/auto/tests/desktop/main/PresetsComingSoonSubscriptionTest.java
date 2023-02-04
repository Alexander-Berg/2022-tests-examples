package ru.auto.tests.desktop.main;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.io.IOException;

import static ru.auto.tests.desktop.TestData.OWNER_USER_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - пресеты - скоро в продаже")
@Feature(MAIN)
@GuiceModules(DesktopTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class PresetsComingSoonSubscriptionTest {

    private static final int PRESET_NUM = 1;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        loginSteps.loginAs(OWNER_USER_PROVIDER.get());

        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().presets().getPreset(PRESET_NUM).waitUntil(isDisplayed()).hover().click();
    }


    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Подписка")
    public void shouldSubscribe() {
        if (basePageSteps.isElementExist(basePageSteps.onMainPage().presets().getSale(0)
                .button("Вы подписаны"))) {
            basePageSteps.onMainPage().presets().getSale(0).button("Вы подписаны").click();
            basePageSteps.scrollDown(200);
            deleteSubscription();
        }
        basePageSteps.onMainPage().presets().getSale(0).button("Подписаться").hover().click();
        basePageSteps.onMainPage().notifier().waitUntil(hasText("Поиск сохранён"));
        deleteSubscription();
        basePageSteps.onMainPage().presets().getSale(0).button("Подписаться").waitUntil(isDisplayed());
    }

    @Step("Удаляем подписку")
    private void deleteSubscription() {
        basePageSteps.onMainPage().savedSearchesPopup().waitUntil(isDisplayed());
        basePageSteps.onMainPage().savedSearchesPopup().deleteButton().hover().click();
        basePageSteps.onMainPage().notifier().waitUntil(hasText("Поиск удалён"));
    }
}