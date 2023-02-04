package ru.yandex.arenda.element.lk.admin.userassign;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface SuggestListItem extends AtlasWebElement {

    @Name("Ссылка на пользователя")
    @FindBy("./div/div")
    AtlasWebElement userName();
}
