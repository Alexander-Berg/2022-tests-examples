package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SuggestDropdown extends VertisElement, Button, Checkbox, Link {

    String MAP = "Карта";
    String METRO = "Метро";
    String DISTRICT = "Район";
    String IN = "+";
    String OUT = "–";
    String RADIUS = "Радиус";

    @Name("Метро «{{ value }}»")
    @FindBy(".//*[@class = 'MetroMap_text'][contains(., '{{ value }}')]")
    VertisElement station(@Param("value") String value);

    @Name("Zoom «{{ value }}»")
    @FindBy("//div[contains(@class, 'MovableMap__zoom')]//button[contains(., '{{ value }}')]")
    VertisElement zoom(@Param("value") String value);

    @Name("Слайдер радиус-пикера")
    @FindBy(".//div[contains(@class, 'RadiusPicker__slider')]")
    VertisElement radiusSlider();

}
