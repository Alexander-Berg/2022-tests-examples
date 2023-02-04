package ru.auto.tests.mobile.tradein;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.TRADEIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Трейд-ин под зарегом")
@Feature(TRADEIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class TradeInRegTwoSalesTest {

    private static final String PATH = "/kia/optima/21342125/21342344/1076842087-f1e84/";
    private static final String PHONE = "+7 911 111-11-11";
    private static final String NAME = "Иван Иванов";
    private static final String FIRST_SALE_PRICE = "700 000 ₽";
    private static final String SECOND_SALE_PRICE = "600 000 ₽";

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
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsNewDealer",
                "desktop/OfferCarsTradeIn",
                "desktop/User",
                "desktop/UserPhones",
                "desktop/UserOffersCarsActiveTwoSales").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа")
    public void shouldSeeTradeInPopup() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().tradeIn().button("Оставить заявку"));
        basePageSteps.onCardPage().tradeIn().popup().waitUntil(isDisplayed()).should(hasText("Обменять свой автомобиль " +
                "на этот\n1/2\nот 1 254 900 ₽\nKia Optima IV Рестайлинг\n700 000 ₽\nMercedes-Benz GL-klasse I (X164) " +
                "Рестайлинг\nЯ согласен, чтобы мне позвонили\nПерезвоните мне"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заявка на трейд-ин под зарегом")
    public void shouldSendTradeInRequest() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().tradeIn().button("Оставить заявку"));
        basePageSteps.onCardPage().tradeIn().popup().input("Имя").should(hasValue(NAME));
        basePageSteps.onCardPage().tradeIn().popup().input("Телефон").should(hasValue(PHONE));
        basePageSteps.onCardPage().tradeIn().popup().button("Перезвоните мне").click();
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Заявка успешно отправлена"));
        basePageSteps.onCardPage().tradeIn().popup().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переключение объявлений")
    public void shouldSwitchSales() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().tradeIn().button("Оставить заявку"));
        basePageSteps.onCardPage().tradeIn().popup().userSalePrice().should(hasText(FIRST_SALE_PRICE));
        basePageSteps.onCardPage().tradeIn().popup().nextSaleButton().waitUntil(isEnabled()).click();
        basePageSteps.onCardPage().tradeIn().popup().userSalePrice().waitUntil(hasText(SECOND_SALE_PRICE));
        basePageSteps.onCardPage().tradeIn().popup().prevSaleButton().waitUntil(isEnabled()).click();
        basePageSteps.onCardPage().tradeIn().popup().userSalePrice().waitUntil(hasText(FIRST_SALE_PRICE));
    }
}
