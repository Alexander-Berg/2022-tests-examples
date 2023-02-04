package ru.auto.tests.desktop.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import org.openqa.selenium.By;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithActivePopup;
import ru.auto.tests.desktop.component.WithBillingModalPopup;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithFavoritesPopup;
import ru.auto.tests.desktop.component.WithFooter;
import ru.auto.tests.desktop.component.WithHeader;
import ru.auto.tests.desktop.component.WithHeaderSavedSearchesPopup;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithNewTrust;
import ru.auto.tests.desktop.component.WithNotifier;
import ru.auto.tests.desktop.component.WithSideBar;
import ru.auto.tests.desktop.component.WithSubHeader;
import ru.auto.tests.desktop.component.WithYaKassa;
import ru.auto.tests.desktop.element.AppPromo;
import ru.auto.tests.desktop.element.DealersBlock;
import ru.auto.tests.desktop.element.DiscountPopup;
import ru.auto.tests.desktop.element.HorizontalCarousel;
import ru.auto.tests.desktop.element.NoMobileBanner;
import ru.auto.tests.desktop.element.Popup;
import ru.auto.tests.desktop.element.VerticalCarousel;
import ru.auto.tests.desktop.element.chat.Chat;
import ru.auto.tests.desktop.element.main.GeoSelectPopup;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

public interface BasePage extends WebPage,
        WithHeader,
        WithSubHeader,
        WithFooter,
        WithSideBar,
        WithActivePopup,
        WithHeaderSavedSearchesPopup,
        WithNotifier,
        WithFavoritesPopup,
        WithBillingModalPopup,
        WithYaKassa,
        WithNewTrust,
        WithButton,
        WithCheckbox,
        WithInput {

    @Name("Баннер «Перейти на мобильную версию Авто.ру?»")
    @FindBy("//div[contains(@class, 'MobileRedirectBanner')]")
    NoMobileBanner noMobileBanner();

    @FindBy("//body")
    VertisElement body();

    @Name("Содержимое страницы")
    @FindBy("//div[@class = 'content']")
    VertisElement content();

    @FindBy("//h1")
    VertisElement h1();

    @FindBy("//h2")
    VertisElement h2();

    @Name("Тэг title")
    @FindBy("//title")
    VertisElement title();

    @Name("Мета og:image")
    @FindBy("//meta[@property = 'og:image']")
    VertisElement metaOgImage();

    @Name("Поп-ап регионов")
    @FindBy("//div[contains(@class, 'GeoSelectPopup')]")
    GeoSelectPopup geoSelectPopup();

    @Name("Фрейм Яндекс.карт")
    @FindBy("//ymaps[contains(@class, 'ymaps-')]")
    VertisElement yandexMap();

    @Name("Поп-ап")
    @FindBy("//div[contains(@class, 'Popup_visible')] | " +
            "//div[contains(@class, 'Modal_visible')] | " +
            "//div[contains(@class, 'popup_visible')]")
    Popup popup();

    @Name("Блок дилеров")
    @FindBy("//div[contains(@class, 'related-dealers services-crosslinks__item')] | " +
            "//div[contains(@class, 'ListingDealersList')]")
    DealersBlock dealersBlock();

    @Name("Точка на карте")
    @FindBy("//ymaps[contains(@class, 'svg-icon ymaps_https___api_maps_yandex')]")
    VertisElement mapPoint();

    @Name("Промо мобильного приложения")
    @FindBy("//div[@class='promo i-bem'] | " +
            "//div[@class='PromoFooterMobileApp-module__container']")
    AppPromo appPromo();

    @Name("Чат")
    @FindBy("//div[contains(@class, 'ChatApp_visible')]")
    Chat chat();

    @Name("Поп-ап авторизации чата")
    @FindBy("//div[contains(@class, 'HeaderChatAuthPopup_open')]")
    VertisElement chatAuthPopup();

    @Name("Тултип о электромобилях")
    @FindBy("//div[contains(@class, 'Popup_visible')]//div[contains(@class, '_electroTooltip')]")
    VertisElement electroTooltip();

    @Name("Похожие объявления горизонтальным списком")
    @FindBy("//div[contains(@class, 'sale-similar-carousel')] | " +
            "//div[contains(@class, 'CarouselLazyOffers')][contains(., 'Похожие')] | " +
            "//div[contains(@class, 'carousel_lazy')][contains(., 'Похожие авто')] | " +
            "//div[contains(@class, 'CarouselUniversal_dir_horizontal')][contains(., 'Похожие')]")
    HorizontalCarousel horizontalRelated();

    @Name("Похожие объявления вертикальным списком")
    @FindBy("//div[@class = 'UpdatableListOffers'] | " +
            "//div[@class = 'VersusRelatedOffers'] |" +
            "//div[contains(@class, 'PageVersusDesktop__offers')]")
    VerticalCarousel verticalRelated();

    @Name("Спецпредложения")
    @FindBy(".//div[contains(@class, 'specials-sales')] | " +
            ".//div[contains(@class, 'CarouselLazyOffers') and .//div[contains(., 'Спецпредложения')]] | " +
            ".//div[contains(@class, 'CarouselUniversal_dir_horizontal') and .//div[contains(., 'Спецпредложения')]]")
    HorizontalCarousel specialSales();

    @Name("Предложения дня горизонтальным списком")
    @FindBy(".//div[contains(@class, 'CarouselLazyOffers') and .//div[contains(., 'Предложения дня')]] | " +
            ".//div[contains(@class, 'CarouselUniversal_dir_horizontal') and .//div[contains(., 'Предложения дня')]]")
    HorizontalCarousel horizontalDailyOffers();

    @Name("Предложения дня вертикальным списком")
    @FindBy("//div[@class = 'UpdatableListOffers']")
    VerticalCarousel verticalDailyOffers();

    @Name("Поп-ап скидки")
    @FindBy("//div[contains(@class, 'PromoPopupDiscount') and contains(@class, 'Modal_visible')]" +
            "//div[contains(@class, 'Modal__content')]")
    DiscountPopup discountPopup();

    @Name("Маркетинговый iframe")
    @FindBy("//iframe[@id = 'marketing-iframe']")
    VertisElement marketingIframe();

    @Name("Жук")
    @FindBy("//div[contains(@class, 'YndxBug')]")
    VertisElement bug();

    @Name("Плашка с веткой")
    @FindBy("//div[@class = 'DevToolsBranch']")
    VertisElement branch();

    @Name("Альтернейт")
    @FindBy("//link[@rel='alternate']")
    VertisElement alternate();

    @Name("Каноникл")
    @FindBy("//link[@rel='canonical']")
    VertisElement canonical();

    @Name("Попап с нотификацией")
    @FindBy("//div[@class = 'NotificationBase__content']")
    VertisElement notificationPopup();

    @Step("Переключаемся на фрейм с поп-апом оплаты")
    default void switchToBillingFrame() {
        billingPopupFrame().waitUntil(WebElementMatchers.isDisplayed(), 10);

        getWrappedDriver().switchTo().frame(getWrappedDriver()
                .findElement(By.xpath("//div[@class = 'PaymentDialogContainer__frame']/iframe | " +
                        "//div[@class = 'pay-card__iframe']/iframe | " +
                        "//iframe[@class = 'PaymentDialogContainer__frame'] | " +
                        "//iframe[@class = 'billing-container__frame']")));
    }

    @Step("Переключаемся на фрейм Я.Кассы")
    default void switchToYaKassaFrame() {
        getWrappedDriver().switchTo().frame(getWrappedDriver()
                .findElement(By.xpath("//iframe[contains(@class, 'Billing__cardFrame')]")));
    }

    @Step("Переключаемся на фрейм нового траста")
    default void switchToNewTrustFrame() {
        getWrappedDriver().switchTo().frame(getWrappedDriver()
                .findElement(By.xpath("//iframe[contains(@class, 'YpcDieHardFrame')]")));
    }

    @Step("Переключаемся на фрейм поп-апа авторизации")
    default void switchToAuthPopupFrame() {
        getWrappedDriver().switchTo().frame(getWrappedDriver()
                .findElement(By.xpath("//div[contains(@class, 'Modal__content AuthModal')]//iframe")));
    }

    @Name("Фрейм видео")
    @FindBy("//iframe[contains(@title, 'www.youtube.com/embed')]")
    VertisElement videoFrame();

    @Name("Аккаунт в модалке выбора паспортного акка")
    @FindBy("//a[contains(@class, 'AuthAccountListItem')]")
    VertisElement passportAccount();

}
