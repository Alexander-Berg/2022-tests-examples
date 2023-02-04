package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Link;

public interface RailwaysPage extends BasePage, Link {

    @Name("Блок «{{ value }}»")
    @FindBy("//div[h2[contains(@class, 'Links') and contains(.,'{{ value }}')]]")
    Link block(@Param("value") String value);
}
