package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithSelect;

public interface AutoProlongPopup extends VertisElement, WithSelect, WithButton {

    @Name("Кнопка «Включить автопродление»")
    @FindBy(".//div[contains(@class, 'VasAutoProlongStatus__popupAddButton')]/button |" +
            ".//label[contains(@class, 'Toggle')]")
    VertisElement turnOnButton();

    @Name("Кнопка «Выключить автопродление»")
    @FindBy(".//div[contains(@class, 'VasAutoProlongStatus__popupDeleteButton')] |" +
            ".//label[contains(@class, 'Toggle_checked')]")
    VertisElement turnOffButton();

    @Name("Текущее выбранное время")
    @FindBy(".//div[contains(@class, 'AutorenewModal__select')]")
    VertisElement currentTime();

    default void selectTime(String time) {
        currentTime().click();
        selectPopup().item(time).click();
    }

}