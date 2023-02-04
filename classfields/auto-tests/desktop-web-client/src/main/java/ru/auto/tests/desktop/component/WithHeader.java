package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.header.Header;

public interface WithHeader {

    @Name("Шапка")
    @FindBy("//header | " +
            "//div[@class = 'FormHeader']")
    Header header();
}