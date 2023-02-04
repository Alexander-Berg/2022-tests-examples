package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.desktop.element.listing.InputGroup;

public interface WithInputGroup {

    @Name("Группа инпутов «{{ value }}»")
    @FindBy(".//span[contains(@class, 'ControlGroup')][contains(., '{{ value }}')]")
    InputGroup inputGroup(@Param("value") String value);
}