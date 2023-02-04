package ru.auto.tests.desktop.step;

import io.qameta.allure.Step;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.element.NewTrust.PAYMENT_SUCCESS;
import static ru.auto.tests.desktop.utils.Utils.formatPrice;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

public class NewTrustSteps extends BasePageSteps {

    public static final String TEST_CARD_NUMBER = "5100004623272515";
    public static final String TEST_CARD_MONTH = "12";
    public static final String TEST_CARD_YEAR = "23";
    public static final String TEST_CARD_CVC_CODE = "900";

    @Step("Оплачиваем картой")
    public void payWithCard() {
        onBasePage().newTrustFrame().waitUntil("Не подгрузился фрейм", isDisplayed(), 25);
        onBasePage().switchToNewTrustFrame();
        onBasePage().newTrust().cardNumberInput().sendKeys(TEST_CARD_NUMBER);
        onBasePage().newTrust().monthInput().sendKeys(TEST_CARD_MONTH);
        onBasePage().newTrust().yearInput().sendKeys(TEST_CARD_YEAR);
        onBasePage().newTrust().cvcInput().sendKeys(TEST_CARD_CVC_CODE);
        switchToDefaultFrame();
        onBasePage().switchToBillingFrame();
        onBasePage().newTrust().payButton().waitUntil(isDisplayed()).click();
        switchToDefaultFrame();
    }

    @Step("Оплачиваем привязанной картой")
    public void payWithTiedCard() {
        onBasePage().newTrust().payButton().waitUntil(isDisplayed()).click();
        onBasePage().newTrustFrame().waitUntil("Не подгрузился фрейм", isDisplayed(), 10);
        onBasePage().switchToNewTrustFrame();
        onBasePage().newTrust().cvcInput().sendKeys(TEST_CARD_CVC_CODE);
        switchToDefaultFrame();
        onBasePage().switchToBillingFrame();
        onBasePage().newTrust().payButton().waitUntil(isDisplayed()).click();
        switchToDefaultFrame();
    }

    @Step("Ждём сообщение об успешной оплате")
    public void waitForSuccessMessage() {
        onBasePage().switchToBillingFrame();
        onBasePage().newTrust().statusTitle()
                .waitUntil("Платёж не прошёл", hasText(PAYMENT_SUCCESS), 90);
        switchToDefaultFrame();
    }

    public void payForVas(String vasName, int vasPrice, String notificationText) {
        onLkSalesPage().switchToBillingFrame();

        onLkSalesPage().billingPopup().waitUntil(isDisplayed());
        onLkSalesPage().newTrust().title().waitUntil(hasText(vasName), 15);
        onLkSalesPage().newTrust().price().waitUntil(hasText(formatPrice(vasPrice)));

        payWithCard();
        waitForSuccessMessage();

        onLkSalesPage().notifier().waitUntil(isDisplayed())
                .waitUntil(hasText(notificationText));

        onLkSalesPage().billingPopupCloseButton().waitUntil(isDisplayed()).click();
        onLkSalesPage().billingPopupFrame().waitUntil(not(isDisplayed()));
    }

}
