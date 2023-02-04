package ru.auto.tests.desktop.element.dealers;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;


public interface MapPopup extends VertisElement {

    @Name("Название")
    @FindBy(".//a[contains(@class,'DealerListItem__name')]")
    VertisElement name();

    @Name("Ссылка на сеть")
    @FindBy(".//a[contains(@class,'DealerListItem__net-name')]")
    VertisElement netUrl();

    @Name("Ссылка на объявления")
    @FindBy(".//a[contains(@class,'DealerListItem__search_results')]")
    VertisElement salesUrl();

    @Step("Кнопка «Показать телефон»")
    @FindBy(".//div[contains(@class, 'DealerListItem__phoneButton')]/button")
    VertisElement showPhones();

    @Step("Кнопка закрытия")
    @FindBy(".//div[contains(@class,'YandexMapBalloon__icon-close')]")
    VertisElement close();
}
