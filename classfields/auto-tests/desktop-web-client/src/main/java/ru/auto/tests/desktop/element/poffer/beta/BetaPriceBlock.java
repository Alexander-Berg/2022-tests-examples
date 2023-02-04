package ru.auto.tests.desktop.element.poffer.beta;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import org.openqa.selenium.Keys;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithSelect;

public interface BetaPriceBlock extends VertisElement, WithCheckbox, WithSelect {

    String EXCHANGE = "Возможен обмен";
    String VAT = "Учитывать НДС";

    @Name("Поле ввода цены")
    @FindBy(".//div[@id='price.price']//input[not(@name='currency')]")
    VertisElement priceInput();

    @Name("Ошибка в цене")
    @FindBy(".//div[@id='price.price']//span[@class='TextInput__error']")
    VertisElement priceError();

    @Name("Иконка информации про обмен")
    @FindBy(".//*[contains(@class, 'OfferFormExchangeField__helpIcon')]")
    VertisElement exchangeInfoIcon();

    @Name("Иконка информации про НДС")
    @FindBy(".//*[contains(@class, 'OfferFormWithNdsField__helpIcon')]")
    VertisElement vatInfoIcon();

    @Step("Очищаем текстовое поле")
    default void clearPriceInput() {
        priceInput().clear();
        priceInput().sendKeys(Keys.HOME, Keys.chord(Keys.SHIFT, Keys.END, Keys.DELETE));
    }

}
