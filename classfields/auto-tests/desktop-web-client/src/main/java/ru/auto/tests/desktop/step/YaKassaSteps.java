package ru.auto.tests.desktop.step;

import io.qameta.allure.Step;

import static ru.auto.tests.desktop.TestCard.TEST_CARD_CVC_CODE;
import static ru.auto.tests.desktop.TestCard.TEST_CARD_MONTH;
import static ru.auto.tests.desktop.TestCard.TEST_CARD_NUMBER;
import static ru.auto.tests.desktop.TestCard.TEST_CARD_YEAR;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

public class YaKassaSteps extends BasePageSteps {

    @Step("Оплачиваем картой")
    public void payWithCard() {
        onBasePage().yaKassaFrame().waitUntil("Не подгрузился фрейм Я.Кассы", isDisplayed(), 10);
        onBasePage().switchToYaKassaFrame();
        onBasePage().yaKassa().cardNumberInput().sendKeys(TEST_CARD_NUMBER);
        onBasePage().yaKassa().monthInput().sendKeys(TEST_CARD_MONTH);
        onBasePage().yaKassa().yearInput().sendKeys(TEST_CARD_YEAR);
        onBasePage().yaKassa().cvcInput().sendKeys(TEST_CARD_CVC_CODE);
        onBasePage().yaKassa().payButton().waitUntil(isDisplayed()).click();
        switchToDefaultFrame();
    }

    @Step("Ждём сообщение об успешной оплате")
    public void waitForSuccessMessage() {
        onBasePage().switchToBillingFrame();
        onBasePage().billingPopup().successMessage()
                .waitUntil("Платёж не прошёл", hasText("Платёж совершён успешно"), 20);
        switchToDefaultFrame();
    }
}