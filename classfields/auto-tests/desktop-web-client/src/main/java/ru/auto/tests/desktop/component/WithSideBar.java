package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WithSideBar {

    @Name("Сайдбар")
    @FindBy("//div[starts-with(@class,'sidebar') or @class='IndexSidebar'] | " +
            "//div[@class='LayoutSidebar__sidebar']")
    VertisElement sidebar();
}