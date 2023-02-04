package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.header.SubHeader;

public interface WithSubHeader {

    @Name("Подшапка")
    @FindBy("//div[@class = 'AppTabs'] | " +
            "//div[@class = 'index__nav tabs'] | " +
            "//div[@class = 'Header2Tabs__tabs'] | " +
            "//div[@class = 'CatalogApplicationTabs']")
    SubHeader subHeader();
}