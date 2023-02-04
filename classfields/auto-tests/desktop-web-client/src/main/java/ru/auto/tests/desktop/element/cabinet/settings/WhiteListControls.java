package ru.auto.tests.desktop.element.cabinet.settings;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface WhiteListControls extends VertisElement, WithButton {

    @Name("Чекбокс выбора всех телефонов")
    @FindBy("./label[contains(@class, 'Whitelist__selectAllCheckbox')]")
    VertisElement selectAllPhonesCheckbox();

}
