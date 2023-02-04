package ru.auto.tests.desktop.element.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface InputGroup extends VertisElement {

    @Name("Инпут «{{ value }}»")
    @FindBy(".//input[contains(@class, 'TextInput__control') and preceding-sibling::div[contains(., '{{ value }}')]] |" +
            ".//input[contains(@class, 'TextInput__control') and ../../div[contains(., '{{ value }}')]]")
    VertisElement input(@Param("value") String value);
}