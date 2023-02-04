package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface CrossLinksBlock extends VertisElement, WithButton {

    @Name("Заголовок блока")
    @FindBy(".//h3[@class = 'CrossLinks__title']")
    VertisElement title();

    @Name("Рейтинг модели или марки")
    @FindBy(".//div[@class = 'CrossLinks__rating']")
    VertisElement rating();

}
