package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface AutoProlongDiscountBanner extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, '__title')]")
    VertisElement title();

    @Name("Информация о стоимости")
    @FindBy(".//div[contains(@class, '__infoItem')]")
    VertisElement priceInfo();
}