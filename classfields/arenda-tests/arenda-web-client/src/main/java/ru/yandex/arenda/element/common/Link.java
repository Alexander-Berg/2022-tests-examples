package ru.yandex.arenda.element.common;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface Link extends AtlasWebElement {

    @Name("Ссылка «{{ value }}»")
    @FindBy(".//a[contains(.,'{{ value }}')]")
    AtlasWebElement link(@Param("value") String value);

    @Name("Ссылка с именем класса «{{ value }}»")
    @FindBy(".//a[contains(@class,'{{ value }}')]")
    AtlasWebElement classLink(@Param("value") String value);

    @Name("Ссылка элемента")
    @FindBy(".//a")
    AtlasWebElement link();

    @Name("Ссылка «{{ value }}»")
    @FindBy(".//span[contains(.,'{{ value }}')]")
    ArendaElement spanLink(@Param("value") String value);
}
