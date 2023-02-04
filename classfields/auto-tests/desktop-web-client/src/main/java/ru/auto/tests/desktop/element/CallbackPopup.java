package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CallbackPopup extends VertisElement, WithInput, WithButton {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'popupTitle')]")
    VertisElement title();

    @Name("Саджест телефонов")
    @FindBy(".//div[contains(@class, 'RichInput__suggest')]")
    VertisElement phonesSuggest();

    @Name("Список телефонов в саджесте")
    @FindBy(".//div[contains(@class, 'RichInput__suggest-item')]")
    ElementsCollection<VertisElement> phonesList();

    @Step("Получаем телефон с индексом {i}")
    default VertisElement getPhone(int i) {
        return phonesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Поле с ошибкой")
    @FindBy(".//span[contains(@class, 'TextInput__error')]")
    VertisElement error();

    @Name("Кнопка очистки инпута")
    @FindBy(".//i[contains(@class, 'TextInput__clear_visible')]")
    VertisElement clearInputButton();
}