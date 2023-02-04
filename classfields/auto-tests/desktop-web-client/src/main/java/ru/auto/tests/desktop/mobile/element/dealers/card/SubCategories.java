package ru.auto.tests.desktop.mobile.element.dealers.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SubCategories extends VertisElement {

    @Name("Подкатегория «{{ text }}»")
    @FindBy(".//div[.= '{{ text }}']")
    VertisElement subCategory(@Param("text") String Text);
}