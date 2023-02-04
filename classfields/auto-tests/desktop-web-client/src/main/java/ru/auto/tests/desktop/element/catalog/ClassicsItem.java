package ru.auto.tests.desktop.element.catalog;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ClassicsItem extends VertisElement {

    @Name("Ссылка с названия модели")
    @FindBy(".//a[contains(@class, 'carousel__title')]")
    VertisElement titleUrl();

    @Name("Ссылка с фото")
    @FindBy(".//div[contains(@class, 'tile_gallery-loaded')]")
    VertisElement imageUrl();
}
