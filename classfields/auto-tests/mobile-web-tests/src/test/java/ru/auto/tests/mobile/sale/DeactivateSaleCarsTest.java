package ru.auto.tests.mobile.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - снятие с продажи")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class DeactivateSaleCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String PHONE = "79111111111";

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
                "desktop/OfferCarsUsedUserOwner").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();

        mockRule.overwriteStub(1, "desktop/OfferCarsUsedUserOwnerInactive");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Снятие с продажи")
    public void shouldDeactivateSale() {
        mockRule.with("mobile/UserOffersCarsHide").update();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().ownerControls().button("Снять с продажи"));
        basePageSteps.onCardPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().soldPopup().radioButton("Продал на Авто.ру").click();
        deactivate();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Снятие с продажи, много спам звонков")
    public void shouldDeactivateSaleWithManySpamCalls() {
        mockRule.with("mobile/UserOffersCarsHideManySpamCalls").update();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().ownerControls().button("Снять с продажи"));
        basePageSteps.onCardPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().soldPopup().checkbox("Много спам звонков").click();
        basePageSteps.onCardPage().soldPopup().radioButton("Продал на Авто.ру").click();
        deactivate();

    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Снятие с продажи, автоподстановка номера телефона покупателя")
    public void shouldDeactivateSaleBuyersPhoneAutomatic() {
        mockRule.with("mobile/UserOffersCarsPredictBuyers",
                "mobile/UserOffersCarsHideBuyerPhone").update();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().ownerControls().button("Снять с продажи"));
        basePageSteps.onCardPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onCardPage().soldPopup().radioButton("+7 911 111-11-1110 июля 2020 в 17:26").click();
        deactivate();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Снятие с продажи, другой номера телефона покупателя")
    public void shouldDeactivateSaleBuyersOtherPhone() {
        mockRule.with("mobile/UserOffersCarsPredictBuyers",
                "mobile/UserOffersCarsHideBuyerPhone").update();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().ownerControls().button("Снять с продажи"));
        basePageSteps.onCardPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onCardPage().soldPopup().radioButton("Другой номер").click();
        basePageSteps.onCardPage().soldPopup().input("Введите телефон покупателя", PHONE);
        deactivate();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Снятие с продажи, ручной ввод номера телефона покупателя")
    public void shouldDeactivateSaleBuyersPhoneManual() {
        mockRule.with("mobile/UserOffersCarsHideBuyerPhone").update();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().ownerControls().button("Снять с продажи"));
        basePageSteps.onCardPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onCardPage().soldPopup().input("Введите телефон покупателя", PHONE);
        deactivate();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Снятие с продажи, продал где-то ещё")
    public void shouldDeactivateSaleSoldSomewhere() {
        mockRule.with("mobile/UserOffersCarsHideSoldOnAvito").update();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().ownerControls().button("Снять с продажи"));
        basePageSteps.onCardPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().soldPopup().radioButton("Продал где-то ещё").click();
        basePageSteps.onCardPage().soldPopup().radioButton("Авито").click();
        deactivate();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Снятие с продажи, другая причина")
    public void shouldDeactivateSaleOtherReason() {
        mockRule.with("mobile/UserOffersCarsHideOtherReason").update();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().ownerControls().button("Снять с продажи"));
        basePageSteps.onCardPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().soldPopup().radioButton("Другая причина").click();
        basePageSteps.onCardPage().soldPopup().input().sendKeys("I don't need no reason");
        deactivate();
    }

    @Step("Снимаем с продажи")
    public void deactivate() {
        basePageSteps.onCardPage().soldPopup().button("Снять с продажи").click();
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Статус объявления изменен"));
        basePageSteps.onCardPage().soldPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onCardPage().ownerControls().button("Снять с продажи").waitUntil(not(isDisplayed()));
        basePageSteps.onCardPage().ownerControls().button("Активировать").waitUntil(isDisplayed());
    }
}
