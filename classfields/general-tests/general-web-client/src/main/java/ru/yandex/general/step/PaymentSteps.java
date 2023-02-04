package ru.yandex.general.step;

import java.util.concurrent.TimeUnit;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

public class PaymentSteps extends BasePageSteps {

    public void switchToFrame() {
        getDriver().switchTo().frame(0);
        getDriver().switchTo().frame(0);
    }

    public void switchToParentFrame() {
        getDriver().switchTo().parentFrame();
    }

    public void makePayment() {
        switchToFrame();
        onPaymentPage().fillCard();
        switchToParentFrame();
        onPaymentPage().pay().click();
        waitSomething(5, TimeUnit.SECONDS);
        switchToParentFrame();
        onPaymentPage().statusText().waitUntil(isDisplayed());
    }

}
