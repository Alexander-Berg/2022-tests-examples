package ru.auto.tests.desktop.mobile.element.garage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface CarSelector extends VertisElement {

    String ADD_DREAM_CAR = "Добавить\nмашину мечты";
    String ADD_EX_CAR = "Добавить\nбывшую";

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//a[contains(@class, 'GarageCardSelectorAddItem') and contains(., '{{ text }}')]")
    VertisElement button(@Param("text") String text);
}
