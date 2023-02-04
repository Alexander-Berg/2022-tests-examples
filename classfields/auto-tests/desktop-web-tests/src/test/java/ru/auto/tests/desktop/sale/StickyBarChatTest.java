package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.TestData.OWNER_USER_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - плавающая панель")
@Feature(SALES)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class StickyBarChatTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public LoginSteps loginSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String saleMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedUser"},
                {TRUCK, "desktop/OfferTrucksUsedUser"},
                {MOTORCYCLE, "desktop/OfferMotoUsedUser"}
        });
    }

    @Before
    public void before() throws IOException {
        loginSteps.loginAs(OWNER_USER_PROVIDER.get());

        mockRule.newMock().with(saleMock,
                //"desktop/SessionAuthUser",
                //"desktop/DeviceWebsocket",
                "desktop/ChatMessageUnread",
                "desktop/ChatRoomGet",
                "desktop/ChatRoomPost",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.scrollDown(1000);
        basePageSteps.onCardPage().stickyBar().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Написать» под зарегом")
    public void shouldClickSendMessageButton() {
        basePageSteps.onCardPage().stickyBar().sendMessageButton().click();
        basePageSteps.onCardPage().chat().waitUntil("Чат не открылся", isDisplayed(), 10);
    }
}