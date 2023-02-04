package ru.yandex.general.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.mobile.element.AttributeItem;
import ru.yandex.general.element.ShemaOrgMark;
import ru.yandex.general.element.OfferCardMessage;
import ru.yandex.general.mobile.element.FullscreenGallery;
import ru.yandex.general.mobile.element.ListingSnippet;
import ru.yandex.general.mobile.element.Button;
import ru.yandex.general.mobile.element.Image;
import ru.yandex.general.mobile.element.Link;
import ru.yandex.general.mobile.element.SellerInfo;
import ru.yandex.general.mobile.element.SimilarCarouselItem;
import ru.yandex.general.mobile.element.Statistics;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface OfferCardPage extends BasePage, Button, Link {

    String ALL_ATTRIBUTES = "Все характеристики";
    String ADDRESS = "Адрес";
    String SHOW_MORE = "Показать ещё";
    String CALL = "Позвонить";
    String WRITE = "Написать";
    String YANDEX_ACTIVITY = "Активность на Яндексе";
    String COMPLAIN = "Пожаловаться";
    String EDIT = "Редактировать";
    String ACTIVATE = "Активировать";
    String SHOW_MAP = "Посмотреть карту";
    String RAISE_UP = "Поднять за";
    String FULL_DESCRIPTION = "Полное описание";
    String CONDITION = "Состояние";
    String DELIVERY_RUSSIA = "Отправлю по России";
    String DELIVERY_TAXI = "Отправлю такси или курьером";
    String DELETE = "Удалить";

    @Name("Секция «{{ value }}»")
    @FindBy("//div[contains(@class, 'OfferCardSection')]//span[contains(., '{{ value }}')]")
    VertisElement section(@Param("value") String value);

    @Name("Цена для владельца")
    @FindBy("//div[contains(@class, 'OfferCardOwner__topInfo')]//span[contains(@class, 'h1')]")
    Link priceOwner();

    @Name("Цена для покупателя")
    @FindBy("//div[contains(@class, 'OfferCardTopInfo')]//span[contains(@class, 'h1')]")
    Link priceBuyer();

    @Name("Кнопка добавить в избранное")
    @FindBy(".//div[contains(@class,'OfferCardTopControls__favoritesButton')]")
    VertisElement addToFavorite();

    @Name("Действия с оффером")
    @FindBy(".//button[contains(@class, 'CardOwnerTopControls__btn')]")
    VertisElement cardAction();

    @Name("Блок статистики")
    @FindBy(".//div[contains(@class, 'Statistics__container')]")
    Statistics statistics();

    @Name("Список атрибутов")
    @FindBy("//li[contains(@class, 'Attributes__item')]")
    ElementsCollection<AttributeItem> attributes();

    @Name("Первый атрибут")
    @FindBy("//li[contains(@class, 'Attributes__item')][1]")
    AttributeItem firstAttribute();

    @Name("Описание")
    @FindBy("//div[contains(@class, 'OfferCardDescription__htmlContainer')]")
    VertisElement description();

    @Name("Сервис шаринга «{{ value }}»")
    @FindBy("//li[contains(@class, 'ya-share2__item_service_{{ value }}')]")
    Link shareService(@Param("value") String value);

    @Name("Сервис шаринга в блоке продавца")
    @FindBy("//div[contains(@class, 'CardShareBlock__panel')]" +
            "//li[contains(@class, 'ya-share2__item_service_{{ value }}')]")
    Link sellerShareService(@Param("value") String value);

    @Name("Снипеты похожих офферов")
    @FindBy("//div[contains(@class, 'OfferCardSimilar')]//div[@role = 'gridItem']")
    ElementsCollection<ListingSnippet> similarSnippets();

    @Name("Сниппеты похожих в блоке сверху")
    @FindBy("//div[contains(@class, 'OfferCardSimilarCarouselItem__wrapper')]")
    ElementsCollection<SimilarCarouselItem> similarCarouseItems();

    @Name("Первый сниппет")
    @FindBy("//div[contains(@class, 'OfferCardSimilar')]//div[@role = 'gridItem'][contains(@style, 'top: 0px; left: 0px;')]")
    ListingSnippet firstSnippet();

    @Name("Баннер на карточке")
    @FindBy("//div[contains(@class, 'OfferCardAd__container')]")
    VertisElement adBanner();

    @Name("Блок инфы о продавце")
    @FindBy("//div[./a/div[contains(@class, 'SellerLink')]]")
    SellerInfo sellerInfo();

    @Name("Блок хлебных крошек")
    @FindBy("//div[contains(@class, 'Breadcrumbs__container')]")
    VertisElement breadcrumbs();

    @Name("Хлебные крошки")
    @FindBy("//div[contains(@class, 'Breadcrumbs__item')]/a")
    ElementsCollection<VertisElement> breadcrumbsList();

    @Name("Сообщение об успешной публикации")
    @FindBy("//div[contains(@class, 'OfferCardOwner__message')][contains(., 'Готово')]")
    VertisElement successPublishMessage();

    @Name("Кнопка шаринга")
    @FindBy("//div[contains(@class, 'shareButton')]")
    VertisElement shareButton();

    @Name("Видео-контейнер")
    @FindBy("//div[contains(@class, 'videoContainer')]")
    VertisElement video();

    @Name("Список превью фото")
    @FindBy("//div[contains(@class, 'CardMedia__photo')]")
    ElementsCollection<Image> photoPreviewList();

    @Name("Фуллскрин галерея")
    @FindBy("//div[contains(@class, 'pswp--open')]")
    FullscreenGallery fullscreenGallery();

    @Name("Адрес")
    @FindBy("//div[contains(@class, 'CardMaps__textItem')]")
    VertisElement address();

    @Name("Сообщение пользователю с типом «{{ value }}»")
    @FindBy("//div[contains(@class, 'Message__container')][contains(@class, '{{ value }}')]")
    OfferCardMessage message(@Param("value") String value);

    @Name("Сообщение пользователю")
    @FindBy("//div[contains(@class, 'Message__container')]")
    OfferCardMessage message();

    @Name("Бейдж активного оффера")
    @FindBy("//span[contains(@class, '_activeBadge')]")
    VertisElement activeBadge();

    @Name("Разметка ShemaOrg «JobPosting»")
    @FindBy("//div[@itemprop = 'JobPosting']")
    ShemaOrgMark jobPostingShemaOrg();

    @Name("Разметка ShemaOrg «Product»")
    @FindBy("//div[@itemtype = 'http://schema.org/Product']")
    ShemaOrgMark productShemaOrg();

    @Name("Бейдж доставки")
    @FindBy("//div[contains(@class, '_deliveryBadge')]")
    VertisElement deliveryBadge();

    @Name("Бейдж доставки «{{ value }}»")
    @FindBy("//div[contains(@class, '_deliveryBadge')]//span[contains(., '{{ value }}')]")
    VertisElement deliveryBadge(@Param("value") String value);

    @Name("Блок удаленной карточки")
    @FindBy("//div[contains(@class, 'OfferCardNotAvailable')]")
    VertisElement cardNotAvaliable();

    default ListingSnippet similarSnippetFirst() {
        similarSnippets().waitUntil(hasSize(greaterThan(0)));
        return firstSnippet();
    }

}
