package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PriceBlock extends VertisElement {

    @Name("Цена")
    @FindBy(".//span[contains(@class, 'SalePrice__staticPrice')]")
    VertisElement price();

    @Name("Кнопка редактирования цены")
    @FindBy(".//*[contains(@class, 'SalePrice__icon_edit')]")
    VertisElement editIcon();

    @Name("Инпут цены")
    @FindBy(".//div[@class = 'Sale__header']//input")
    VertisElement priceInput();

    default int offerPrice() {
        return Integer.parseInt(price().getText().replaceAll(" ", "").replaceAll("\u20BD", ""));
    }
}
