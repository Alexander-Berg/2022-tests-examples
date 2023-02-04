package ru.auto.tests.desktop.mobile.element.dealers.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface MapPopup extends VertisElement {

    @Name("Ссылка «Авто в продаже»")
    @FindBy(".//a[contains(@class,'dealer-list-item__search_results')]")
    VertisElement salesUrl();

    @Name("Кнопка «Показать телефон»")
    @FindBy(".//button[contains(@class, 'dealer-list-item__phones-paranja')]")
    VertisElement showPhoneButton();
}
