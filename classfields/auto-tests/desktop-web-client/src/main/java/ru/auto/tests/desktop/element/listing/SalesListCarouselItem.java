package ru.auto.tests.desktop.element.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.Price;
import ru.auto.tests.desktop.element.card.header.ToolBar;

public interface SalesListCarouselItem extends VertisElement {

    @Name("Ссылка на дилера")
    @FindBy(".//a[contains(@class, 'SalonName')]")
    VertisElement dealerUrl();

    @Name("Стикер «Этот автомобиль продан»")
    @FindBy(".//div[contains(@class, 'ListingItemSoldInfo')]")
    VertisElement soldBadge();

    @Name("Цена на оффер")
    @FindBy(".//div[contains(@class,'__price') and not(contains(@class, 'old'))]")
    Price price();

    @Name("Бейдж «{{ text }}»")
    @FindBy(".//div[contains(@class, 'Badge ') and .= '{{ text }}']")
    VertisElement badge(@Param("text") String text);

    @Name("Бейджи")
    @FindBy(".//div[@class = 'ListingItemTagsDesktop']")
    VertisElement badges();

    @Name("Кнопка «Показать телефон»")
    @FindBy(".//button[.//span[.= 'Показать телефон']]")
    VertisElement showPhonesButton();

    @Name("Тулбар с действиями над объявлением")
    @FindBy(".//div[@class = 'ListingItemGalleryDesktop__actions']")
    ToolBar toolBar();

    @Name("Ссылка на оффер")
    @FindBy(".//a")
    VertisElement link();

    @Name("Развернуть оффер")
    @FindBy(".//span[contains(@class, '_unfold')]")
    VertisElement unfold();

    @Name("Ссылка с именем продавца")
    @FindBy(".//div[contains(@class, '_sellerName')]//a")
    VertisElement resellerLink();

    @Name("«Показать телефон» в галерее")
    @FindBy(".//div[@class = 'ListingItemGalleryPhone']")
    VertisElement galleryPhoneButton();

    @Name("«Смотреть отчёт» в галерее")
    @FindBy(".//a[@class = 'ListingItemGalleryVinReport']")
    VertisElement galleryVinReport();

    @Name("Продавец в галерее")
    @FindBy(".//div[@class = 'ListingItemGallerySeller']")
    VertisElement gallerySellerButton();

    @Name("«Ещё фото» в галерее")
    @FindBy(".//div[contains(@class, 'GalleryDesktop__more')]")
    VertisElement galleryMorePhotos();

}
