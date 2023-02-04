package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 06.04.18
 */
public interface UpdatesPerDayPopup extends VertisElement {

    @Name("Обновления в день {{ name }}")
    @FindBy(".//div[contains(@class, 'MenuItem') and contains(., '{{ name }}')]")
    VertisElement select(@Param("name") String name);
}
