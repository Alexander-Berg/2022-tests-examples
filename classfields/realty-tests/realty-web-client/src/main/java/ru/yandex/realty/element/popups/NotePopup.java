package ru.yandex.realty.element.popups;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

public interface NotePopup extends Button {

    String SAVE_NOTE = "Сохранить заметку";
    String DELETE = "Удалить";

    @Name("Поле ввода")
    @FindBy(".//textarea")
    AtlasWebElement inputField();
}
