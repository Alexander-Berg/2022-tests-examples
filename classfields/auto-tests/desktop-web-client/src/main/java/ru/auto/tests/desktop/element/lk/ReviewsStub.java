package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ReviewsStub extends VertisElement {

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//a[.='{{ text }}']")
    VertisElement button(@Param("text") String text);
}