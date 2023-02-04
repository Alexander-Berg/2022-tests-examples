package ru.yandex.general.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.Button;

public interface PaymentPage extends BasePage {

    String PAYMENT_SUCCESS = "Поднятие в поиске\nуспешно оплачено";
    String THANKS = "Спасибо";

    String NUMBER_INPUT = "card_number-input";
    String MONTH_INPUT = "card_month-input";
    String CVV_INPUT = "card_cvv-input";
    String YEAR_INPUT_NAME = "expiration_year";

    String NUMBER = "5469380041179762";
    String MONTH = "05";
    String YEAR = "22";
    String CVV = "126";

    @Name("Инпут с id = «{{ value }}»")
    @FindBy("//input[@id = '{{ value }}']")
    VertisElement inputId(@Param("value") String value);

    @Name("Инпут с name = «{{ value }}»")
    @FindBy("//input[@name = '{{ value }}']")
    VertisElement inputName(@Param("value") String value);

    @Name("Кнопка «Оплатить»")
    @FindBy("//div[@class = 'checkout-card-form__controls']//button[@title = 'Оплатить']")
    VertisElement pay();

    @Name("")
    @FindBy("//div[contains(@class, 'PaymentFormStatus__text')]")
    VertisElement statusText();

    default void fillCard() {
        inputId(NUMBER_INPUT).sendKeys(NUMBER);
        inputId(MONTH_INPUT).sendKeys(MONTH);
        inputName(YEAR_INPUT_NAME).sendKeys(YEAR);
        inputId(CVV_INPUT).sendKeys(CVV);
    }

}
