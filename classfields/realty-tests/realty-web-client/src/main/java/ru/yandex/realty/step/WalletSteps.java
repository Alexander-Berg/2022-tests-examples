package ru.yandex.realty.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.yandex.realty.config.RealtyWebConfig;
import ru.yandex.realty.page.WalletPage;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.element.wallet.Cards.ADD_CARD;
import static ru.yandex.realty.element.wallet.CardsPopup.USE_CARD;

/**
 * @author kurau (Yuri Kalinin)
 */
public class WalletSteps extends WebDriverSteps {

    public static final String SECOND_CARD = "5555555555554444";
    public static final String SECOND_CARD_MASK = String.format("*** %s", SECOND_CARD.substring(12));
    public static final String ERROR_MESSAGE_OF_LONG_TIME_TRANSACTION = "Платеж обрабатывается уже более минуты";
    public static final String TRANSACTION_FAILED_MESSAGE = "Платеж не прошёл";

    public WalletPage onWalletPage() {
        return on(WalletPage.class);
    }

    @Inject
    private RealtyWebConfig config;

    @Step("Должна быть привязана тестовая банковская карта")
    public WalletSteps shouldSeeDefaultCard() {
        onWalletPage().cards()
                .cardTypeVISA()
                .waitUntil("Должна отобразиться карта в интерфейсе", isDisplayed(), 30);
        onWalletPage().cards()
                .cardTypeVISA()
                .should(hasText(String.format("**** **** **** %s", cardNumber().substring(12))));
        return this;
    }

    @Step("Платим тестовой картой через попап и запоминаем ее")
    public WalletSteps payWithCardAndRemember() {
        return addTestingCard(true);
    }


    @Step("Добавляем вторую карту")
    public WalletSteps addSecondCard() {
        onWalletPage().cards().button(ADD_CARD).click();
        switchToCardForm();
        onWalletPage().addCardForm().cardNumber().sendKeys(SECOND_CARD);
        onWalletPage().addCardForm().month().sendKeys("02");
        onWalletPage().addCardForm().year().sendKeys("23");
        onWalletPage().addCardForm().cardCvc().sendKeys("000");
        switchToPaymentPopup();
        rememberCard(true);
        payAndWaitSuccess();
        onWalletPage().cardsPopup().close().clickIf(isDisplayed());
        return this;
    }

    @Step("Платим тестовой картой через попап и не запоминаем ее")
    public WalletSteps payWithCardWithoutRemember() {
        return addTestingCard(false);
    }

    private WalletSteps addTestingCard(boolean remember) {
        switchToCardForm();
        fillsCardFieldsWithDefaultData();
        switchToPaymentPopup();
        rememberCard(remember);
        payAndWaitSuccess();
        return this;
    }

    @Step("Переключаемся в попап для оплаты")
    public void switchToPaymentPopup() {
        getDriver().switchTo().parentFrame();
    }

    @Step("Переключаемся на форму ввода данных с карты")
    public void switchToCardForm() {
        onWalletPage().addCardFormIFrame().should(isDisplayed());
        getDriver().switchTo().frame(getDriver().findElement(By.xpath("//iframe[contains(@class, 'KassaCardForm__iframe')]")));
    }

    @Step("Заполняем поля для тестовой карты")
    public void fillsCardFieldsWithDefaultData() {
        onWalletPage().addCardForm().cardNumber().sendKeys(cardNumber());
        onWalletPage().addCardForm().month().sendKeys("01");
        onWalletPage().addCardForm().year().sendKeys("23");
        onWalletPage().addCardForm().cardCvc().sendKeys("000");
    }

    @Step("Чекбокс «Запомнить» [{remember}]")
    public void rememberCard(boolean remember) {
        if (remember) {
            onWalletPage().cardsPopup().selectCheckBox(USE_CARD);
        } else {
            onWalletPage().cardsPopup().deselectCheckBox(USE_CARD);
        }
    }

    @Step("Платим кошельком")
    public void payWithWallet() {
        switchToPaymentPopup();
        onWalletPage().cardsPopup().paymentMethods().waitUntil(hasSize(greaterThan(1)))
                .filter(method -> method.getText().contains("Баланс кошелька")).waitUntil(hasSize(1)).get(0).click();
        payAndWaitSuccess();
    }


    @Step("Должны видеть первую транзакцию «{type}» на сумму «{amount}»")
    public void shouldSeeFirstTransaction(String type, String amount) {
        onWalletPage().transactionHistory().paymentList().get(0).type()
                .should("Тип транзакции не совпал", hasText(type));

        String text = onWalletPage().transactionHistory().paymentList().get(0).amount().getText();
        assertThat(text.replaceAll(" ", "")).as(
                "Потраченные деньги в транзакции не равны цене операции").contains(amount);
    }

    @Step("Кладем деньги {sum} на кошелек")
    public void addMoneyToWallet(String sum) {
        onWalletPage().balance().input().waitUntil(isDisplayed());
        onWalletPage().balance().clearButton().click();
        onWalletPage().balance().input().sendKeys(sum);
        onWalletPage().balance().submitButton().click();
    }

    @Step("Платим и ждем успешного завершения")
    public void payAndWaitSuccess() {
        onWalletPage().cardsPopup().paymentButton()
                .waitUntil("Жали когда появится кнопка оплаты", isDisplayed(), 30).click();
        onWalletPage().cardsPopup().spinVisible().waitUntil(exists());
        await().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .alias(ERROR_MESSAGE_OF_LONG_TIME_TRANSACTION).ignoreExceptions()
                .pollInterval(1, SECONDS).atMost(55, SECONDS)
                .until(() -> {
                    onWalletPage().cardsPopup().spinVisible().waitUntil(not(exists()));
                    return true;
                });
        onWalletPage().cardsPopup().successMessage().waitUntil(TRANSACTION_FAILED_MESSAGE, isDisplayed());
        onWalletPage().cardsPopup().close().clickIf(isDisplayed());
        onWalletPage().promocodePopup().spanLink("Выйти").clickIf(isDisplayed());
    }

    public String cardNumber() {
        return config.cardNumber();
    }

    public String cardNumberTemplate() {
        return String.format("*** %s", cardNumber().substring(12));
    }
}
