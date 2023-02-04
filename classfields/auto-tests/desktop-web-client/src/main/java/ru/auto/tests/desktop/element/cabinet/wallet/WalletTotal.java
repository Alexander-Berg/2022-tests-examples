package ru.auto.tests.desktop.element.cabinet.wallet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WalletTotal extends VertisElement {

    @Name("Кнопка сворачивания/разворачивания блока")
    @FindBy(".//*[contains(@class, 'WalletTotal__toggleIcon')]")
    VertisElement toggle();

    @Name("Пункт «{{ text }}»")
    @FindBy(".//div[contains(@class, 'WalletTag__title') and .= '{{ text }}']")
    VertisElement item(@Param("text") String text);
}