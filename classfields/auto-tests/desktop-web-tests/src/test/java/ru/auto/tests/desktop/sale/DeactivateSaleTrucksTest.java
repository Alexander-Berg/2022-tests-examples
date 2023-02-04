package ru.auto.tests.desktop.sale;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.DKP;
import static ru.auto.tests.desktop.consts.Pages.DOCS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Карточка объявления - снятие с продажи")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DeactivateSaleTrucksTest {

    private static final String SALE_ID_HASH = "/1076842087-f1e84/";
    private static final String SALE_ID = "1076842087";
    private final static String STATUS_ACTIVE = "Опубликовано";

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
                "desktop/OfferTrucksUsedUserOwner").post();

        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path(SALE_ID_HASH).open();

        mockRule.overwriteStub(1, "desktop/OfferTrucksUsedUserOwnerInactive");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Снятие с продажи")
    public void shouldDeactivateSale() {
        mockRule.with("desktop/UserOffersTrucksHide").update();

        basePageSteps.onCardPage().cardOwnerPanel().status().should(isDisplayed()).should(hasText(STATUS_ACTIVE));
        basePageSteps.onCardPage().cardOwnerPanel().button("Снять с продажи").click();

        basePageSteps.onCardPage().soldPopup().waitUntil(isDisplayed()).should(hasText("Пожалуйста, укажите " +
                "причину снятия\nПродал на Авто.ру\nПродал дилеру\nПродал где-то ещё\nПередумал продавать\nМало звонков " +
                "от покупателей\nПродам позже\nДругая причина\nМного спам звонков\nБесплатно составить договор купли-продажи\n" +
                "Снять с продажи\nОставить активным"));
        basePageSteps.onCardPage().soldPopup().radioButton("Продал на Авто.ру").click();
        deactivate();
        closeReviewPopup();
        checkSaleStatus();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Снятие с продажи с указанием цены продажи")
    public void shouldDeactivateSaleSoldPrice() {
        mockRule.with("desktop/UserOffersTrucksHideSoldPrice").update();

        basePageSteps.onCardPage().cardOwnerPanel().button("Снять с продажи").click();
        basePageSteps.onCardPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onCardPage().soldPopup().input("Стоимость, \u20BD", "500000");
        deactivate();
        closeReviewPopup();
        checkSaleStatus();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Снятие с продажи - чекбокс «Много спам звонков»")
    public void shouldDeactivateSaleManySpamCalls() {
        mockRule.with("desktop/UserOffersTrucksHideManySpamCalls").update();

        basePageSteps.onCardPage().cardOwnerPanel().button("Снять с продажи").click();
        basePageSteps.onCardPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onCardPage().soldPopup().checkbox("Много спам звонков").click();
        deactivate();
        closeReviewPopup();
        checkSaleStatus();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Снятие с продажи - чекбокс «Бесплатно составить договор купли-продажи»")
    public void shouldDeactivateSaleDkp() {
        mockRule.with("desktop/UserOffersTrucksHide").update();

        basePageSteps.onCardPage().cardOwnerPanel().button("Снять с продажи").click();
        basePageSteps.onCardPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onCardPage().soldPopup().checkbox("Бесплатно составить договор купли-продажи").click();
        basePageSteps.onCardPage().soldPopup().button("Снять с продажи").waitUntil(isEnabled()).click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(DOCS).path(DKP).addParam("sale_id", SALE_ID).shouldNotSeeDiff();
        urlSteps.switchToTab(0);
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Статус объявления изменен"));
        basePageSteps.onCardPage().soldPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onCardPage().notifier().waitUntil(not(isDisplayed()));
        closeReviewPopup();
        checkSaleStatus();
    }


    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Снятие с продажи - выбор номера телефона покупателя из списка")
    public void shouldDeactivateSaleBuyersPhoneFromList() {
        mockRule.with("desktop/UserOffersTrucksPredictBayers",
                "desktop/UserOffersTrucksHideBuyerPhone").update();

        basePageSteps.onCardPage().cardOwnerPanel().button("Снять с продажи").click();
        basePageSteps.onCardPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onCardPage().soldPopup().radioButton("+7 911 111-11-1110 июля 2020 в 17:26").click();
        deactivate();
        closeReviewPopup();
        checkSaleStatus();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Снятие с продажи - другой номера телефона покупателя")
    public void shouldDeactivateSaleBuyersOtherPhone() {
        mockRule.with("desktop/UserOffersTrucksPredictBayers",
                "desktop/UserOffersTrucksHideBuyerPhone").update();

        basePageSteps.onCardPage().cardOwnerPanel().button("Снять с продажи").click();
        basePageSteps.onCardPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onCardPage().soldPopup().radioButton("Другой номер").click();
        basePageSteps.onCardPage().soldPopup().input("Введите телефон покупателя", "+79111111111");
        deactivate();
        closeReviewPopup();
        checkSaleStatus();
    }

    @Step("Снимаем объявление с продажи")
    public void deactivate() {
        basePageSteps.onCardPage().soldPopup().button("Снять с продажи").waitUntil(isEnabled()).click();
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Статус объявления изменен"));
        basePageSteps.onCardPage().soldPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onCardPage().notifier().waitUntil(not(isDisplayed()));
    }

    @Step("Закрываем поп-ап «Оставить отзыв»")
    public void closeReviewPopup() {
        basePageSteps.onCardPage().reviewsPromo().cancelButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().reviewsPromo().waitUntil(not(isDisplayed()));
    }

    @Step("Проверяем, что объявление неактивно")
    public void checkSaleStatus() {
        basePageSteps.onCardPage().cardOwnerPanel().button("Снять с продажи").should(not(isDisplayed()));
        basePageSteps.onCardPage().cardOwnerPanel().button("Опубликовать").should(isDisplayed());
    }
}