package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface GalleryItem extends VertisElement {

    @Name("Изображение")
    @FindBy(".//img")
    VertisElement image();

    @Name("Промо панорам")
    @FindBy(".//div[contains(@class, 'addPanorama')]")
    VertisElement panoramaPromo();
}
