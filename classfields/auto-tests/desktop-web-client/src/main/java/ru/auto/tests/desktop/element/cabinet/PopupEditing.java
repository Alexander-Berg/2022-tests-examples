package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.03.18
 */
public interface PopupEditing extends VertisElement {

    @Name("Кнопка «{{ name }}»")
    @FindBy(".//div[contains(@class, 'SaleMenu__item') and contains(., '{{ name }}')] | " +
            ".//div[contains(@class, 'SaleMenuGroup__item') and contains(., '{{ name }}')]")
    VertisElement button(@Param("name") String name);
}
