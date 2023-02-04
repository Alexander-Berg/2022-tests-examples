package ru.auto.tests.desktop.mobile.element.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ListingHeader extends VertisElement {

    @Name("Марка или модель «{{ text }}»")
    @FindBy(".//a[contains(@class, 'TextList__item-link') and contains(., '{{ text }}')]")
    VertisElement markOrModel(@Param("text") String Text);
}