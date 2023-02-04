package ru.auto.tests.desktop.lk.sales.reseller;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Снятие с продажи. Мото")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_RESELLER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class HideMotoOfferTest {

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
                "desktop-lk/UserFavoriteReseller",
                "desktop-lk/UserOffersMotoActive").post();

        basePageSteps.setWideWindowSize();

        urlSteps.testing().path(MY).path(RESELLER).path(MOTO).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Снятие с продажи")
    public void shouldDeactivateSale() {
        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop-lk/UserFavoriteReseller",
                "desktop-lk/UserOffersMotoInactive",
                "desktop-lk/UserOffersMotoHide").post();

        basePageSteps.mouseOver(basePageSteps.onLkResellerSalesPage().getSale(0));
        basePageSteps.onLkResellerSalesPage().getSale(0).controlsColumn().hideIcon().should(isDisplayed()).click();

        basePageSteps.onLkResellerSalesPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onLkResellerSalesPage().soldPopup().radioButton("Продал на Авто.ру").click();

        deactivate();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Снятие с продажи с указанием цены продажи")
    public void shouldDeactivateSaleSoldPrice() {
        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop-lk/UserFavoriteReseller",
                "desktop-lk/UserOffersMotoInactive",
                "desktop-lk/UserOffersMotoHideSoldPrice").post();

        basePageSteps.mouseOver(basePageSteps.onLkResellerSalesPage().getSale(0));
        basePageSteps.onLkResellerSalesPage().getSale(0).controlsColumn().hideIcon().should(isDisplayed()).click();
        basePageSteps.onLkResellerSalesPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onLkResellerSalesPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onLkResellerSalesPage().soldPopup().input("Стоимость, \u20BD", "500000");
        deactivate();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Снятие с продажи - выбор номера телефона покупателя из списка")
    public void shouldDeactivateSaleBuyersPhoneFromList() {
        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop-lk/UserFavoriteReseller",
                "desktop-lk/UserOffersMotoInactive",
                "desktop-lk/UserOffersMotoPredictBuyers",
                "desktop-lk/UserOffersMotoHideBuyerPhone").post();

        basePageSteps.mouseOver(basePageSteps.onLkResellerSalesPage().getSale(0));
        basePageSteps.onLkResellerSalesPage().getSale(0).controlsColumn().hideIcon().should(isDisplayed()).click();
        basePageSteps.onLkResellerSalesPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onLkResellerSalesPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onLkResellerSalesPage().soldPopup().radioButton("+7 911 111-11-1110 июля 2020 в 17:26").click();
        deactivate();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Снятие с продажи - другой номера телефона покупателя")
    public void shouldDeactivateSaleBuyersOtherPhone() {
        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop-lk/UserFavoriteReseller",
                "desktop-lk/UserOffersMotoInactive",
                "desktop-lk/UserOffersMotoPredictBuyers",
                "desktop-lk/UserOffersMotoHideBuyerPhone").post();

        basePageSteps.mouseOver(basePageSteps.onLkResellerSalesPage().getSale(0));
        basePageSteps.onLkResellerSalesPage().getSale(0).controlsColumn().hideIcon().should(isDisplayed()).click();
        basePageSteps.onLkResellerSalesPage().soldPopup().waitUntil(isDisplayed());
        basePageSteps.onLkResellerSalesPage().soldPopup().radioButton("Продал на Авто.ру").click();
        basePageSteps.onLkResellerSalesPage().soldPopup().radioButton("Другой номер").click();
        basePageSteps.onLkResellerSalesPage().soldPopup().input("Введите телефон покупателя", "+79111111111");
        deactivate();
    }

    @Step("Снимаем объявление с продажи")
    private void deactivate() {
        basePageSteps.onLkResellerSalesPage().soldPopup().button("Снять с продажи").click();
        basePageSteps.onLkResellerSalesPage().notifier().waitUntil(hasText("Статус объявления изменен"));
        basePageSteps.onLkResellerSalesPage().soldPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onLkResellerSalesPage().notifier().waitUntil(not(isDisplayed()));
        basePageSteps.onLkResellerSalesPage().reviewsPromo().cancelButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkResellerSalesPage().reviewsPromo().waitUntil(not(isDisplayed()));
        basePageSteps.onLkResellerSalesPage().getSale(0).datesColumn().createDate()
                .waitUntil(hasText("Снято с публикации 3 июня 2019"));
        basePageSteps.onLkResellerSalesPage().getSale(0).status().text().waitUntil(hasText("Объявление готово к публикации"));
        basePageSteps.onLkResellerSalesPage().getSale(0).status().button("Опубликовать объявление").waitUntil(isDisplayed());
    }
}
