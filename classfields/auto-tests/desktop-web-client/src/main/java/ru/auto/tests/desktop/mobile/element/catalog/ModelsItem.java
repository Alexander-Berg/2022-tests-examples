package ru.auto.tests.desktop.mobile.element.catalog;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface ModelsItem extends VertisElement, WithButton {

    @Name("Ссылка на объявления")
    @FindBy(".//a[contains(@class, 'catalog-item__details-url')] | " +
            ".//a[contains(@class, 'extraInfo__link')]")
    VertisElement salesUrl();
}
