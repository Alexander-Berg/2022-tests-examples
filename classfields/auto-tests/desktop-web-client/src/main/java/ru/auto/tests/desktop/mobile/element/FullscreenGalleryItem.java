package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FullscreenGalleryItem extends VertisElement {

    @Name("Изображение")
    @FindBy(".//img")
    VertisElement image();
}