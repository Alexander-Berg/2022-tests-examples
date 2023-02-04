package ru.yandex.general.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.AttributeItem;
import ru.yandex.general.element.CardNotice;
import ru.yandex.general.element.FullscreenGallery;
import ru.yandex.general.element.Link;
import ru.yandex.general.element.ListingSnippet;
import ru.yandex.general.element.OfferCardMessage;
import ru.yandex.general.element.ShemaOrgMark;
import ru.yandex.general.element.Sidebar;
import ru.yandex.general.mobile.element.SimilarCarouselItem;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface OfferCardPage extends BasePage {

    String DEACTIVATE = "Снять с публикации";
    String YANDEX_ACTIVITY = "Активность на Яндексе";
    String COMPLAIN = "Пожаловаться";
    String SUPPORT = "Поддержка";
    String CHAT_WITH_SUPPORT = "Написать в поддержку";
    String BUILD_A_ROUTE = "Построить маршрут";
    String EDIT = "Редактировать";
    String SHOW_MAP = "Посмотреть карту";
    String NEXT = "Далее";
    String SEND = "Отправить";
    String CONDITION = "Состояние";
    String DELETE = "Удалить";
    String ACTIVATE = "Активировать";
    String SHOW_FULL = "Показать полностью";

    String MESSAGE_DEFAULT = "default";
    String MESSAGE_ERROR = "error";
    String MESSAGE_EXPIRED = "expired";

    @Name("Описание")
    @FindBy("//div[contains(@class, 'OfferCardDescription__htmlContainer')]")
    VertisElement description();

    @Name("Название")
    @FindBy("//h1[contains(@class, 'OfferCardSidbarTitle__title')]")
    VertisElement title();

    @Name("Имя продавца")
    @FindBy("//div[contains(@class, 'OfferCardSeller__info')]/span")
    VertisElement sellerName();

    @Name("Хлебные крошки")
    @FindBy("//div[contains(@class, 'OfferCardMainContent__breadcrumbs')]//a")
    ElementsCollection<VertisElement> breadcrumbsList();

    @Name("Ссылка на youtube ролик")
    @FindBy("//div[@class = 'ytp-title-text']/a")
    VertisElement youtubeLink();

    @Name("Главное фото")
    @FindBy("//div[contains(@class, 'Media__main')]//img")
    VertisElement mainPhoto();

    @Name("Главное видео")
    @FindBy("//div[contains(@class, 'mainVideoContainer')]")
    VertisElement mainVideo();

    @Name("Список превью фото оффера")
    @FindBy("//div[contains(@class, 'miniPhoto')]//img")
    ElementsCollection<VertisElement> previewsList();

    @Name("Кнопка в превью «Ещё n фото»")
    @FindBy("//div[contains(@class, 'Media__miniStub')]")
    VertisElement morePhoto();

    @Name("Кнопка добавить в избранное")
    @FindBy(".//div[contains(@class,'OfferCardFavoritesButton__wrapper')]")
    VertisElement addToFavorite();

    @Name("Кнопка «Написать»")
    @FindBy(".//div[contains(@class, 'ChatButton')]//button")
    VertisElement startChat();

    @Name("Список атрибутов")
    @FindBy("//li[contains(@class, 'Attributes__item')]")
    ElementsCollection<AttributeItem> attributes();

    @Name("Первый атрибут")
    @FindBy("//li[contains(@class, 'Attributes__item')][1]")
    AttributeItem firstAttribute();

    @Name("Блок шаринга")
    @FindBy(".//div[contains(@class, 'shareList')]")
    VertisElement shareBlock();

    @Name("Снипеты похожих офферов")
    @FindBy("//div[contains(@class, 'OfferCardSimilar')]//div[@role = 'gridItem']")
    ElementsCollection<ListingSnippet> similarSnippets();

    @Name("Первый сниппет")
    @FindBy("//div[contains(@class, 'OfferCardSimilar')]//div[@role = 'gridItem'][contains(@style, 'top: 0px; left: 0px;')]")
    ListingSnippet firstSnippet();

    @Name("Меню с доп. опциями")
    @FindBy("//button[contains(@class, 'OfferCardOwnerMoreBtn')]")
    VertisElement more();

    @Name("Блок заметки")
    @FindBy("//div[contains(@class, 'Sidebar__notice')]")
    CardNotice notice();

    @Name("Сервис шаринга «{{ value }}»")
    @FindBy("//li[contains(@class, 'ya-share2__item_service_{{ value }}')]")
    Link shareService(@Param("value") String value);

    @Name("Развернуть сервисы шаринга")
    @FindBy("//div[contains(@class, 'Share__shareButton')]")
    VertisElement shareButton();

    @Name("Сайдбар")
    @FindBy("//div[contains(@class, 'Sidebar__wrapper')]")
    Sidebar sidebar();

    @Name("Баннер на карточке")
    @FindBy("//vertisads-desktop-card")
    VertisElement adBanner();

    @Name("Превью видео")
    @FindBy("//div[contains(@class, '_videoPreview')]")
    VertisElement videoPreview();

    @Name("Видео-контейнер")
    @FindBy("//div[contains(@class, 'mainVideoContainer')]")
    VertisElement video();

    @Name("Фуллскрин галерея")
    @FindBy("//div[@data-prop-name = 'Полноэкранная галерея']")
    FullscreenGallery fullscreenGallery();

    @Name("Сообщение пользователю с типом «{{ value }}»")
    @FindBy("//div[contains(@class, 'Message__container')][contains(@class, '{{ value }}')]")
    OfferCardMessage message(@Param("value") String value);

    @Name("Сообщение пользователю")
    @FindBy("//div[contains(@class, 'Message__container')]")
    OfferCardMessage message();

    @Name("Свернутая карточка")
    @FindBy("//div[contains(@class, 'CardMainContent__minimized_')]")
    VertisElement minimizedCard();

    @Name("Разметка ShemaOrg «JobPosting»")
    @FindBy("//div[@itemprop = 'JobPosting']")
    ShemaOrgMark jobPostingShemaOrg();

    @Name("Разметка ShemaOrg «Product»")
    @FindBy("//div[@itemtype = 'http://schema.org/Product']")
    ShemaOrgMark productShemaOrg();

    @Name("Сниппеты похожих в блоке сверху")
    @FindBy("//div[contains(@class, 'OfferCardSimilarCarouselItem__wrapper')]")
    ElementsCollection<SimilarCarouselItem> similarCarouseItems();

    @Name("Дней до снятия с выдачи")
    @FindBy("//span[contains(@class, '_daysUntilExpire_')]")
    VertisElement daysUntilExpire();

    default ListingSnippet similarSnippetFirst() {
        similarSnippets().waitUntil(hasSize(greaterThan(0)));
        return firstSnippet();
    }

}
