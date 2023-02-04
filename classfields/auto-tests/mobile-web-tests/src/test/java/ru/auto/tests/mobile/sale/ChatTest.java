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
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
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

import static java.lang.String.format;
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

@DisplayName("Активное б/у объявление частника под зарегом - клик по кнопке чата")
@Feature(SALES)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ChatTest {

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
    private LoginSteps loginSteps;

    @Inject
    private UrlSteps urlSteps;

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
        mockRule.newMock().with(saleMock,
                "desktop/ChatMessageUnread",
                "desktop/ChatRoomGet",
                "desktop/ChatRoomPost",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        loginSteps.loginAs(OWNER_USER_PROVIDER.get());
    }

    @Test
    @DisplayName("Клик по кнопке «Написать»")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickSendMessageButton() {
        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().floatingContacts().sendMessageButton().should(isDisplayed()).click();
        basePageSteps.onCardPage().chat().waitUntil(isDisplayed());
    }

    @Test
    @DisplayName("Открытие чата по ссылке")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldOpenChatByUrl() {
        urlSteps.open(format("%s#open-chat%s",
                urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID), SALE_ID));
        basePageSteps.onCardPage().chat().waitUntil(isDisplayed());
    }
}
