package ru.auto.tests.desktop.element.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface CardVas extends VertisElement {

    @Name("Вкладка «{{ text }}»")
    @FindBy(".//div[@class = 'CardVAS__tab__title' and contains(., '{{ text }}')] | " +
            ".//div[@class = 'card-vas__tabs-item-name' and contains(., '{{ text }}')]")
    VertisElement tab(@Param("text") String text);

    @Name("Активная вкладка")
    @FindBy(".//div[contains(@class, 'CardVAS__tab_active')]/div[contains(@class, 'CardVAS__tab__title')] |" +
            ".//div[contains(@class, 'card-vas__tabs-item_active')]/div[contains(@class, 'card-vas__tabs-item-name')]")
    VertisElement activeTab();

    @Name("Кнопка «Подключить»")
    @FindBy(".//div[contains(@class, 'CardVAS__bottom')]//button | " +
            ".//div[contains(@class, 'card-vas__item_active')]//a[contains(@class, 'card-vas__activate')]")
    VertisElement buyButton();

}
