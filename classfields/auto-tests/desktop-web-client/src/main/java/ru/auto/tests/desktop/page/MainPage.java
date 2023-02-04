package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithContactsPopup;
import ru.auto.tests.desktop.component.WithMag;
import ru.auto.tests.desktop.component.WithSavedSearchesPopup;
import ru.auto.tests.desktop.element.AppPromo;
import ru.auto.tests.desktop.element.DiscountTimer;
import ru.auto.tests.desktop.element.HorizontalCarousel;
import ru.auto.tests.desktop.element.Popup;
import ru.auto.tests.desktop.element.lk.CreditsForm;
import ru.auto.tests.desktop.element.main.AutoruChannel;
import ru.auto.tests.desktop.element.main.CatalogNews;
import ru.auto.tests.desktop.element.main.CommercialBlock;
import ru.auto.tests.desktop.element.main.LastSearches;
import ru.auto.tests.desktop.element.main.MagTeaser;
import ru.auto.tests.desktop.element.main.MarksBlock;
import ru.auto.tests.desktop.element.main.MotoBlock;
import ru.auto.tests.desktop.element.main.Presets;
import ru.auto.tests.desktop.element.main.QrCodePopup;
import ru.auto.tests.desktop.element.main.Reviews;
import ru.auto.tests.desktop.element.main.SocialBlock;
import ru.auto.tests.desktop.element.main.Textbook;

public interface MainPage extends BasePage, WithContactsPopup, WithMag, WithSavedSearchesPopup {

    @Name("Блок «Последние поиски»")
    @FindBy("//div[@class = 'Index__block IndexSearchHistory']")
    LastSearches lastSearches();

    @Name("Блок марок")
    @FindBy("//div[@class = 'IndexSelector__left' and .//span[.= 'Помощник']]")
    MarksBlock marksBlock();

    @Name("Пресеты")
    @FindBy("//div[@class = 'IndexPresets Index__block']")
    Presets presets();

    @Name("Блок «Мототехника»")
    @FindBy("//div[@class = 'IndexMoto Index__block']")
    MotoBlock motoBlock();

    @Name("Блок «Коммерческий транспорт»")
    @FindBy("//div[@class = 'IndexCommercial Index__block']")
    CommercialBlock commercialBlock();

    @Name("Блок «Новинки каталога»")
    @FindBy("//div[@class = 'IndexCatalog Index__block']")
    CatalogNews catalogNews();

    @Name("Блок «Журнал» в сайдбаре")
    @FindBy("//div[contains(@class, 'JournalTeaser IndexSelector__right')]")
    MagTeaser magTeaser();

    @Name("Блок «Канал Авто.ру»")
    @FindBy("//div[@class = 'IndexPlaylists Index__block']")
    AutoruChannel autoruChannel();

    @Name("Поп-ап с видео канала Авто.ру")
    @FindBy("//div[contains(@class, 'VideoModal')]")
    VertisElement videoPopup();

    @Name("Блок «Учебник Авто.ру»")
    @FindBy("//div[@class = 'IndexTextbook Index__block']")
    Textbook textbook();

    @Name("Промо приложения»")
    @FindBy("//div[contains(@class, 'AppStoreBanner')]")
    AppPromo appPromo();

    @Name("Блок соцкнопок»")
    @FindBy("//div[contains(@class, 'IndexSocialLinks Index__col')]")
    SocialBlock socialBlock();

    @Name("Виджет новинок")
    @FindBy("//div[@class = 'brand-new__frame']")
    MarksBlock brandNewWidget();

    @Name("Блок «Отзывы владельцев»")
    @FindBy("//div[@class = 'IndexReviews Index__block']")
    Reviews reviews();

    @Name("Блок «Избранное»")
    @FindBy(".//div[contains(@class, 'IndexFavorites Index__block')]")
    HorizontalCarousel favorites();

    @Name("Блок «Вы смотрели»")
    @FindBy(".//div[contains(@class, 'IndexWatched Index__block')]" +
            "//div[contains(@class, 'CarouselUniversal_dir_horizontal')]")
    HorizontalCarousel watched();

    @Name("Блок «Персонально для вас»")
    @FindBy(".//div[contains(@class, 'IndexPersonalized Index__block')]")
    HorizontalCarousel personalized();

    @Name("Таймер скидки")
    @FindBy("//div[contains(@class, 'PromoPopupDiscountTimer')]")
    DiscountTimer discountTimer();

    @Name("Поп-ап QR-кода")
    @FindBy("//div[contains(@class, 'AppStoreButtons__modal')]")
    QrCodePopup qrCodePopup();

    @Name("Поп-ап короткой заявки на кредит")
    @FindBy("//div[contains(@class, 'CreditApplicationForm')]")
    CreditsForm creditApplicationPopup();

    @Name("Попап «Можем ли мы позвонить?»")
    @FindBy("//div[contains(@class, 'Modal_visible')][.//div[@class = 'CanWeCallYouModal']]")
    Popup canWeCallYouPopup();

    @Name("Попап «Сэкономьте на покупке автомобиля!»")
    @FindBy("//div[contains(@class, 'Modal_visible')][.//div[contains(@class, 'PromoPopupGreatDeal')]]")
    Popup greatDealPopup();

    @Name("Попап «Только на Авто.ру»")
    @FindBy("//div[contains(@class, 'Modal_visible')][.//div[contains(@class, 'PromoPopupAutoruExclusive')]]")
    Popup autoruExclusivePopup();

    @Name("Попап «Входите на Авто.ру одним кликом»")
    @FindBy("//div[@class = 'NotificationBase__wrapper'][.//div[contains(@class, 'NotificationYandexAuthSuggest')]]")
    Popup yandexAuthPopup();

}
