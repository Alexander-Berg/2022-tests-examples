package ru.auto.tests.desktop.mobile.element.header;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SubHeader extends VertisElement {

    @Name("Ссылка «{{ text }}» в подшапке")
    @FindBy(".//a[.= '{{ text }}']")
    VertisElement url(@Param("text") String Text);
}
