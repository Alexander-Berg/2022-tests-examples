package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.mobile.element.Link;

public interface DevInfo extends AtlasWebElement, Link {

    String FINISHED = "сдано";
    String SUSPENDED = "заморожено";
    String UNFINISHED = "строятся";
    String MORE_ABOUT_DEVELOPER = "Подробнее о\u00A0застройщике";

    @Name("Название")
    @FindBy(".//div[contains(@class, 'name')]")
    AtlasWebElement name();

    @Name("Лого")
    @FindBy(".//div[contains(@class, 'logo')]/img")
    AtlasWebElement logo();

    @Name("Строит дома с")
    @FindBy(".//div[contains(@class, 'born')]")
    AtlasWebElement born();

    @Name("Количество домов раздела «{{ value }}»")
    @FindBy(".//div[contains(@class, 'object')][.//span[contains(.,'{{ value }}')]]//span[contains(@class, 'Amount')]")
    AtlasWebElement amount(@Param("value") String value);

}
