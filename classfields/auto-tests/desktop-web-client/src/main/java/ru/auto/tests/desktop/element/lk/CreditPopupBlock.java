package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;

public interface CreditPopupBlock extends VertisElement, WithInput {

    @Name("Иконка редактирования цены")
    @FindBy(".//span[@class = 'PriceInput__icon']")
    VertisElement priceEditIcon();

    @Name("Выбранное значение")
    @FindBy(".//span[contains(@class, '_fieldValue')]")
    VertisElement value();

    @Name("Слайдер")
    @FindBy(".//div[@class = 'Slider__click-bar']")
    VertisElement slider();

}
