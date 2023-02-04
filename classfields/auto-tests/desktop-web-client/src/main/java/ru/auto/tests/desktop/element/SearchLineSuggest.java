package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface SearchLineSuggest extends VertisElement, WithInput {

    @Name("Список подсказок")
    @FindBy(".//div[@class = 'RichInput__suggest-item RichInput__suggest-item_theme_default']")
    ElementsCollection<VertisElement> itemsList();

    @Step("Получаем подсказку с индексом {i}")
    default VertisElement getItem(int i) {
        return itemsList().should(hasSize(greaterThan(i))).get(i);
    }
}