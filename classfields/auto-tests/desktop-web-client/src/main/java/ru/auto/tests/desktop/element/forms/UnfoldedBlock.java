package ru.auto.tests.desktop.element.forms;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithGeoSuggest;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithRadioButton;
import ru.auto.tests.desktop.component.WithSelect;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface UnfoldedBlock extends VertisElement, WithButton, WithRadioButton, WithInput, WithCheckbox, WithGeoSuggest,
        WithSelect {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'FormSection__Title')]")
    VertisElement title();

    @Name("Список результатов поиска")
    @FindBy(".//div[contains(@class, 'FormSection__ListItemText')] | " +
            ".//span[contains(@class, 'Radio__text')]")
    ElementsCollection<VertisElement> searchResultsList();

    @Step("Получаем результат поиска с индексом {i}")
    default VertisElement getSearchResult(int i) {
        return searchResultsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Цвет «{{ color }}»")
    @FindBy(".//div[contains(@class, 'ColorItem_{{ color }}')]")
    VertisElement color(@Param("color") String color);

    @Name("Сообщение об ошибке")
    @FindBy(".//span[contains(@class, 'TextInput__error')]")
    VertisElement errorMessage();

    @Name("Список телефонов")
    @FindBy(".//div[contains(@class, 'PhonesItem')]/label")
    ElementsCollection<VertisElement> phonesList();

    @Step("Получаем телефон с индексом {i}")
    default VertisElement getPhone(int i) {
        return phonesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Тег «{{ text }}» в блоке описания")
    @FindBy(".//div[@class = 'DescriptionItem' and .= '{{ text }}']")
    VertisElement descriptionTag(@Param("text") String text);

    @Name("Рейтинг «{{ text }}»")
    @FindBy(".//div[@class = 'ReviewRatingsForms__rating' and .//div[.= '{{ text }}']]")
    Rating rating(@Param("text") String text);

    @Name("Поп-ап плюсов/минусов")
    @FindBy(".//div[contains(@class, 'ProsAndConsItem__list ')]")
    PlusMinusPopup plusMinusPopup();

    @Name("Иконка подсказки НДС")
    @FindBy(".//div[contains(@class, 'PriceInput__tooltip')]")
    VertisElement priceVatInfoButton();

    @Name("Список инпутов")
    @FindBy(".//input")
    ElementsCollection<VertisElement> inputsList();

    @Step("Получаем инпут с индексом {i}")
    default VertisElement getInput(int i) {
        return inputsList().should(hasSize(greaterThan(i))).get(i);
    }
}
