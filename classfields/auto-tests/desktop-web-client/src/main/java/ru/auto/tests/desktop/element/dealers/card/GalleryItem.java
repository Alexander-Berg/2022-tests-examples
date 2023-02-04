package ru.auto.tests.desktop.element.dealers.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface GalleryItem extends VertisElement {

    @Name("Изображение")
    @FindBy(".//img")
    VertisElement img();
}