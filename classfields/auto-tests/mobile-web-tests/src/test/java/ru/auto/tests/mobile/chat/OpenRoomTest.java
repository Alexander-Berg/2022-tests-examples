package ru.auto.tests.mobile.chat;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.io.IOException;

import static java.lang.String.format;
import static ru.auto.tests.desktop.TestData.CLIENT_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CHAT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Открытие комнаты чата")
@Feature(PROMO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class OpenRoomTest {

    private static final String ROOM_ID = "ab63f77a89a3c71652848a8ac10698ab";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public LoginSteps loginSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        loginSteps.loginAs(CLIENT_PROVIDER.get());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Открытие комнаты чата")
    public void shouldOpenRoomByPath() {
        urlSteps.testing().path(CHAT).path(format("/%s/", ROOM_ID)).open();

        basePageSteps.onMainPage().chat().waitUntil("Чат не открылся", isDisplayed(), 10);
        basePageSteps.onMainPage().chat().header().waitUntil(hasText("Авто.ру"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Открытие комнаты чата")
    public void shouldOpenRoomByGetParam() {
        urlSteps.testing().path(CHAT).addParam("chat_id", ROOM_ID).open();

        basePageSteps.onMainPage().chat().waitUntil("Чат не открылся", isDisplayed(), 10);
        basePageSteps.onMainPage().chat().header().waitUntil(hasText("Авто.ру"));
    }
}
