package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithCrossLinksBlock;
import ru.auto.tests.desktop.mobile.element.catalog.Novelties;
import ru.auto.tests.desktop.mobile.element.catalog.Presets;

public interface CatalogMainPage extends CatalogPage, WithCrossLinksBlock {

    @Name("Новинки")
    @FindBy("//div[@class = 'Carousel'] | " +
            "//div[contains(@class, 'Carousel MainCarousel')]")
    Novelties novelties();

    @Name("Пресеты")
    @FindBy("//div[contains(@class, 'catalog__presets')] | " +
            "//div[@class = 'Presets']")
    Presets presets();

    @Name("Рекламный блок с2")
    @FindBy("//div[contains(@class, 'catalog__features')]/div/ancestor::div")
    VertisElement c2advert();
}