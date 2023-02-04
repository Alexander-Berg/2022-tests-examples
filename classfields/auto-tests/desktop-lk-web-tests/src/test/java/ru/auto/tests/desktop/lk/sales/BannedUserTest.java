package ru.auto.tests.desktop.lk.sales;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.TestData;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Забаненный пользователь")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class BannedUserTest {

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
    private LoginSteps loginSteps;

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение ЛК с текстом про бан")
    public void shouldSeeBanReason() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUserBanned"),
                stub("desktop/UserBanned"),
                stub("desktop/UserModerationStatusBanned"),
                stub("desktop/UserOffersCarsEmpty")
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();
        basePageSteps.onLkSalesPage().bannedMessage().should(hasText("Личный кабинет заблокирован\nМы полагаем, что вы " +
                "пытались продать несуществующий автомобиль. Если это не так и вы можете подтвердить, что владеете им, " +
                "напишите в чат с поддержкой и выберите сценарий Пройти верификацию — наш бот подробно расскажет, " +
                "что нужно для повторной проверки.\nНаписать в поддержку"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Написать в поддержку»")
    public void shouldClickSupportButton() throws IOException {
        mockRule.setStubs(
                stub("desktop/UserModerationStatusBanned"),
                stub("desktop/ProxyPublicApi")
        ).create();

        loginSteps.loginAs(TestData.USER_2_PROVIDER.get());
        urlSteps.testing().path(MY).path(CARS).open();
        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        basePageSteps.onLkSalesPage().bannedMessage().button("Написать в поддержку").hover().click();
        basePageSteps.onCardPage().chat().waitUntil(isDisplayed(), 10);
    }

}