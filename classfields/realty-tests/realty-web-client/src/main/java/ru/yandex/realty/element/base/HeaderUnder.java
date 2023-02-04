package ru.yandex.realty.element.base;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.ButtonWithTitle;
import ru.yandex.realty.element.Link;

public interface HeaderUnder extends Button, Link, ButtonWithTitle {

    @Name("Раскрытое меню хедера")
    @FindBy("//div[@class='HeaderExpandedMenu']")
    ExpandedMenu expandedMenu();

    @Name("Заголовок подхедера «{{ value }}»")
    @FindBy(".//a[@data-test='{{ value }}']")
    AtlasWebElement mainMenuItem(@Param("value") String value);
}
