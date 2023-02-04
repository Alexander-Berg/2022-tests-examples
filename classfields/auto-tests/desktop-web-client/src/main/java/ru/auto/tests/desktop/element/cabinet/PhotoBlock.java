package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PhotoBlock extends VertisElement {

    @Name("Чекбокс групповых действий")
    @FindBy(".//label[contains(@class, 'OfferSnippetRoyalPhoto__checkbox')]")
    VertisElement select();

    @Name("Мини галерея")
    @FindBy(".//div[contains(@class, 'Brazzers')]//div[contains(@class, 'Brazzers__page')]")
    ElementsCollection<VertisElement> miniGalleryItems();
}
