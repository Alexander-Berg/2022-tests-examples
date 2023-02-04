package ru.auto.tests.desktop.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;

public interface AddOptionsPopup extends VertisElement, WithButton, WithCheckbox {

    @Name("Заголовок")
    @FindBy(".//h2")
    VertisElement title();

    @Name("Кнопка закрытия")
    @FindBy(".//*[contains(@class, 'IconSvg_close-small')]")
    VertisElement closeButton();
}