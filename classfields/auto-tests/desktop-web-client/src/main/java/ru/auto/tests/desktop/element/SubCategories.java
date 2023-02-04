package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SubCategories extends VertisElement {

    @Name("Подкатегория «{{ text }}»")
    @FindBy(".//a[contains(., '{{ text }}')]")
    VertisElement subCategory(@Param("text") String Text);
}