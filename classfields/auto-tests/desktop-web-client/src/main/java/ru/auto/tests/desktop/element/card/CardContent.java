package ru.auto.tests.desktop.element.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface CardContent extends VertisElement {

    @Name("Кнопка «Характеристики модели в каталоге»")
    @FindBy(".//a[contains(@class, 'CardCatalogLink ')]")
    VertisElement catalogTechDescription();
}
