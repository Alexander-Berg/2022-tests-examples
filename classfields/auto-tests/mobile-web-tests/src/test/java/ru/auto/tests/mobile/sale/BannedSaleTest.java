package ru.auto.tests.mobile.sale;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.TestData.OWNER_USER_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Забаненное объявление")
@Feature(SALES)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BannedSaleTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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

    @Inject
    private LoginSteps loginSteps;

    @Parameterized.Parameter
    public String mock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"desktop/OfferCarsBannedDoNotExist"},
                {"desktop/OfferCarsBannedHigherPrice"}
        });
    }

    @Before
    public void before() throws IOException {
        mockRule.newMock().with(mock,
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        loginSteps.loginAs(OWNER_USER_PROVIDER.get());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение плашки «Заблокировано модератором»")
    public void shouldSeeBanned() {
        basePageSteps.onCardPage().price().bannedBadge().should(isDisplayed()).should(hasText("Заблокировано"));
        basePageSteps.onCardPage().banReasons().should(isDisplayed()).should(hasText("Причины блокировки\n" +
                "Необходимо подтверждение\nМы полагаем, что вы пытались продать несуществующий автомобиль. " +
                "Если это не так и вы можете подтвердить, что владеете им, напишите в службу поддержки."));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Написать в поддержку»")
    public void shouldClickSupportButton() throws InterruptedException {
        waitSomething(3, TimeUnit.SECONDS);
        basePageSteps.onCardPage().ownerControls().button("Написать в поддержку").click();
        basePageSteps.onCardPage().chat().waitUntil(isDisplayed());
    }
}
