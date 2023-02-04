package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.header.SubHeader;

public interface WithSubHeader {

    @Name("Подшапка")
    @FindBy("//div[contains(@class, 'TopNavigation_background_white')] | " +
            "//div[contains(@class, 'nav-top_view_white')]")
    SubHeader subHeader();
}