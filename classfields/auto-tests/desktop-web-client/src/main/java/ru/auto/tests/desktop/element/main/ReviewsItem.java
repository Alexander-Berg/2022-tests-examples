package ru.auto.tests.desktop.element.main;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ReviewsItem extends VertisElement {

    @Name("Фото")
    @FindBy(".//img")
    VertisElement photo();

}
