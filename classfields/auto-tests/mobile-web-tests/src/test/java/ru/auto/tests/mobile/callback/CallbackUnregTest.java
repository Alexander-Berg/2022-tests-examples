package ru.auto.tests.mobile.callback;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
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

@DisplayName("Карточка объявления - заказ обратного звонка")
@Feature(CALLBACK)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CallbackUnregTest {

    private static final String SALE_ID = "1076842087-f1e84";
    private static final String PHONE = "9111111111";

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

    //@Parameter("Тип ТС")
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
        mockRule.newMock().with("desktop/AuthLoginOrRegister",
                "desktop/UserConfirm",
                "desktop/UserConfirmError",
                saleMock,
                callbackMock).post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().callbackButton());

    }


    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа")
    public void shouldSeeCallbackPopup() {
        basePageSteps.onCardPage().callbackPopup().title().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().callbackPopup().waitUntil(isDisplayed()).should(hasText("Заявка на обратный звонок\n" +
                "Номер телефона\nПодтвердить номер\nОтправляя заявку, я соглашаюсь с условиями пользовательского " +
                "соглашения"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение пользовательского соглашения")
    public void shouldSeeUserAgreement() {
        basePageSteps.onCardPage().callbackPopup().button("пользовательского\u00a0соглашения").hover().click();
        basePageSteps.onCardPage().callbackPopup().userAgreement().waitUntil(hasText("Я даю свое согласие на передачу " +
                "в ООО «Яндекс.Вертикали» (ОГРН: 515746192742) и оператору связи ООО «ФАСТКОМ» (ОГРН: 1117746465440), " +
                "а также лицу, указанному в качестве продавца в объявлении о продаже транспортного средства, моей " +
                "персональной информации, указанной в настоящей форме, содержащей, в том числе, мои персональные данные, " +
                "и согласен с тем, что мои персональные данные будут обрабатываться указанными лицами в соответствии " +
                "с Федеральным законом «О персональных данных» в целях связи со мной по вопросам купли-продажи " +
                "транспортного средства.\nРазговор может быть записан в целях оценки качества услуг."));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заказ обратного звонка под незарегом")
    public void shouldSendCallbackUnreg() {
        basePageSteps.onCardPage().callbackPopup().input("Номер телефона").click();
        basePageSteps.onCardPage().callbackPopup().input("Номер телефона", PHONE);
        basePageSteps.onCardPage().callbackPopup().button("Подтвердить номер").waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().callbackPopup().input("Код из SMS").waitUntil(isDisplayed());
        basePageSteps.onCardPage().callbackPopup().input("Код из SMS", "1234");
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Заявка отправлена"));
        basePageSteps.onCardPage().callbackPopup().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заказ обратного звонка под незарегом, повторный ввод кода из SMS")
    public void shouldResendCallbackUnreg() {
        basePageSteps.onCardPage().callbackPopup().input("Номер телефона").click();
        basePageSteps.onCardPage().callbackPopup().input("Номер телефона", PHONE);
        basePageSteps.onCardPage().callbackPopup().button("Подтвердить номер").waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().callbackPopup().input("Код из SMS").waitUntil(isDisplayed());
        basePageSteps.onCardPage().callbackPopup().input("Код из SMS", "1111");
        basePageSteps.onCardPage().callbackPopup().errorMessage().waitUntil(hasText("Неверный код"));
        basePageSteps.onCardPage().callbackPopup().button("Выслать повторно").click();
        basePageSteps.onCardPage().callbackPopup().clearInput("Код из SMS");
        basePageSteps.onCardPage().callbackPopup().input("Код из SMS").sendKeys("1234");
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Заявка отправлена"));
        basePageSteps.onCardPage().callbackPopup().waitUntil(not(isDisplayed()));
    }
}
