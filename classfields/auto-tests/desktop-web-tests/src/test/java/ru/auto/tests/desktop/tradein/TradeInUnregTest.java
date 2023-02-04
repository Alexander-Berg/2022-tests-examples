package ru.auto.tests.desktop.tradein;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.TRADEIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Трейд-ин под незарегом")
@Feature(TRADEIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class TradeInUnregTest {

    private static final String PATH = "/kia/optima/21342125/21342344/1076842087-f1e84/";
    private static final String NAME = "Иван Иванов";
    private static final String PHONE = "9111111111";
    private static final String CONFIRMATION_CODE = "1234";

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsNewDealer",
                "desktop/OfferCarsTradeInNoSales",
                "desktop/UserPhones",
                "desktop/UserConfirm").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа")
    public void shouldSeeTradeInPopup() {
        basePageSteps.onCardPage().cardHeader().tradeInButton().should(hasText("Обмен на мой авто")).click();
        basePageSteps.onCardPage().cardHeader().tradeInPopup().waitUntil(isDisplayed())
                .should(hasText("Обменять свой автомобиль на этот\nот 1 254 900 ₽\nKia Optima IV Рестайлинг\n" +
                        "Ваш автомобиль\nДилер уточнит у вас его параметры\nТелефон\nИмя\n" +
                        "Я согласен, чтобы мне позвонили\nПерезвоните мне"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заявка на трейд-ин под незарегом")
    public void shouldSendTradeInRequest() {
        basePageSteps.onCardPage().cardHeader().tradeInButton().should(isDisplayed()).click();
        basePageSteps.onCardPage().cardHeader().tradeInPopup().input("Имя").sendKeys(NAME);
        basePageSteps.onCardPage().cardHeader().tradeInPopup().input("Телефон").sendKeys(PHONE);
        basePageSteps.onCardPage().cardHeader().tradeInPopup().input("Код из смс").waitUntil(isDisplayed());
        basePageSteps.onCardPage().cardHeader().tradeInPopup().input("Код из смс").sendKeys(CONFIRMATION_CODE);
        basePageSteps.onCardPage().cardHeader().tradeInPopup().input("Код из смс").waitUntil(not(isDisplayed()));
        basePageSteps.onCardPage().cardHeader().tradeInPopup().button("Перезвоните мне").click();
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Заявка успешно отправлена. Менеджер дилерского центра " +
                "свяжется с вами в ближайшее время"));
        basePageSteps.onCardPage().cardHeader().tradeInPopup().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Чекбокс «Я согласен, чтобы мне позвонили»")
    public void shouldClickAgreeCheckbox() {
        basePageSteps.onCardPage().cardHeader().tradeInButton().should(isDisplayed()).click();
        basePageSteps.onCardPage().cardHeader().tradeInPopup().button("Перезвоните мне").should(isEnabled());
        basePageSteps.onCardPage().cardHeader().tradeInPopup().checkbox("Я согласен, чтобы мне позвонили").click();
        basePageSteps.onCardPage().cardHeader().tradeInPopup().button("Перезвоните мне").waitUntil(not(isEnabled()));
        basePageSteps.onCardPage().cardHeader().tradeInPopup().checkbox("Я согласен, чтобы мне позвонили").click();
        basePageSteps.onCardPage().cardHeader().tradeInPopup().button("Перезвоните мне").waitUntil(isEnabled());
    }
}
