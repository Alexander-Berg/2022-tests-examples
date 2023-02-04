package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Link extends VertisElement {

    String HREF = "href";

    @Name("Ссылка «{{ value }}»")
    @FindBy(".//a[contains(.,'{{ value }}')]")
    VertisElement link(@Param("value") String value);

    @Name("Ссылка элемента")
    @FindBy(".//a")
    VertisElement link();

    @Name("Ссылка «{{ value }}»")
    @FindBy(".//span[contains(.,'{{ value }}')]")
    VertisElement spanLink(@Param("value") String value);
    
}
