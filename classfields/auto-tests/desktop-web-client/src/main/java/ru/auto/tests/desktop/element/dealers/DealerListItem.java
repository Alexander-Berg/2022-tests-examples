package ru.auto.tests.desktop.element.dealers;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface DealerListItem extends VertisElement {

    @Name("Название")
    @FindBy(".//a[contains(@class,'DealerListItem__name')]")
    VertisElement name();

    @Name("Ссылка на сеть")
    @FindBy(".//a[contains(@class, 'ListItem__net-name')]")
    VertisElement netUrl();

    @Name("Ссылка на объявления")
    @FindBy(".//a[contains(@class,'DealerListItem__search_results')]")
    VertisElement salesUrl();

    @Name("Кнопка «Показать телефон»")
    @FindBy(".//div[contains(@class, 'DealerListItem__phoneButton')]/button")
    VertisElement showPhones();

    @Name("Иконка «Проверенный дилер»")
    @FindBy(".//div[contains(@class, 'SalonVerifiedLabelWithPopup__popup')] | " +
            ".//div[@class = 'SalonVerifiedLabel-module__container']")
    VertisElement loyaltyIcon();

}
