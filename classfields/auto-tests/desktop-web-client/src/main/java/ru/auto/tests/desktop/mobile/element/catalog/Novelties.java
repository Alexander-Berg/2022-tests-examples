package ru.auto.tests.desktop.mobile.element.catalog;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Novelties extends VertisElement {

    @Name("Лоадер")
    @FindBy(".//div[@class = 'Carousel__page-dots'] | " +
            ".//div[contains(@class, 'Carousel__dots')]")
    VertisElement loader();
}