package ru.yandex.realty.element.saleads.popup;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.PopupWithItem;

public interface PublishControlPopup extends PopupWithItem {

    @Name("Контент попапа")
    @FindBy(".//div[@class='Popup__content offer-publish-control__select-popup']")
    AtlasWebElement content();
}
