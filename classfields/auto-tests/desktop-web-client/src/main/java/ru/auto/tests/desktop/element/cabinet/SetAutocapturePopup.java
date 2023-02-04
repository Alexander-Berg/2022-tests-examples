package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 09.04.18
 */
public interface SetAutocapturePopup extends VertisElement {

    @Name("Закрыть")
    @FindBy(".//div[contains(@class, 'Popup__closer')]")
    VertisElement close();

    @Name("Кнопка «{{ day }}»")
    @FindBy(".//button[contains(., '{{ day }}')]")
    VertisElement dayOfWeek(@Param("day") String day);

    @Name("Кнопка «Время активации»")
    @FindBy(".//div[contains(@class, 'ServicePopupSchedule__selector')]/button")
    VertisElement timeOfActivation();

    @Name("Кнопка «{{ name }}»")
    @FindBy(".//button[contains(., '{{ name }}')]")
    VertisElement button(@Param("name") String name);
}
