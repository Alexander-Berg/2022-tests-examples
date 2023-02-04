package ru.auto.tests.desktop.mobile.element.poffer;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import org.openqa.selenium.Keys;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithSelect;
import ru.auto.tests.desktop.mobile.element.WithInput;

public interface PriceBlock extends VertisElement, WithInput, WithSelect {

    @Name("Поле ввода цены")
    @FindBy(".//div[@id='price.price']//input[not(@name='currency')]")
    VertisElement priceInput();

    @Name("Ошибка в цене")
    @FindBy(".//div[@id='price.price']//span[@class='TextInput__error']")
    VertisElement priceError();

    @Step("Очищаем текстовое поле")
    default void clearPriceInput() {
        priceInput().clear();
        priceInput().sendKeys(Keys.HOME, Keys.chord(Keys.SHIFT, Keys.END, Keys.DELETE));
    }

}
