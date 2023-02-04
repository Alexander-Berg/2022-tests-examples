package ru.yandex.arenda.element.lk.tenantlk;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface ToAppPopup extends AtlasWebElement {

    @Name("Крестик закрытия")
    @FindBy(".//*[contains(@class,'Modal__closeBtn')]")
    AtlasWebElement closeCross();
}
