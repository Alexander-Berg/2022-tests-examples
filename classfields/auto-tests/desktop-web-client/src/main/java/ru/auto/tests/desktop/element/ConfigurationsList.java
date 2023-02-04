package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface ConfigurationsList  extends VertisElement, WithButton {

    @Name("Кнопка конфигурации с предложениями")
    @FindBy(".//a[contains(@class, 'ListingAllConfigurationsItem__offersLink')] | " +
            ".//a[contains(@class, 'ListingAllConfigurationsMobileItem__link')]")
    VertisElement configurationOffersButton();

    @Name("Кнопка блока конфигураций")
    @FindBy(".//span[contains(@class, 'ListingAllConfigurationsGroup__link')] | " +
            ".//a[contains(@class, 'ListingAllConfigurationsGroup__link')] | " +
            ".//span[contains(@class, 'ListingAllConfigurationsMobileGroup__link')] | " +
            ".//a[contains(@class, 'ListingAllConfigurationsMobileGroup__link')]")
    VertisElement configurationButton();
}
