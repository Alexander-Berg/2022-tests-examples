package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SaleListItemGallery extends VertisElement {

    @Name("Кнопка «Позвонить»")
    @FindBy(".//div[contains(@class, 'OfferGallery__item_type_phone')] | " +
            ".//div[contains(@class, 'ListingItemGalleryPhone')]")
    VertisElement callButton();

    @Name("Контакты")
    @FindBy(".//div[contains(@class, 'OfferGallery__item_type_seller')]")
    VertisElement contacts();
}