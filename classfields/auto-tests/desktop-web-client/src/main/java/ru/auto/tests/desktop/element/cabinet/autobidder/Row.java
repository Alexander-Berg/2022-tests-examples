package ru.auto.tests.desktop.element.cabinet.autobidder;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Row extends VertisElement {

    String ACTIVE_CAMPAIGN = "Активная кампания";
    String SUSPENDED = "Приостановлена";

    @Name("Название")
    @FindBy(".//td[1]//div[contains(@class, '_title')]")
    VertisElement title();

    @Name("Описание кампании")
    @FindBy(".//td[1]//div[contains(@class, '_subtitle')]")
    VertisElement description();

    @Name("Срок")
    @FindBy(".//td[2]")
    VertisElement period();

    @Name("Статус кампании")
    @FindBy(".//td[3]//div[contains(@class, '_title')]")
    VertisElement status();

    @Name("Кружок «{{ index }}»")
    @FindBy(".//td[3]//div[contains(@class, '_thermometerCounter')][{{ index }}]")
    VertisElement circle(@Param("index") int index);

    @Name("Кнопка «Play»")
    @FindBy(".//td[4]//*[contains(@class, '_auction-autostrategy')]")
    VertisElement play();

    @Name("Кнопка «Pause»")
    @FindBy(".//td[4]//*[contains(@class, '_pause')]")
    VertisElement pause();

    @Name("Кнопка удаления")
    @FindBy(".//td[4]//*[contains(@class, '_trash')]")
    VertisElement delete();

}
