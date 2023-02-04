package ru.auto.tests.desktop.mobile.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface GeoSuggestPopup extends VertisElement, WithInput, WithButton {

    @Name("Кнопка «Отменить»")
    @FindBy(".//div[contains(@class, 'SearchTextInput__cancel')]")
    VertisElement cancelButton();

    @Name("Список регионов")
    @FindBy(".//div[@class = 'GeoSelectSuggestPopup__item']")
    ElementsCollection<VertisElement> regionsList();

    @Name("Регион, в названии которого есть «{{ text }}»")
    @FindBy(".//div[@class = 'GeoSelectSuggestPopup__item' and contains(., '{{ text }}')]")
    VertisElement regionContains(@Param("text") String Text);

    @Step("Получаем регион с индексом {i}")
    default VertisElement getRegion(int i) {
        return regionsList().should(hasSize(greaterThan(i))).get(i);
    }
}