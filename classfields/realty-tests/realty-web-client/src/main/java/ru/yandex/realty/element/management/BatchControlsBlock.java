package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * Created by kopitsa on 24.07.17.
 */
public interface BatchControlsBlock extends AtlasWebElement {

    @Name("Чекбокс для выбора всего списка офферов")
    @FindBy(".//span[contains(@class, 'batch-checkbox')]//input")
    AtlasWebElement selectAllCheckbox();

    @Name("Тумблер выключения списка офферов")
    @FindBy(".//span[contains(@class, 'manage-table__control_type_batch-power')]//button")
    AtlasWebElement powerSwitch();

    @Name("Кнопка удаления списка офферов")
    @FindBy(".//button[contains(@class, 'manage-table__control_type_batch-remove')]")
    AtlasWebElement deleteButton();

    @Name("Кнопка обновления списка офферов")
    @FindBy(".//button[contains(@class, 'manage-table__control_type_batch-extend')]")
    AtlasWebElement refreshButton();
}
