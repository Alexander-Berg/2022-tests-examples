package ru.yandex.realty.mobile.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import ru.yandex.realty.config.RealtyWebConfig;
import ru.yandex.realty.mobile.page.PaymentPopupPage;
import ru.yandex.realty.step.CommonSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.mobile.page.PaymentPopupPage.REMEMBER_CARD;

public class PaymentSteps extends CommonSteps {

    @Inject
    private RealtyWebConfig config;

    public PaymentPopupPage onPaymentPopupPage() {
        return on(PaymentPopupPage.class);
    }

    @Step("Переключаемся в попап для оплаты")
    public void switchToPaymentPopup() {
        getDriver().switchTo().parentFrame();
    }

    @Step("Переключаемся на форму ввода данных с карты")
    public void switchToCardForm() {
        onPaymentPopupPage().addCardFormIFrame().should(isDisplayed());
        getDriver().switchTo().frame(getDriver().findElement(By.xpath("//iframe[contains(@class, 'YandexKassaCardForm__iframe')]")));
    }

    @Step("Заполняем поля для тестовой карты")
    public void fillsCardFieldsWithDefaultData() {
        onPaymentPopupPage().addCardForm().cardNumber().sendKeys(cardNumber());
        onPaymentPopupPage().addCardForm().month().sendKeys("01");
        onPaymentPopupPage().addCardForm().year().sendKeys("22");
        onPaymentPopupPage().addCardForm().cardCvc().sendKeys("000");
    }

    @Step("Оплата картой без запоминанния")
    public void defaultPay() {
        onPaymentPopupPage().paymentVisible().waitUntil(isDisplayed());
        onPaymentPopupPage().payButton().click();
        onPaymentPopupPage().deselectCheckBox(REMEMBER_CARD);
        switchToCardForm();
        fillsCardFieldsWithDefaultData();
        switchToPaymentPopup();
        onPaymentPopupPage().payButton().click();
        onPaymentPopupPage().successContainer().waitUntil(isDisplayed(), 30);
    }

    @Step("Оплата картой без запоминанния")
    public void promocodePay() {
        onPaymentPopupPage().paymentVisible().waitUntil(isDisplayed());
        onPaymentPopupPage().payButton().click();
    }

    public String cardNumber() {
        return config.cardNumber();
    }
}
