package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.element.Popup;
import ru.auto.tests.desktop.mobile.element.WithInput;

public interface NotePopup extends Popup, WithInput {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'sale-note__title')]")
    VertisElement title();
}
