package ru.auto.tests.desktop.element.group;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithShare;

public interface GroupOffers extends VertisElement, WithShare {

    @Name("Название")
    @FindBy(".//div[contains(@class, 'CardGroupOffersHeader__title')]")
    VertisElement title();

    @Name("Фильтры")
    @FindBy(".//div[contains(@class, 'CardGroupOffersHeaderFilters')]")
    GroupOffersFilters filters();
}
