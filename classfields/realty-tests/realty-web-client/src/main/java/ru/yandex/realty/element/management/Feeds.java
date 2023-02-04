package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface Feeds extends AtlasWebElement {

    @Name("Ссылка на скачивание фида «{{ value }}»")
    @FindBy(".//span[contains(@class,'feed-preview__download-links')]/a[contains(.,'{{ value }}')]")
    AtlasWebElement downloadLink(@Param("value") String value);
}
