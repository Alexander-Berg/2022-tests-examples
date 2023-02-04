package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.element.popups.OfferPreview;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface OfferInfo extends Button {

    @Name("Информация о типе и адресе")
    @FindBy(".//div[contains(@class,'owner-offer-preview-info-panel__info-wrapper')]")
    AtlasWebElement baseInfo();

    @Name("Цена")
    @FindBy(".//div[contains(@class, 'owner-offer-price')]")
    OfferPrice price();

    @Name("Ссылка на оффер")
    @FindBy(".//div[contains(@class, 'owner-offer-preview-info-panel')]//a")
    AtlasWebElement offerLink();

    @Name("Редактировать фото")
    @FindBy(".//button[contains(@class, 'owner-offer-preview-info-panel-photo')]")
    OfferPreview offerPreview();

    @Name("Редактировать фото")
    @FindBy(".//button[contains(@class, 'owner-offer-preview-info-panel-photo')]")
    OfferPreview addPhotoButton();

    @Name("Раскрыть статистику")
    @FindBy(".//button[@class='owner-offer-preview-stats-opener']")
    AtlasWebElement statsOpener();
}
