package ru.auto.tests.desktop.mobile.element.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.element.FilterButton;

public interface MMMFilter extends VertisElement {

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//div[contains(@class, 'PseudoInput ') and .= '{{ text }}']")
    FilterButton button(@Param("text") String text);

    @Name("Кнопка раскрытия фильтра")
    @FindBy(".//div[contains(@class, 'PseudoInputGroup__expand')]")
    VertisElement expandButton();

}