package ru.auto.tests.desktop.element.group;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface GroupColorsPopup extends VertisElement {

    @Name("Список цветов")
    @FindBy("//div[contains(@class, 'ColorSelectorItem')]")
    ElementsCollection<VertisElement> colorsList();

    @Step("Получаем цвет с индексом {i}")
    default VertisElement getColor(int i) {
        return colorsList().should(hasSize(greaterThan(i))).get(i);
    }
}