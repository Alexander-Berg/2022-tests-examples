package ru.auto.tests.mobile.sale;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.TestData.OWNER_USER_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(SALES)
@DisplayName("Объявление - Кнопка «Написать в чат», когда пользователь запретил ему звонить")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ChatOnlyTest {

    private static final String SALE_ID = "1091760550-84c8e396";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    private LoginSteps loginSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String saleMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedUserActiveChatOnly"},
                {TRUCK, "desktop/OfferTrucksUsedUserActiveChatOnly"},
                {MOTORCYCLE, "desktop/OfferMotoUsedUserActiveChatOnly"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(stub(saleMock)).create();
    }

    @Test
    @DisplayName("Клик по кнопке «Написать» под незарегом")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickSendMessageButtonUnauth() {
        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
        String saleUrl = urlSteps.getCurrentUrl();
        basePageSteps.onCardPage().floatingContacts().callButton().should(not(isDisplayed()));
        basePageSteps.onCardPage().floatingContacts().button("Написать").click();

        basePageSteps.onCardPage().authPopup().should(isDisplayed());
        basePageSteps.onCardPage().authPopup().iframe()
                .should(hasAttribute("src", containsString(
                        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                                .addParam("r", encode(saleUrl))
                                .addParam("inModal", "true")
                                .addParam("autoLogin", "true")
                                .addParam("welcomeTitle", "")
                                .toString()
                )));
    }

    @Test
    @DisplayName("Клик по кнопке «Написать» в галерее под незарегом")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickGallerySendMessageButtonUnauth() {
        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
        String saleUrl = urlSteps.getCurrentUrl();

        basePageSteps.onCardPage().gallery().getItem(0).click();
        basePageSteps.onCardPage().fullScreenGallery().callButton().should(not(isDisplayed()));
        basePageSteps.onCardPage().fullScreenGallery().chatButton().click();

        basePageSteps.onCardPage().authPopup().should(isDisplayed());
        basePageSteps.onCardPage().authPopup().iframe()
                .should(hasAttribute("src", containsString(
                        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                                .addParam("r", encode(saleUrl))
                                .addParam("inModal", "true")
                                .addParam("autoLogin", "true")
                                .addParam("welcomeTitle", "")
                                .toString()
                )));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Написать» под зарегом")
    public void shouldClickSendMessageButtonAuth() throws IOException {
        loginSteps.loginAs(OWNER_USER_PROVIDER.get());

        mockRule.setStubs(
                stub("desktop/ChatMessageUnread"),
                stub("desktop/ChatRoomGet"),
                stub("desktop/ChatRoomPost"),
                stub("desktop/UserFavoritesAllSubscriptionsEmpty"),
                stub("desktop/ProxyPublicApi")
        ).update();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
        waitSomething(3, TimeUnit.SECONDS);
        basePageSteps.onCardPage().floatingContacts().callButton().should(not(isDisplayed()));
        basePageSteps.onCardPage().floatingContacts().button("Написать").click();
        basePageSteps.onCardPage().chat().waitUntil("Чат не открылся", isDisplayed(), 15);
    }
}
