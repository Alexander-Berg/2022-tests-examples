package ru.yandex.realty.element.developer;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface Slider extends AtlasWebElement {

    @Name("Слайды")
    @FindBy(".//div[contains(@class, 'slide_')]")
    ElementsCollection<Slide> slides();

    @Name("Стрелка влево")
    @FindBy(".//div[contains(@class, 'arrowLeft')]")
    AtlasWebElement arrowLeft();

    @Name("Стрелка вправо")
    @FindBy(".//div[contains(@class, 'arrowRight')]")
    AtlasWebElement arrowRight();

    default Slide getLastSlide() {
        return slides().stream().reduce((first, second) -> second).orElse(null);
    }

}
