package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.InputClear;

public interface Input extends VertisElement {

    String PLACEHOLDER = "placeholder";
    String DISABLED = "disabled";

    @Name("Инпут")
    @FindBy(".//input")
    InputClear input();

    @Name("Список инпутов")
    @FindBy(".//input")
    ElementsCollection<VertisElement> inputList();

    @Name("Текстовая область")
    @FindBy(".//textarea")
    VertisElement textarea();

    @Name("Очистка текстовой области")
    @FindBy(".//*[contains(@class, 'Textarea__iconClose')]")
    VertisElement clearTextarea();

    @Name("Инпут «{{ value }}»")
    @FindBy(".//input[contains(@placeholder, '{{ value }}')]")
    InputClear input(@Param("value") String value);

    @Name("Инпут с плавающим плейсхолдером «{{ value }}»")
    @FindBy(".//span[span[contains(@class, 'Placeholder')][contains(., '{{ value }}')]]//input")
    InputClear inputWithFloatedPlaceholder(@Param("value") String value);

    @Name("Инпут тип «{{ value }}»")
    @FindBy(".//input[@type = '{{ value }}']")
    VertisElement inputWithType(@Param("value") String value);

    @Name("Пустой инпут")
    @FindBy(".//input[@value = '']")
    VertisElement emptyInput();

    @Name("Подсказка под инпутом «{{ value }}»")
    @FindBy(".//span[contains(@class, 'Textinput-Hint')][contains(., '{{ value }}')]")
    VertisElement inputHint(@Param("value") String value);

    @Name("Подсказка под инпутом")
    @FindBy(".//span[contains(@class, 'Textinput-Hint')]")
    VertisElement inputHint();

    default void setInput(String value) {
        int valueUntilChangeLength = input().getAttribute("value").length();
        StringBuilder builder = new StringBuilder(valueUntilChangeLength);
        for(int i = 0; i < valueUntilChangeLength; i++) {
            builder.append("\b");
        }
        input().sendKeys(builder.toString() + value);
    }

    default void setTextarea(String value) {
        int valueUntilChangeLength = textarea().getText().length();
        StringBuilder builder = new StringBuilder(valueUntilChangeLength);
        for(int i = 0; i < valueUntilChangeLength; i++) {
            builder.append("\b");
        }
        textarea().sendKeys(builder.toString() + value);
    }

}
