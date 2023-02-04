package ru.auto.tests.desktop.callback;

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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CALLBACK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("Карточка объявления - заказ обратного звонка под зарегом, у которого несколько телефонов")
@Feature(CALLBACK)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CallbackRegWithSeveralPhonesTest {

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String saleMock;

    @Parameterized.Parameter(2)
    public String callbackMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedDealer", "desktop/OfferCarsRegisterCallback"},
                {TRUCK, "desktop/OfferTrucksUsedDealer", "desktop/OfferTrucksRegisterCallback"},
                {MOTORCYCLE, "desktop/OfferMotoUsedDealer", "desktop/OfferMotoRegisterCallback"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/UserWithSeveralPhones",
                saleMock,
                callbackMock).post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заказ обратного звонка под зарегом, у которого несколько телефонов")
    public void shouldSendDealerCallbackRegWithSeveralPhones() {
        basePageSteps.onCardPage().contacts().callBackButton().click();
        basePageSteps.onCardPage().callbackPopup().clearInputButton().click();
        basePageSteps.onCardPage().callbackPopup().input("Номер телефона", "9");
        basePageSteps.onCardPage().callbackPopup().phonesSuggest().waitUntil(isDisplayed());
        basePageSteps.onCardPage().callbackPopup().phonesList().should(hasSize(2));
        basePageSteps.onCardPage().callbackPopup().getPhone(0).click();
        basePageSteps.onCardPage().callbackPopup().button("Перезвоните мне").waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Заявка отправлена"));
        basePageSteps.onCardPage().callbackPopup().should(not(isDisplayed()));
    }
}