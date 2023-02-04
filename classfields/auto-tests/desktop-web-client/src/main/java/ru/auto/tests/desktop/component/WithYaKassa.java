package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.YaKassa;

public interface WithYaKassa extends WithCheckbox {

    @Name("Фрейм Я.кассы")
    @FindBy("//iframe[contains(@class, 'Billing__cardFrame')]")
    VertisElement yaKassaFrame();

    @Name("Фрейм Я.кассы")
    @FindBy("//html")
    YaKassa yaKassa();

}
