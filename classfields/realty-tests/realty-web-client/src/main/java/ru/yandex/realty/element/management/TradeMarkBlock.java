package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.ButtonWithTitle;

public interface TradeMarkBlock extends ButtonWithTitle {

    @Name("Инпут файла")
    @FindBy(".//input[@class='attach__control']")
    AtlasWebElement inputFile();

    @Name("Удалить товарный знак")
    @FindBy(".//*[contains(@class,'Tag__clear')]")
    AtlasWebElement clearSign();

}
