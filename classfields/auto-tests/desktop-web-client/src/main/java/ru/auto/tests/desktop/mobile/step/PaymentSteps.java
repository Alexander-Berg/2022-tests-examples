package ru.auto.tests.desktop.mobile.step;

import io.qameta.allure.Step;
import org.awaitility.Duration;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.desktop.mobile.page.PaymentPage;

import java.time.LocalDate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static ru.auto.tests.desktop.TestCard.TEST_CARD_CVC_CODE;
import static ru.auto.tests.desktop.TestCard.TEST_CARD_NUMBER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

/**
 * @author Anton Tsyganov (jenkl)
 */
public class PaymentSteps extends BasePageSteps {

    public PaymentPage onPaymentPage() {
        return on(PaymentPage.class);
    }

    @Step("Платим тестовой картой")
    public PaymentSteps payByCard() {

        switchToPaymentMethodsFrame();

        switchToYaKassaFrame();

        onPaymentPage().yaKassaFrame().waitUntil("Не появилась форма для ввода данных карты", isDisplayed(), 10);

        addTestingCard();
        onPaymentPage().yaKassaFrame().pay().click();

        switchToDefaultFrame();

        return this;
    }

    @Step("Ждём сообщение об успешной оплате")
    public PaymentSteps waitForSuccessMessage() {
        switchToPaymentMethodsFrame();

        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollDelay(Duration.ZERO)
                .pollInterval(100, MILLISECONDS)
                .atMost(10, SECONDS)
                .until(() -> onPaymentPage().successMessage(), hasText("Платёж совершён успешно"));

        switchToDefaultFrame();

        return this;
    }

    private void addTestingCard() {
        fillsCardFieldsWithDefaultData();
        //dontRememberCard();
    }

    @Step("Заполняем поля для тестовой карты")
    private void fillsCardFieldsWithDefaultData() {
        onPaymentPage().yaKassaFrame().cardNumber()
                .waitUntil("Ждем пока отобразиться поле ввода номера карты", exists(), 60)
                .sendKeys(TEST_CARD_NUMBER);

        LocalDate now = LocalDate.now().plusYears(2);

        onPaymentPage().yaKassaFrame().month().sendKeys("12");
        onPaymentPage().yaKassaFrame().year().sendKeys("25");
        onPaymentPage().yaKassaFrame().cardCvc().sendKeys(TEST_CARD_CVC_CODE);
    }

    @Step("Снимаем чекбокс «Запомнить» [{remember}]")
    private void dontRememberCard() {
        getDriver().switchTo().defaultContent();
        switchToPaymentMethodsFrame();

        //onPaymentPage().rememberCheckbox().waitUntil(isEnabled()).click();
        onPaymentPage().checkbox("Всегда оплачивать с этой карты").waitUntil(isEnabled()).click();
        switchToYaKassaFrame();
    }
}
