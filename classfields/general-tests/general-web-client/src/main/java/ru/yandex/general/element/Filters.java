package ru.yandex.general.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Filters extends VertisElement, Link, Checkbox {

    String SAVE_SEARCH = "Сохранить поиск";
    String ACTIVE = "_active_";
    String FIND_IN_MY_REGION = "Искать в моем регионе";

    @Name("Сортировка")
    @FindBy(".//span[contains(@class, 'ListingSort__button')]")
    VertisElement sortButton();

    @Name("Каунтер примененных фильтров")
    @FindBy(".//span[contains(@class, 'AllFiltersButton__count')]")
    VertisElement counter();

    @Name("Выдача списком")
    @FindBy(".//span[contains(@class, 'OfferListingChooseDisplayType__viewButton')][1]")
    VertisElement listListingDisplayType();

    @Name("Выдача плиткой")
    @FindBy(".//span[contains(@class, 'OfferListingChooseDisplayType__viewButton')][2]")
    VertisElement gridListingDisplayType();

    @Name("Чипсина фильтра «{{ value }}»")
    @FindBy(".//div[contains(@class, '_filterChip_')][contains(., '{{ value }}')]")
    FilterChip chips(@Param("value") String value);

    @Name("Список чипсин фильтра")
    @FindBy(".//div[contains(@class, '_filterChip_')][not(contains(., 'Сбросить все'))]")
    ElementsCollection<FilterChip> chipsList();

}
