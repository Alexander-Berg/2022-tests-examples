package ru.yandex.arenda.element.lk.admin.callcenter;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.common.Input;

public interface TenantInfo extends Input {

    @Name("Селектор кандидата")
    @FindBy(".//select[@id='SHOWING_TYPE']")
    AtlasWebElement showingTypeSelector();

    @Name("Опция селектора value=«{{ value }}»")
    @FindBy(".//select[@id='SHOWING_TYPE']/option[.='{{ value }}']")
    AtlasWebElement showingTypeOption(@Param("value") String value);
}
