package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface StatusBlock extends VertisElement, WithButton {

    @Name("Количество дней")
    @FindBy(".//span[contains(@class, 'DaysLeft')]")
    VertisElement daysLeft();

}
