package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface PricePopup extends VertisElement, WithButton {

    String APPLY = "Применить";

    @Name("Поле ввода цены")
    @FindBy(".//label[contains(@class,'SalesItemPriceNewDesign__input')]//input")
    VertisElement priceInput();

    @Name("Кнопка очистки инпута")
    @FindBy(".//label[contains(@class,'SalesItemPriceNewDesign__input')]//i")
    VertisElement priceCleanButton();

}
