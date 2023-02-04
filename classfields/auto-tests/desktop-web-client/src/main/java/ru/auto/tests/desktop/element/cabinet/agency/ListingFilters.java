package ru.auto.tests.desktop.element.cabinet.agency;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 17.09.18
 */
public interface ListingFilters extends VertisElement {

    String ALL = "Все";
    String ACTIVE = "Активные";
    String FREEZED = "Замороженные";
    String STOPPED = "Остановленные";

    @Name("Фильтр «{{ name }}»")
    @FindBy(".//div[contains(@class, 'listing-filter-new__item')][contains(., '{{ name }}')] | " +
            ".//div[contains(@class, 'ClientsFiltersItem') and contains(., '{{ name }}')]")
    VertisElement filter(@Param("name") String name);

}
