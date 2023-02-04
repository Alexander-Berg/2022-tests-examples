package ru.auto.tests.mobile.callback;

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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CALLBACK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("Заказ обратного звонка на групповой карточке")
@Feature(CALLBACK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class CallbackGroupTest {

    private static final String PATH = "/kia/optima/21342050-21342121/";
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

    @Before
    public void before() {

        mockRule.newMock().with("mobile/SearchCarsBreadcrumbsMarkModelGroup",
                "mobile/SearchCarsGroupContextGroup",
                "mobile/SearchCarsGroupContextListing",
                "desktop/AuthLoginOrRegister",
                "desktop/UserConfirm",
                "desktop/OfferCarsRegisterCallbackForNewCategory").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заказ обратного звонка под незарегом")
    public void shouldSendCallbackUnreg() {
        basePageSteps.onGroupPage().getSale(0).button("Контакты").click();
        basePageSteps.onGroupPage().popup().button("Заказать обратный звонок").hover().click();
        basePageSteps.onGroupPage().callbackPopup().input("Номер телефона").click();
        basePageSteps.onGroupPage().callbackPopup().input("Номер телефона", PHONE);
        basePageSteps.onGroupPage().callbackPopup().button("Подтвердить номер").waitUntil(isDisplayed()).click();
        basePageSteps.onGroupPage().callbackPopup().input("Код из SMS").waitUntil(isDisplayed());
        basePageSteps.onGroupPage().callbackPopup().input("Код из SMS", "1234");
        basePageSteps.onGroupPage().notifier().waitUntil(hasText("Заявка отправлена"));
        basePageSteps.onGroupPage().callbackPopup().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение пользовательского соглашения")
    public void shouldSeeUserAgreement() {
        basePageSteps.onGroupPage().getSale(0).button("Контакты").click();
        basePageSteps.onGroupPage().popup().button("Заказать обратный звонок").hover().click();
        basePageSteps.onGroupPage().callbackPopup().button("пользовательского\u00a0соглашения").hover().click();
        basePageSteps.onGroupPage().popup().waitUntil(hasText("Я даю свое согласие на передачу " +
                "в ООО «Яндекс.Вертикали» (ОГРН: 515746192742) и оператору связи ООО «ФАСТКОМ» (ОГРН: 1117746465440), " +
                "а также лицу, указанному в качестве продавца в объявлении о продаже транспортного средства, моей " +
                "персональной информации, указанной в настоящей форме, содержащей, в том числе, мои персональные данные, " +
                "и согласен с тем, что мои персональные данные будут обрабатываться указанными лицами в соответствии " +
                "с Федеральным законом «О персональных данных» в целях связи со мной по вопросам купли-продажи " +
                "транспортного средства.\nРазговор может быть записан в целях оценки качества услуг."));
    }
}
