package ru.auto.tests.desktop.element.card.gallery;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PanoramaSpotAddPopup extends VertisElement {

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//div[.= '{{ text }}']")
    VertisElement button(@Param("text") String text);
}