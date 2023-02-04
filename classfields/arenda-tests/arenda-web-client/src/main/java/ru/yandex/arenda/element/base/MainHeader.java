package ru.yandex.arenda.element.base;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.arenda.element.common.Link;

public interface MainHeader extends Link {

    @Name("Лого Yandex")
    @FindBy(".//a[contains(@aria-label,'Яндекс')]")
    AtlasWebElement yandexLogo();

    @Name("Лого Yandex")
    @FindBy(".//a[contains(@aria-label,'Аренда')]")
    AtlasWebElement arendaLogo();
}
