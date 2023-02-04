package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Paginator extends VertisElement {

    @Name("Стрелка назад")
    @FindBy(".//li[contains(@class, 'Pagination__previous')]")
    Link prev();

    @Name("Стрелка вперед")
    @FindBy(".//li[contains(@class, 'Pagination__next')]")
    Link next();

    @Name("Страница «{{ value }}»")
    @FindBy(".//li[contains(@class, 'Pagination__item')][{{ value }}]")
    Link pageNumber(@Param("value") int value);

    @Name("Активный пейдж")
    @FindBy(".//li[contains(@class, 'Pagination__active')]")
    Link activePage();

}
