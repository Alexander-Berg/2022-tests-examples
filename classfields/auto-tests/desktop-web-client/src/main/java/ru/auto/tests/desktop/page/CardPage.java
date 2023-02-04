package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithActivatePopup;
import ru.auto.tests.desktop.component.WithAuthPopup;
import ru.auto.tests.desktop.component.WithAutoProlongDiscountBanner;
import ru.auto.tests.desktop.component.WithAutoProlongFailedBanner;
import ru.auto.tests.desktop.component.WithAutoProlongInfo;
import ru.auto.tests.desktop.component.WithBillingModalPopup;
import ru.auto.tests.desktop.component.WithBreadcrumbs;
import ru.auto.tests.desktop.component.WithCallbackPopup;
import ru.auto.tests.desktop.component.WithCardBadges;
import ru.auto.tests.desktop.component.WithCardContacts;
import ru.auto.tests.desktop.component.WithCardHeader;
import ru.auto.tests.desktop.component.WithCardVas;
import ru.auto.tests.desktop.component.WithCheckWalletBanner;
import ru.auto.tests.desktop.component.WithContactsPopup;
import ru.auto.tests.desktop.component.WithFullScreenGallery;
import ru.auto.tests.desktop.component.WithGallery;
import ru.auto.tests.desktop.component.WithMag;
import ru.auto.tests.desktop.component.WithReviews;
import ru.auto.tests.desktop.component.WithReviewsPromo;
import ru.auto.tests.desktop.component.WithSavedSearchesPopup;
import ru.auto.tests.desktop.component.WithShare;
import ru.auto.tests.desktop.component.WithSoldPopup;
import ru.auto.tests.desktop.component.WithVideos;
import ru.auto.tests.desktop.component.WithVinReport;
import ru.auto.tests.desktop.component.WithYaKassa;
import ru.auto.tests.desktop.element.DealBlock;
import ru.auto.tests.desktop.element.DeliveryInfo;
import ru.auto.tests.desktop.element.ExistingDealBanner;
import ru.auto.tests.desktop.element.HorizontalCarousel;
import ru.auto.tests.desktop.element.PopularBanner;
import ru.auto.tests.desktop.element.Popup;
import ru.auto.tests.desktop.element.card.AddedToGarageBanner;
import ru.auto.tests.desktop.element.card.BannedMessage;
import ru.auto.tests.desktop.element.card.Benefits;
import ru.auto.tests.desktop.element.card.BookingPopup;
import ru.auto.tests.desktop.element.card.BookingStatus;
import ru.auto.tests.desktop.element.card.CallHistoryPromo;
import ru.auto.tests.desktop.element.card.CardComplectation;
import ru.auto.tests.desktop.element.card.CardContent;
import ru.auto.tests.desktop.element.card.CardCreditBlock;
import ru.auto.tests.desktop.element.card.CardOwnerPanel;
import ru.auto.tests.desktop.element.card.Damages;
import ru.auto.tests.desktop.element.card.Features;
import ru.auto.tests.desktop.element.card.PricePopup;
import ru.auto.tests.desktop.element.card.StickyBar;
import ru.auto.tests.desktop.element.card.Taxi;
import ru.auto.tests.desktop.element.card.Textbook;
import ru.auto.tests.desktop.element.card.VinHistoryCommentsPopup;

public interface CardPage extends BasePage,
        WithCardHeader,
        WithBreadcrumbs,
        WithGallery,
        WithFullScreenGallery,
        WithCardVas,
        WithBillingModalPopup,
        WithCallbackPopup,
        WithCardBadges,
        WithCardContacts,
        WithContactsPopup,
        WithShare,
        WithSoldPopup,
        WithReviewsPromo,
        WithActivatePopup,
        WithYaKassa,
        WithAuthPopup,
        WithVinReport,
        WithAutoProlongInfo,
        WithAutoProlongFailedBanner,
        WithAutoProlongDiscountBanner,
        WithCheckWalletBanner,
        WithMag,
        WithSavedSearchesPopup,
        WithReviews,
        WithVideos {

    @Name("Панель владельца")
    @FindBy("//div[contains(@class,'owner-panel')] | " +
            "//div[@class='CardOwnerControls']")
    CardOwnerPanel cardOwnerPanel();

    @Name("Контент карточки")
    @FindBy("//div[contains(@class, 'card__content')] | " +
            "//div[contains(@class, 'LayoutSidebar__content')]")
    CardContent cardContent();

    @Name("Повреждения")
    @FindBy("//div[contains(@class, 'VehicleBodyDamages ')]")
    Damages damages();

    @Name("Параметры")
    @FindBy("//ul[contains(@class, 'CardInfo')]")
    Features features();

    @Name("Комментарий продавца")
    @FindBy("//div[@class = 'cardContacts-details i-bem'] | " +
            "//div[@name = 'description'] | " +
            "//div[contains(@class, 'CardDescription')] |" +
            "//div[contains(@class, 'seller-details')]")
    VertisElement sellerComment();

    @Name("Поп-ап с ценами")
    @FindBy("//div[contains(@class, 'card__price-curr-popup')] | " +
            "//div[contains(@class, 'Price-module__popup')] | " +
            "//div[contains(@class, 'PriceNewOffer__popup')] | " +
            "//div[contains(@class, 'Popup_visible')]//div[contains(@class, 'Offer-module__price')] | " +
            "//div[contains(@class, 'Popup_visible')]//div[contains(@class, 'OfferOwnerGoodPriceDesktop')] | " +
            "//div[contains(@class, 'Popup_visible')]//div[contains(@class, 'PriceUsedOffer__price')]")
    PricePopup pricePopup();

    @Name("Попап с выбором авто из гаража на обмен")
    @FindBy("//div[contains(@class, 'OfferPriceExchangeGaragePopup') and contains(@class, 'Popup_visible')]")
    Popup exchangeCarPopup();

    @Name("Плавающая панель")
    @FindBy("//div[contains(@class, 'CardStickyBar CardStickyBar_stuck')]")
    StickyBar stickyBar();

    @Name("Заглушка для галереи")
    @FindBy("//div[contains(@class, 'CardImageGallery_empty')]")
    VertisElement galleryStub();

    @Name("Кнопка «Добавить фото»")
    @FindBy("//*[contains(@class, 'CardImageGallery')]/a[.='Добавить фото']")
    VertisElement galleryAddPhotoButton();

    @Name("Комплектация")
    @FindBy("//div[contains(@class, 'CardComplectation-module__CardComplectation')] | " +
            "//section[contains(@class, 'CardComplectation')]")
    CardComplectation complectation();

    @Name("Плашка «Продано»")
    @FindBy("//div[@class = 'CardSold'] | " +
            "//div[@class = 'card__sold']")
    VertisElement soldMessage();

    @Name("Блок сертификации производителя")
    @FindBy("//div[@class = 'CardManufacturerCertPlate']")
    VertisElement manufacturerCert();

    @Name("Иконка вызова поп-апа с описанием сертификации производителя")
    @FindBy("//div[@class = 'CardManufacturerCertPlate__info']")
    VertisElement manufacturerCertHintIcon();

    @Name("Промо истории звонков")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    CallHistoryPromo callHistoryPromo();

    @Name("Блок модератора")
    @FindBy("//iframe[contains(@class, 'CardModeration')]")
    VertisElement moderationBlock();

    @Name("Блок «Учебник Авто.ру»")
    @FindBy("//div[@name = 'tutorial']")
    Textbook textbook();

    @Name("Скидки")
    @FindBy(".//div[contains(@class, 'CardOfferBody__discountList')]")
    VertisElement discounts();

    @Name("Плашка «Заблокировано модератором»")
    @FindBy("//div[contains(@class, 'BanMessage')]")
    BannedMessage bannedMessage();

    @Name("Блок кредита")
    @FindBy("//div[@name = 'credit'] | " +
            "//div[contains(@class, 'DealerCreditForm')] | " +
            "//div[contains(@class, 'CreditCard')]")
    CardCreditBlock cardCreditBlock();

    @Name("Шторка кредита")
    @FindBy("//div[contains(@class, 'Curtain_open')]")
    VertisElement creditCurtain();

    @Name("Блок «Доставка из»")
    @FindBy("//div[contains(@class, 'CardDeliveryInfo')]")
    DeliveryInfo deliveryInfo();

    @Name("Баннер «Это объявление популярнее, чем ваше»")
    @FindBy("//div[contains(@class, 'InfoBubble_visible')]")
    PopularBanner morePopularBanner();

    @Name("Баннер «Это объявление популярнее, чем ваше» цвета «{{ color }}»")
    @FindBy("//div[contains(@class, 'InfoBubble_theme_{{ color }}')]")
    PopularBanner morePopularBanner(@Param("color") String color);

    @Name("Поп-ап «Комментируйте отчёт о своём автомобиле»")
    @FindBy("//div[contains(@class, 'Modal_visible') and .//div[contains(@class, 'CardPromoAboutComments')]]")
    VinHistoryCommentsPopup vinHistoryCommentsPopup();

    @Name("Преимущества")
    @FindBy("//div[contains(@class, 'CardBenefits')]")
    Benefits benefits();

    @Name("Попап преимущества")
    @FindBy("//div[@class = 'CardBenefits__item-popup']")
    Popup benefitPopup();

    @Name("Кнопка «Забронировать автомобиль»")
    @FindBy("//button[contains(@class, 'BookingModal__card-button')]")
    VertisElement bookingButton();

    @Name("Поп-ап бронирования")
    @FindBy("//div[contains(@class, 'BookingModal')]")
    BookingPopup bookingPopup();

    @Name("Статус бронирования")
    @FindBy("//div[contains(@class, 'BookingStatus')]")
    BookingStatus bookingStatus();

    @Name("Блок «Зарабатывайте на своём авто»")
    @FindBy("//div[contains(@class, 'TaxiPromoForTruckDrivers')]")
    Taxi taxi();

    @Name("Такой же, но новый")
    @FindBy("//div[contains(@class, 'CardSameButNew')]")
    HorizontalCarousel sameButNew();

    @Name("Поп-ап пользовательского соглашения")
    @FindBy("//div[contains(@class, 'Popup_visible') and not(contains(@class, 'CardCallbackButton')) " +
            "and .//div[contains(@class, 'termsPopup')]]")
    VertisElement userAgreementPopup();

    @Name("Поп-ап с историей звонков")
    @FindBy("//div[contains(@class, 'CallsHistoryModal')]//div[@class='Modal__content'] | " +
            "//div[@class = 'OfferCallHistoryModalContent']")
    VertisElement callHistoryPopup();

    @Name("Блок безопасной сделки")
    @FindBy("//div[@class = 'SafeDealBlockDesktop']")
    DealBlock dealBlock();

    @Name("Баннер созданной безопасной сделки")
    @FindBy("//div[contains(@class, 'CardSafeDeal__created')]")
    ExistingDealBanner existingDealBanner();

    @Name("Баннер «Мы добавили ваш автомобиль в Гараж»")
    @FindBy("//div[contains(@class, 'AddedToGarageBanner ')]")
    AddedToGarageBanner addedToGarageBanner();

    @Name("Тултип перекупа")
    @FindBy("//div[contains(@class, 'Popup_visible')]//div[@class = 'CardSellerNamePlace__resellerPopup']")
    VertisElement resellerTooltip();

    @Name("Попап «Безопасная сделка»")
    @FindBy("//div[contains(@class, 'Modal_visible')][.//div[@class = 'PromoPopupSafeDealSellerOnboarding']]")
    Popup safeDealOnboardingPopup();

}
