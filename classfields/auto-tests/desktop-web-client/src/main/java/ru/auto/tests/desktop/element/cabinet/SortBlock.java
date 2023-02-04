package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SortBlock extends VertisElement {

    @Name("Сортировка по «{{ name }}»")
    @FindBy(".//div[contains(@class, 'Listing__column') and contains(., '{{ name }}')]" +
            "/div[contains(@class, 'listing-sort')]")
    VertisElement sortBy(@Param("name") String name);
}
