package ru.auto.tests.desktop.element.listing;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithRadioButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface SelectPopup extends VertisElement, WithRadioButton {

    @Name("Пункт «{{ text }}»")
    @FindBy(".//div[contains(@class, 'MenuItem') and .='{{ text }}'] |" +
            ".//div[contains(@class, 'menu-item') and .='{{ text }}']")
    VertisElement item(@Param("text") String text);

    @Name("Список пунктов")
    @FindBy(".//div[contains(@class, 'MenuItem')] |" +
            ".//div[contains(@class, 'menu-item')] | " +
            ".//div[contains(@class, 'ColorSelectorItem')] | " +
            ".//div[contains(@class, 'MenuItem')]//div[@class = 'MenuItemGroup__children']")
    ElementsCollection<VertisElement> itemsList();

    @Name("Кнопка «+» в пункте «{{ text }}»")
    @FindBy(".//div[contains(@class, 'MenuItem') and .='{{ text }}']/following-sibling::button")
    VertisElement plusButton(@Param("text") String text);

    @Step("Получаем пункт с индексом {i}")
    default VertisElement getItem(int i) {
        return itemsList().should(hasSize(greaterThan(i))).get(i);
    }
}