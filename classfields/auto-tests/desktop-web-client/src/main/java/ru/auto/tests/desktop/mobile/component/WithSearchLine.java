package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.SearchLine;

public interface WithSearchLine {

    @Name("Строка поиска")
    @FindBy("//div[contains(@class, 'SearchLine')]")
    SearchLine searchLine();
}