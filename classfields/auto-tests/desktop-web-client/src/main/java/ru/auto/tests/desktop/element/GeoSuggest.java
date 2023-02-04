package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface GeoSuggest extends Popup {

    @Name("Регион «{{ text }}»")
    @FindBy("//div[contains(@class, 'GeoSuggest__suggest-item-region') and .='{{ text }}'] | " +
            "//div[contains(@class, 'Suggest__item') and .='{{ text }}'] | " +
            "//div[contains(@class, 'suggest__item') and .='{{ text }}'] |" +
            "//div[contains(@class, 'suggest-item') and .='{{ text }}']")
    VertisElement region(@Param("text") String Text);

    @Name("Регион, в названии которого есть «{{ text }}»")
    @FindBy("//div[contains(@class, 'GeoSuggest__suggest-item-region') and contains(., '{{ text }}')] | " +
            "//div[contains(@class, 'Suggest__item') and contains(., '{{ text }}')] | " +
            "//div[contains(@class, 'suggest__item') and contains(., '{{ text }}')] |" +
            "//div[contains(@class, 'suggest-item') and contains(., '{{ text }}')]")
    VertisElement regionContains(@Param("text") String Text);

    @Name("Список регионов")
    @FindBy(".//div[contains(@class, 'RichInput__suggest-item')]")
    ElementsCollection<VertisElement> itemsList();

    @Step("Получаем регион с индексом {i}")
    default VertisElement getItem(int i) {
        return itemsList().should(hasSize(greaterThan(i))).get(i);
    }
}