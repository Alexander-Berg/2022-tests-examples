package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface Slider extends AtlasWebElement {

    @Name("Айтемы слайдера")
    @FindBy(".//div[@class = 'SwipeableSlider__item']")
    ElementsCollection<Link> sliderItems();

}
