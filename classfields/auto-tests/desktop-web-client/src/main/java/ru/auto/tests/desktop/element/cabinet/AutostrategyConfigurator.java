package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 27.03.18
 */
public interface AutostrategyConfigurator extends VertisElement {

    @Name("Кнопка открытия календаря")
    @FindBy(".//button[contains(@class, 'DateRange__button')]")
    VertisElement calendarButton();

    @Name("Обновления в день")
    @FindBy(".//div[./input[@name = 'limit']]/button")
    VertisElement updatesPerDay();

    @Name("Поисковые выдачи для автостратегии '{{ name }}'")
    @FindBy("//label[contains(., '{{ name }}')]")
    VertisElement searchingForAutostrategy(@Param("name") String name);

    @Name("Кнопка «{{ value }}»")
    @FindBy(".//button[contains(., '{{ value }}')]")
    VertisElement button(@Param("value") String value);
}
