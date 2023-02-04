package ru.auto.tests.desktop.element.main;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * Created by kopitsa on 19.09.17.
 */
public interface GeoSelectPopup extends VertisElement {

    @Name("Кнопка «Сохранить»")
    @FindBy(".//button[contains(@class, 'Button_place_bottom')]")
    VertisElement confirmButton();

    @Name("Кнопка радиуса «{{ text }}»")
    @FindBy(".//span[contains(@class, 'Slider__item-content') and .= '{{ text }}']")
    VertisElement radiusButton(@Param("text") String text);

    @Name("Дефолтный регион «{{ text }}»")
    @FindBy(".//div[contains(@class, 'GeoSelectPopup__regions')]" +
            "/button[contains(@class, 'Button_color_white')]/span[.='{{ text }}']")
    VertisElement defaultRegion(@Param("text") String text);

    @Name("Выбранный регион «{{ text }}»")
    @FindBy(".//div[contains(@class, 'GeoSelectPopup__regions')]" +
            "/button[contains(@class, 'Button_color_blue')]/span[.='{{ text }}']")
    VertisElement selectedRegion(@Param("text") String text);

    @Name("Инпут региона")
    @FindBy(".//input")
    VertisElement regionInput();

    @Name("Элемент саджеста")
    @FindBy("//div[contains(@class, 'GeoSelectPopup__suggest-item-region') and .='{{ text }}']")
    VertisElement suggestItem(@Param("text") String text);
}
