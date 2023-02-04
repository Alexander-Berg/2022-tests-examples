package ru.auto.tests.desktop.mobile.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithAppPromo;
import ru.auto.tests.desktop.mobile.component.WithBillingPopup;
import ru.auto.tests.desktop.mobile.component.WithBreadcrumbs;
import ru.auto.tests.desktop.mobile.component.WithCallbackPopup;
import ru.auto.tests.desktop.mobile.component.WithGallery;
import ru.auto.tests.desktop.mobile.component.WithMag;
import ru.auto.tests.desktop.mobile.component.WithReviews;
import ru.auto.tests.desktop.mobile.component.WithReviewsPlusMinusPopup;
import ru.auto.tests.desktop.mobile.component.WithTextbook;
import ru.auto.tests.desktop.mobile.component.WithVideos;
import ru.auto.tests.desktop.mobile.element.DealBlock;
import ru.auto.tests.desktop.mobile.element.ExistingDealBanner;
import ru.auto.tests.desktop.mobile.element.cardpage.AddedToGarageBanner;
import ru.auto.tests.desktop.mobile.element.cardpage.Benefits;
import ru.auto.tests.desktop.mobile.element.cardpage.BookingPopup;
import ru.auto.tests.desktop.mobile.element.cardpage.CardActions;
import ru.auto.tests.desktop.mobile.element.cardpage.CardCreditBlock;
import ru.auto.tests.desktop.mobile.element.cardpage.CardDateAndStats;
import ru.auto.tests.desktop.mobile.element.cardpage.Complectation;
import ru.auto.tests.desktop.mobile.element.cardpage.Contacts;
import ru.auto.tests.desktop.mobile.element.cardpage.Damages;
import ru.auto.tests.desktop.mobile.element.cardpage.ExchangeGarageModal;
import ru.auto.tests.desktop.mobile.element.cardpage.Features;
import ru.auto.tests.desktop.mobile.element.cardpage.FloatingActualizeButton;
import ru.auto.tests.desktop.mobile.element.cardpage.FloatingContacts;
import ru.auto.tests.desktop.mobile.element.cardpage.GalleryStub;
import ru.auto.tests.desktop.mobile.element.cardpage.OwnerControls;
import ru.auto.tests.desktop.mobile.element.cardpage.PrevNext;
import ru.auto.tests.desktop.mobile.element.cardpage.PrevNextInfo;
import ru.auto.tests.desktop.mobile.element.cardpage.Price;
import ru.auto.tests.desktop.mobile.element.cardpage.Related;
import ru.auto.tests.desktop.mobile.element.cardpage.SameButNew;
import ru.auto.tests.desktop.mobile.element.cardpage.SellerComment;
import ru.auto.tests.desktop.mobile.element.cardpage.SoldPopup;
import ru.auto.tests.desktop.mobile.element.cardpage.Specials;
import ru.auto.tests.desktop.mobile.element.cardpage.TradeIn;
import ru.auto.tests.desktop.mobile.element.cardpage.VinReport;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

/**
 * Created by kopitsa on 15.09.17.
 */
public interface CardPage extends BasePage, WithGallery, WithAppPromo, WithTextbook, WithBreadcrumbs,
        WithMag, WithReviews, WithReviewsPlusMinusPopup, WithVideos, WithBillingPopup, WithCallbackPopup {

    String COMPLAIN_BUTTON = "Пожаловаться на объявление";

    @Name("Цена")
    @FindBy(".//div[contains(@class, 'card-price i-bem')] | " +
            ".//div[contains(@class, 'CardPrice')]")
    Price price();

    @Name("Предложение кредита")
    @FindBy(".//span[contains(@class, 'CreditPrice_type_link')]")
    VertisElement creditOffer();

    @Name("Дата и статистика")
    @FindBy("//div[contains(@class, 'CardDatesAndStats')]")
    CardDateAndStats dateAndStats();

    @Name("Количество просмотров")
    @FindBy("//div[contains(@class, 'item_views')]")
    VertisElement views();

    @Name("Характеристики")
    @FindBy("//div[contains(@class, 'card__features features')] | " +
            "//div[contains(@class, 'CardTechParams')]")
    Features features();

    @Name("Комплектация")
    @FindBy("//div[contains(@class, 'card__options')] | " +
            "//div[contains(@class, 'PageCard__complectation')] | " +
            "//section[contains(@class, 'OfferAmpComplectation PageCard__')]")
    Complectation complectation();

    @Name("Заголовок объявления")
    @FindBy("//h1")
    VertisElement title();

    @Name("Ссылка «Все характеристики»")
    @FindBy(".//a[contains(@class, 'TechParams__catalogLink')]")
    VertisElement allFeaturesUrl();

    @Name("Блок комментария от продавца")
    @FindBy("//div[contains(@class, 'card__description')] | " +
            "//div[contains(@class, 'PageCard__description')]")
    SellerComment sellerComment();

    @Name("Сообщение, что объявление продано")
    @FindBy("//div[contains(@class, 'PageCard__sold')]")
    VertisElement statusSold();

    @Name("Сообщение, что объявление удалено")
    @FindBy("//div[contains(@class, 'PageCard__cardStatus_removed')]")
    VertisElement statusRemoved();

    @Name("Контакты")
    @FindBy("//div[contains(@class, 'card__contacts')] | " +
            "//div[@class = 'CardSellerInfo'] | " +
            "//div[@class = 'OfferSellerInfo'] | " +
            "//div[@class = 'OfferAmpSellerInfo']")
    Contacts contacts();

    @Name("Поп-ап причин снятия с продажи")
    @FindBy("//div[contains(@class, 'Modal_visible')]//div[@class='Modal__content']")
    SoldPopup soldPopup();

    @Name("Спецпредложения")
    @FindBy("//div[contains(@class, 'WidgetSpecialOffers')]")
    Specials specials();

    @Name("Похожие")
    @FindBy("//div[contains(@class, 'LazyComponent') and .//div[.= 'Похожие объявления']] | " +
            "//div[contains(@class, 'PageCard__sectionVertical') and .//div[.= 'Похожие объявления']]")
    Related related();

    @Name("Действия с объявлением")
    @FindBy(".//div[contains(@class, 'CardActions')]")
    CardActions cardActions();

    @Name("Заметка")
    @FindBy(".//div[@class = 'CardNote__note']")
    VertisElement note();

    @Name("Панель владельца")
    @FindBy("//div[contains(@class, 'CardOwnerControls')]")
    OwnerControls ownerControls();

    @Name("Блок сертификации производителя")
    @FindBy("//div[contains(@class, 'card-manufacturer-cert')] | " +
            "//section[contains(@class, 'CardBrandCertInfo')]")
    VertisElement manufacturerCert();

    @Name("Иконка вызова поп-апа с описанием сертификации производителя")
    @FindBy("//div[contains(@class, 'CardBrandCertInfo__logo-container')]")
    VertisElement manufacturerCertLogo();

    @Name("Кнопки перехода на предыдущее/следующее объявления")
    @FindBy("//div[contains(@class, 'CardPrevNext')]")
    PrevNext prevNext();

    @Name("Кнопка «Заказать обратный звонок»")
    @FindBy(".//button[contains(@class, 'CallbackButton')]")
    VertisElement callbackButton();

    @Name("Блок «Обменяйте свой автомобиль на этот\n»")
    @FindBy("//section[contains(@class, 'CardTradeinMobile PageCard__section')]")
    TradeIn tradeIn();

    @Name("Плавающая кнопка актуальности")
    @FindBy("//div[contains(@class, 'CardActualizeButton')]")
    FloatingActualizeButton floatingActualizeButton();

    @Name("Блок кредита")
    @FindBy("//div[contains(@class, 'DealerCreditForm')] | " +
            "//div[contains(@class, 'CreditCard')] |" +
            "//div[contains(@class, 'CreditCardRoot')]")
    CardCreditBlock cardCreditBlock();

    @Name("Сниппет кредитной завки")
    @FindBy("//div[contains(@class, 'CreditFlowClaimSnippet')]")
    VertisElement cardCreditClaimSnippet();

    @Name("Шторка кредита")
    @FindBy("//div[contains(@class, 'Curtain_open')]")
    VertisElement creditCurtain();

    @Name("Заглушка для галереи")
    @FindBy("//a[contains(@class, 'CardGalleryPlaceholder')]")
    GalleryStub galleryStub();

    @Name("Причины блокировки")
    @FindBy("//div[contains(@class, 'BanReason')]")
    VertisElement banReasons();

    @Name("Повреждения")
    @FindBy("//div[contains(@class, 'PageCard__damages')]")
    Damages damages();

    @Name("Преимущества")
    @FindBy("//div[contains(@class, 'CardBenefits')]")
    Benefits benefits();

    @Name("Блок «Доставка из»")
    @FindBy("//div[@class = 'CardDeliveryInfo__container']")
    VertisElement deliveryInfo();

    @Name("Плавающие контакты")
    @FindBy("//div[contains(@class, 'OfferFloatPhones')] | " +
            "//div[contains(@class, 'OfferAmpFloatPhones')]")
    FloatingContacts floatingContacts();

    @Name("Пояснение про переход на предыдущее/следующее объявление")
    @FindBy("//div[contains(@class, 'CardPrevNext__curtainContent')]")
    PrevNextInfo prevNextinfo();

    @Name("Кнопка «Забронировать автомобиль»")
    @FindBy("//button[contains(@class, 'BookingModal__card-button')]")
    VertisElement bookingButton();

    @Name("Поп-ап бронирования")
    @FindBy("//div[contains(@class, 'BookingModal')]")
    BookingPopup bookingPopup();

    @Name("Статус бронирования")
    @FindBy("//div[contains(@class, 'BookingStatus')]")
    VertisElement bookingStatus();

    @Name("Отчёт «Проверка по VIN»")
    @FindBy("//div[@name = 'vinReport'] | " +
            "//div[contains(@class, 'card-avtokod ')] | " +
            "//div[contains(@class, 'CardVinReport')] | " +
            "//div[contains(@class, 'OfferAmpVinReport')]")
    VinReport vinReport();

    @Name("Такой же, но новый")
    @FindBy("//div[contains(@class, 'IndexBlock PageCard__sectionVertical')]")
    SameButNew sameButNew();

    @Name("Блок безопасной сделки")
    @FindBy("//div[@class = 'SafeDealBlockMobile']")
    DealBlock dealBlock();

    @Name("Баннер созданной безопасной сделки")
    @FindBy("//div[contains(@class, 'CardSafeDeal__created')]")
    ExistingDealBanner existingDealBanner();

    @Name("Баннер «Мы добавили ваш автомобиль в Гараж»")
    @FindBy("//div[contains(@class, 'AddedToGarageBanner ')]")
    AddedToGarageBanner addedToGarageBanner();

    @Name("Модалка выбора авто из гаража для обмена")
    @FindBy("//div[contains(@class, 'Modal_visible')][.//div[contains(@class, 'OfferPriceExchangeGarageModal')]]")
    ExchangeGarageModal exchangeGarageModal();

    @Name("Элемент скролла хлебных крошек")
    @FindBy("//div[@class = 'PageCard__breadcrumbsScroller']")
    VertisElement breadcrumbsScrollView();

    @Step("Добавляем заметку")
    default void addNote(String noteText) {
        cardActions().noteButton().should(WebElementMatchers.isDisplayed()).hover().click();
        cardActions().notePopup().input().waitUntil(WebElementMatchers.isDisplayed()).sendKeys(noteText);
        cardActions().notePopup().button("Сохранить").should(WebElementMatchers.isDisplayed()).click();
    }
}
