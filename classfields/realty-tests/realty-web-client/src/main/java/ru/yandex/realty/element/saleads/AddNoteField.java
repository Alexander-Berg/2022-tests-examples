package ru.yandex.realty.element.saleads;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.RealtyElement;

/**
 * Created by kopitsa on 27.07.17.
 */
public interface AddNoteField extends RealtyElement {

    @Name("Поле для ввода")
    @FindBy(".//input")
    AtlasWebElement input();

    @Name("Кнопка сохранения заметки")
    @FindBy("./div[@class = 'add-note__save']")
    AtlasWebElement saveButton();

    @Name("Кнопка удаления заметки")
    @FindBy("./div[@class = 'add-note__remove']")
    AtlasWebElement deleteButton();

    @Name("Лейбл с текстом")
    @FindBy("./label")
    AtlasWebElement label();
}
