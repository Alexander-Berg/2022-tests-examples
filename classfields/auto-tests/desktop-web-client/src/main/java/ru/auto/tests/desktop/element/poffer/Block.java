package ru.auto.tests.desktop.element.poffer;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithRadioButton;
import ru.auto.tests.desktop.component.WithSelect;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Block extends VertisElement, WithButton, WithRadioButton, WithInput, WithCheckbox,
        WithSelect {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'FormSection__Title')] | " +
            ".//div[contains(@class, 'section-title')]")
    VertisElement title();

    @Name("Элемент «{{ text }}» в списке")
    @FindBy(".//div[contains(@class, 'menu-item') and .= '{{ text }}']")
    VertisElement listItem(@Param("text") String Text);

    @Name("Цвет «{{ color }}»")
    @FindBy(".//label[.//span[contains(@style, '{{ color }}')]]")
    VertisElement color(@Param("color") String color);

    @Name("Сообщение об ошибке")
    @FindBy(".//div[contains(@class, 'error-text') and not(contains(@class, 'hidden'))] | " +
            ".//div[contains(@class, 'video__error')] | " +
            ".//span[contains(@class, 'TextInput__error')]")
    VertisElement errorMessage();

    @Name("Список телефонов")
    @FindBy(".//div[contains(@class, 'PhonesItem')]/label")
    ElementsCollection<VertisElement> phonesList();

    @Name("Тег «{{ text }}» в блоке описания")
    @FindBy(".//div[@class = 'DescriptionItem' and .= '{{ text }}']")
    VertisElement descriptionTag(@Param("text") String text);

    @Name("Список инпутов")
    @FindBy(".//input")
    ElementsCollection<VertisElement> inputsList();

    @Name("Икнока ?")
    @FindBy(".//a[contains(@class, 'dropdown')]")
    VertisElement helpIcon();

    @Step("Получаем инпут с индексом {i}")
    default VertisElement getInput(int i) {
        return inputsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Получаем телефон с индексом {i}")
    default VertisElement getPhone(int i) {
        return phonesList().should(hasSize(greaterThan(i))).get(i);
    }
}