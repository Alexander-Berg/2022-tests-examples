package ru.yandex.general.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.ShemaOrgMark;
import ru.yandex.general.element.Tab;
import ru.yandex.general.mobile.element.Button;
import ru.yandex.general.mobile.element.Footer;
import ru.yandex.general.mobile.element.Header;
import ru.yandex.general.mobile.element.Map;
import ru.yandex.general.mobile.element.Popup;
import ru.yandex.general.mobile.element.Screen;
import ru.yandex.general.mobile.element.SplashBanner;
import ru.yandex.general.mobile.element.StatisticsPopup;
import ru.yandex.general.mobile.element.TabBar;
import ru.yandex.general.mobile.element.Wrapper;

import java.util.Comparator;
import java.util.stream.Collectors;

public interface BasePage extends WebPage, Button {

    String JOB_POSTING = "JobPosting";
    String AGGREGATE_OFFER = "AggregateOffer";
    String ERROR_404_TEXT = "Вы ошиблись адресом,\nили такой страницы не существует.";
    String MAIN_PAGE_MOSCOW_H1 = "Объявления в Москве";
    String MAIN_PAGE_NOVOSIBIRSK_H1 = "Объявления в Новосибирске";

    @Name("Вся страница")
    @FindBy("//body")
    VertisElement pageRoot();

    @Name("Цвет страницы «{{ value }}»")
    @FindBy("//html[contains(@class, '{{ value }}')]")
    VertisElement pageColor(@Param("value") String value);

    @Name("Хэдер")
    @FindBy("//header")
    Header header();

    @Name("Футер")
    @FindBy("//footer")
    Footer footer();

    @Name("Seo description")
    @FindBy("//head/meta[@name = 'description']")
    VertisElement seoDescription();

    @Name("Экран")
    @FindBy("//div[contains(@class, 'Screen__wrapper')]")
    Wrapper wrapper();

    @Name("Список экранов")
    @FindBy("//div[contains(@class, 'Screen__wrapper')]")
    ElementsCollection<Wrapper> wrapperList();

    @Name("Экран «{{ value }}»")
    @FindBy("//div[contains(@class, 'Screen__wrapper')][.//span[contains(@class, 'title')][contains(., '{{ value }}')]]")
    Wrapper wrapper(@Param("value") String value);

    @Name("Экран")
    @FindBy("//div[contains(@class, 'Screen__baseContent')]")
    Screen screen();

    @Name("Таб-бар")
    @FindBy("//div[contains(@class, 'TabBar__container')]")
    TabBar tabBar();

    @Name("Карта")
    @FindBy("//ymaps")
    Map map();

    @Name("H1")
    @FindBy("//h1")
    VertisElement h1();

    @Name("Тайтл страницы")
    @FindBy("//div[contains(@class, 'PersonalHeader')]/span")
    VertisElement lkPageTitle();

    @Name("Кнопка «Назад»")
    @FindBy("//button[contains(@class, 'BackButton')]")
    VertisElement backButton();

    @Name("Попап паспортного логина")
    @FindBy("//div[contains(@class, 'PassportRequiredModal')]")
    Popup passportRequiredModal();

    @Name("Попап")
    @FindBy("//div[contains(@class, 'PopupMobile__contentEnterDone')]")
    Popup popup();

    @Name("Попап «{{ value }}»")
    @FindBy("//div[contains(@class, 'PopupMobile__contentEnterDone')][.//div[contains(@class, 'header')]" +
            "[contains(., '{{ value }}')]]")
    Popup popup(@Param("value") String value);

    @Name("Попап статистики")
    @FindBy("//div[contains(@class, 'PopupMobile__contentEnterDone')][./div[contains(@class, 'OfferStatisticsModal')]]")
    StatisticsPopup statisticsPopup();

    @Name("Скелетоны")
    @FindBy("//div[contains(@class, 'Skeleton')]")
    VertisElement skeleton();

    @Name("Всплывающее сообщение «{{ value }}»")
    @FindBy(".//div[contains(@class, 'NotificationsItem__content')][contains(.,'{{ value }}')]")
    Button popupNotification(@Param("value") String value);

    @Name("Карточка оффера")
    @FindBy("//div[contains(@class, 'OfferCard')]")
    VertisElement offerCard();

    @Name("Элемент хлебных крошек «{{ value }}»")
    @FindBy("//div[contains(@class, 'Breadcrumbs__item')][contains(., '{{ value }}')]")
    VertisElement breadcrumbsItem(@Param("value") String value);

    @Name("Фрейм с видео")
    @FindBy("//iframe[contains(@id, 'videoFrame')]")
    VertisElement videoFrame();

    @Name("Таб «{{ value }}»")
    @FindBy("//label[contains(@class, 'Tab__root')][contains(., '{{ value }}')]")
    Tab tab(@Param("value") String value);

    @Name("Разметка LdJson «{{ value }}»")
    @FindBy("//script[@type = 'application/ld+json'][contains(., '{{ value }}')]")
    VertisElement ldJson(@Param("value") String value);

    @Name("Разметка ShemaOrg «AggregateOffer»")
    @FindBy("//div[contains(@class, 'OfferListingSeo')]")
    ShemaOrgMark listingSeoBlock();

    @Name("Разметка LdJson «Logo»")
    @FindBy("//script[@type = 'application/ld+json'][contains(., 'Organization')][contains(., 'logo')]")
    VertisElement ldJsonLogo();

    @Name("Ошибка 500/404")
    @FindBy("//img[@class = 'img500']")
    VertisElement errorImage();

    @Name("Абзац")
    @FindBy("//p")
    VertisElement paragraph();

    @Name("Сплэш баннер сверху")
    @FindBy("//div[contains(@class, 'TopSplash__wrapper')]")
    SplashBanner topSplashBanner();

    @Name("Сплэш баннер снизу")
    @FindBy("//div[contains(@class, 'BottomSplash__wrapper')]")
    SplashBanner bottomSplashBanner();

    default Wrapper getDisplayedWrapper() {
        return wrapperList().stream()
                .sorted(Comparator.comparingInt(Wrapper::getZindex).reversed())
                .collect(Collectors.toList())
                .get(0);
    }

}
