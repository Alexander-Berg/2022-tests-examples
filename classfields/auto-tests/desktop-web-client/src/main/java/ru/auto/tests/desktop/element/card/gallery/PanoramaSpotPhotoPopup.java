package ru.auto.tests.desktop.element.card.gallery;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface PanoramaSpotPhotoPopup extends VertisElement, WithButton {

    @Name("Фото")
    @FindBy(".//img")
    VertisElement photo();
}