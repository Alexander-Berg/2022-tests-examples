package ru.yandex.general.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Input extends VertisElement {

    String URL = "url";
    String FILE = "file";
    String VALUE = "value";

    @Name("Инпут")
    @FindBy(".//input")
    InputClear input();

    @Name("Инпут тип «{{ value }}»")
    @FindBy(".//input[@type = '{{ value }}']")
    VertisElement inputWithType(@Param("value") String value);

    @Name("Список инпутов")
    @FindBy(".//input")
    ElementsCollection<VertisElement> inputList();

    @Name("Текстовая область")
    @FindBy(".//textarea")
    VertisElement textarea();

    @Name("Очистка текстовой области")
    @FindBy(".//*[contains(@class, '__iconClose')]")
    VertisElement clearTextarea();

    @Name("Инпут «{{ value }}»")
    @FindBy(".//input[contains(@placeholder, '{{ value }}')]")
    InputClear input(@Param("value") String value);

    @Name("Подсказка под инпутом «{{ value }}»")
    @FindBy(".//span[contains(@class, 'Textinput-Hint')][contains(., '{{ value }}')]")
    VertisElement inputHint(@Param("value") String value);

}
