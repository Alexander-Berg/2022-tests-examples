package ru.auto.tests.desktop.element.cabinet.placement_report;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PlatformTable extends VertisElement {

    @Name("Заголовок столбца «{{ text }}»")
    @FindBy(".//th[contains(@class, 'HoveredTooltip__trigger') and .='{{ text }}']")
    VertisElement tableHeader(@Param("text") String text);
}
