package ru.auto.tests.desktop.element.catalog;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PresetContent extends VertisElement {

    @Name("Кнопка «Показать еще»")
    @FindBy(".//div[contains(@class, 'catalog-presets__more')]")
    VertisElement showMore();

    @Name("Кнопка «Показать все»")
    @FindBy(".//a[contains(@class, 'catalog-presets__all')]")
    VertisElement showAll();

    @Name("Список элементов пресета")
    @FindBy(".//a//div[contains(@class, 'tile_gallery-loaded')]")
    ElementsCollection<VertisElement> presetItemsList();
}
