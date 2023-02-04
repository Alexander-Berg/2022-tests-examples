package ru.yandex.general.page;

import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.BanMessage;
import ru.yandex.general.element.Button;
import ru.yandex.general.element.FloatedHeader;
import ru.yandex.general.element.Footer;
import ru.yandex.general.element.Header;
import ru.yandex.general.element.Link;
import ru.yandex.general.element.LkSidebar;
import ru.yandex.general.element.Map;
import ru.yandex.general.element.Popup;
import ru.yandex.general.element.RecallModal;
import ru.yandex.general.element.SearchBar;
import ru.yandex.general.element.ShemaOrgMark;
import ru.yandex.general.element.SidebarCategories;
import ru.yandex.general.element.Tab;
import ru.yandex.general.element.UserInfoPopup;

public interface BasePage extends WebPage, Button, Link {

    int PXLS_TO_FLOAT_HEADER = 1;
    String URA_SPASIBO = "Ура, спасибо!";
    String VAS_SUCCESSFULL = "Поднятие успешно применено!";
    String JOB_POSTING = "JobPosting";
    String AGGREGATE_OFFER = "AggregateOffer";
    String PRODUCT = "Product";
    String BREADCRUMB_LIST = "BreadcrumbList";
    String ORGANIZATION = "Organization";
    String LOGIN_WITH_YANDEX_ID = "Войдите с Яндекс ID";
    String ERROR_404_TEXT = "Вы ошиблись адресом,\nили такой страницы не существует.";
    String MAIN_PAGE_MOSCOW_H1 = "Объявления в Москве";

    @Name("Вся страница")
    @FindBy("//body")
    VertisElement pageRoot();

    @Name("Цвет страницы «{{ value }}»")
    @FindBy("//html[@class = 'font_loaded {{ value }}']")
    VertisElement pageColor(@Param("value") String value);

    @Name("Основной контент страницы")
    @FindBy("//div[contains(@class, 'Page__main')]")
    VertisElement pageMain();

    @Name("Регион")
    @FindBy("//div[contains(@class, 'LocationSuggestTriggerButton__title_')]")
    VertisElement region();

    @Name("Модалка")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    Popup modal();

    @Name("Попап")
    @FindBy("//div[contains(@class, 'Popup2_visible')]")
    Popup popup();

    @Name("Прилипший при скролле хэдер")
    @FindBy("//header[contains(@class, 'Sticky')]")
    FloatedHeader floatedHeader();

    @Name("Хэдер")
    @FindBy("//header")
    Header header();

    @Name("Серч-бар")
    @FindBy("//div[contains(@class, '_searchSuggestWrapper')]")
    SearchBar searchBar();

    @Name("Футер")
    @FindBy("//footer")
    Footer footer();

    @Name("H1")
    @FindBy("//h1")
    VertisElement h1();

    @Name("Текст H1")
    @FindBy("//*[contains(@class, 'Text__h1')]")
    VertisElement textH1();

    @Name("Seo description")
    @FindBy("//head/meta[@name = 'description']")
    VertisElement seoDescription();

    @Name("Попап паспортного логина")
    @FindBy("//div[contains(@class, 'PassportRequiredModal')]")
    Popup passportRequiredModal();

    @Name("Юзер инфо попап")
    @FindBy("//div[contains(@class, 'HeaderUserInfoPopup')]")
    UserInfoPopup userInfoPopup();

    @Name("Сайдбар с категориями")
    @FindBy("//aside[contains(@class, 'Page__navigation')]")
    SidebarCategories sidebarCategories();

    @Name("Скелетоны")
    @FindBy("//div[contains(@class, 'Skeleton')]")
    VertisElement skeleton();

    @Name("Всплывающее сообщение «{{ value }}»")
    @FindBy(".//div[contains(@class, 'NotificationsItem__content')][contains(.,'{{ value }}')]")
    Button popupNotification(@Param("value") String value);

    @Name("Лого «Объявления»")
    @FindBy(".//a[contains(@class, 'Link HeaderLogo')]")
    VertisElement oLogo();

    @Name("Лого «Яндекс»")
    @FindBy(".//a[contains(@class, 'yaLink')]")
    VertisElement yLogo();

    @Name("Нотификация с текстом «{{ value }}»")
    @FindBy("//div[contains(@class, 'Notification__text')][contains(., '{{ value }}')]")
    VertisElement notificationWithText(@Param("value") String value);

    @Name("Попап снятия оффера")
    @FindBy("//div[contains(@class, 'Modal_visible')][.//div[contains(@class, 'RecallReasonModal')]]")
    RecallModal recallOfferPopup();

    @Name("Карточка оффера")
    @FindBy("//div[contains(@class, 'OfferCardMainContent')]")
    VertisElement offerCard();

    @Name("Элемент хлебных крошек «{{ value }}»")
    @FindBy("//div[contains(@class, 'Breadcrumbs__item')][contains(., '{{ value }}')]")
    VertisElement breadcrumbsItem(@Param("value") String value);

    @Name("Баннер в блоке навигации")
    @FindBy("//div[contains(@class, 'NavigationWithBanner')]")
    VertisElement adBannerNavigationBlock();

    @Name("Кнопка «Разместить»")
    @FindBy("//div[contains(@class, 'CreateOfferButton')]/a")
    VertisElement createOffer();

    @Name("Блок сообщения забаненному юзеру")
    @FindBy("//div[contains(@class, 'Message__error')]")
    BanMessage banMessage();

    @Name("Сайдбар")
    @FindBy("//aside[contains(@class, 'navigation')]")
    LkSidebar lkSidebar();

    @Name("Фрейм с видео")
    @FindBy("//iframe[contains(@id, 'video')]")
    VertisElement videoFrame();

    @Name("Карта")
    @FindBy("//ymaps")
    Map map();

    @Name("Таб «{{ value }}»")
    @FindBy("//label[contains(@class, 'Tab__root')][contains(., '{{ value }}')]")
    Tab tab(@Param("value") String value);

    @Name("Разметка LdJson «{{ value }}»")
    @FindBy("//script[@type = 'application/ld+json'][contains(., '{{ value }}')]")
    VertisElement ldJson(@Param("value") String value);

    @Name("Разметка LdJson «Logo»")
    @FindBy("//script[@type = 'application/ld+json'][contains(., 'Organization')][contains(., 'logo')]")
    VertisElement ldJsonLogo();

    @Name("Разметка ShemaOrg «AggregateOffer»")
    @FindBy("//div[contains(@class, 'OfferListingSeo')]")
    ShemaOrgMark listingSeoBlock();

    @Name("Ошибка 500/404")
    @FindBy("//img[@class = 'img500']")
    VertisElement errorImage();

    @Name("Попап чатов")
    @FindBy("//div[contains(@class, 'ya-chat-popup')]")
    VertisElement chatPopup();

    @Name("Кнопка виджета чатов снизу-справа")
    @FindBy("//div[contains(@class, 'ChatWidgetButtonView')]")
    VertisElement chatWidgetButton();

    @Name("Абзац")
    @FindBy("//p")
    VertisElement paragraph();

}
