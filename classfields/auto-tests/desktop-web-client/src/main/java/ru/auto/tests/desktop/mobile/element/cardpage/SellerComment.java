package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface SellerComment extends VertisElement, WithButton {

    @Name("Кнопка «{{ text }}» для страниц AMP")
    @FindBy("//label[.= '{{ text }}']")
    VertisElement showAllButton(@Param("text") String value);

}