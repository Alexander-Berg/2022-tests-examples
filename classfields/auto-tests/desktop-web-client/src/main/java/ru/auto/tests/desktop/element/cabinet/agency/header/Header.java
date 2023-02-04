package ru.auto.tests.desktop.element.cabinet.agency.header;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 19.09.18
 */
public interface Header extends ru.auto.tests.desktop.element.cabinet.header.Header {

    @Name("Страница кабинета «{{ name }}»")
    @FindBy(".//div[contains(@class, 'HeaderLinks__container')]/a[contains(., '{{ name }}')]")
    VertisElement cabinetPage(@Param("name") String name);

    @Name("Найти клиента")
    @FindBy(".//div[contains(@class, 'Header__search')]")
    VertisElement findClients();

    @Name("Поп-ап поиска клиентов")
    @FindBy(".//div[contains(@class, 'HeaderSearchClient__container')]")
    SearchClientPopup searchClientPopup();
}
