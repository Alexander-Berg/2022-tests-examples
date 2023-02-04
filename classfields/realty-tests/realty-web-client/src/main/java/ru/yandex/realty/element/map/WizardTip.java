package ru.yandex.realty.element.map;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.RealtyElement;

/**
 * Created by kantemirov on 23.05.18.
 */
public interface WizardTip extends AtlasWebElement {

    @Name("Кнопка закрыть попапа подсказки")
    @FindBy(".//i[contains(@class, 'Tip__closer')]")
    RealtyElement closeButton();
}
