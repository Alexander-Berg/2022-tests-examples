package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface AutoProlongInfo extends VertisElement {

    @Name("Количество дней")
    @FindBy(".//div[contains(@class, 'timeLeft')]")
    VertisElement timeLeft();

}
