package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

public interface PageBanner extends AtlasWebElement, Link, Button, CloseCross {

    String THANKS_NEXT_TIME = "Спасибо, в другой раз";

    @Name("Закрыть")
    @FindBy(".//*[contains(@class, 'SplashBanner__button_close')]")
    AtlasWebElement close();
}
