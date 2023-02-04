package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.mobile.element.CardSectionExpandable;
import ru.yandex.realty.mobile.element.ComplainBlock;
import ru.yandex.realty.mobile.element.Link;
import ru.yandex.realty.mobile.element.MobilePopup;
import ru.yandex.realty.mobile.element.ShareBlock;
import ru.yandex.realty.mobile.element.SimilarTouchOffer;
import ru.yandex.realty.mobile.element.offercard.EgrnBlock;
import ru.yandex.realty.mobile.element.offercard.OfferGallery;
import ru.yandex.realty.mobile.element.offercard.PriceHistoryModal;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface OfferCardPage extends BasePage, Link, Button, ShareBlock, InputField {

    String CALL_BUTTON = "Позвонить";
    String ADD_TO_FAV = "Добавить в избранное";
    String DEL_FROM_FAV = "Удалить из избранного";
    String SHARE = "Поделиться";

    @Name("Кнопка «Назад»")
    @FindBy("//div[@class='OfferCard']//span[contains(@class,'NavBar__button-back')]")
    AtlasWebElement navButtonBackOnOffer();

    @Name("Изменение цены. Вниз")
    @FindBy(".//span[contains(@class,'PriceDiffLabel_decreased')]")
    AtlasWebElement priceDecreased();

    @Name("Изменение цены. Вверх")
    @FindBy(".//span[contains(@class,'PriceDiffLabel_increased')]")
    AtlasWebElement priceIncreased();

    @Name("Блок «Объявление устарело»")
    @FindBy(".//div[@class='OfferCardInactive']")
    AtlasWebElement inactiveBlock();

    @Name("Кнопка «История изменения цены»")
    @FindBy(".//span[contains(@class,'OfferCardHeader__price-history-link')]")
    AtlasWebElement priceHistoryButton();

    @Name("Модуль истории изменения цены")
    @FindBy(".//div[contains(@class,'OfferCardPriceHistory__modal') and contains(@class,'Modal_visible')]")
    PriceHistoryModal priceHistoryModal();

    @Name("Модуль нескольких телефонов")
    @FindBy(".//div[contains(@class,'Modal_visible') and contains(@class, 'FixedModal')]//a")
    ElementsCollection<AtlasWebElement> phones();

    @Name("Кнопка «Позвонить»")
    @FindBy(".//div[@class='OfferCardPhone']")
    Link makePhone();

    @Name("Фото оффера")
    @FindBy(".//div[contains(@class,'CardMainGallery')]//div[@aria-hidden='false']//div[@class='SwipeGallery__thumb']")
    AtlasWebElement offerPhoto();

    @Name("Галерея оффера")
    @FindBy(".//div[contains(@class,'SwipeableFSGallery__layout')]")
    OfferGallery offersGallery();

    @Name("Кнопка «Добавить в избранное в галерее»")
    @FindBy(".//div[contains(@class,'OfferFSGallerySnippet')]//button[contains(@class,'ItemAddToFavorite')]")
    AtlasWebElement addToFavInGallery();

    @Name("Добавить в избранное панели навигации")
    @FindBy(".//div[@class='OfferCardNavBar__controls']//button[contains(@class,'ItemAddToFavorite')]")
    AtlasWebElement addToFavNavBar();

    @Name("Список похожих офферов")
    @FindBy(".//div[contains(@class,'OfferCardSimilarOffersList')]//li[contains(@class, 'SerpListItem_type_offer')]")
    ElementsCollection<SimilarTouchOffer> similarOffers();

    @Name("Кнопка «Показать ещё» в списке похожих офферов")
    @FindBy(".//div[contains(@class,'OfferCardSimilarOffersList')]//button[contains(.,'Показать ещё')]")
    AtlasWebElement similarOfferShowMore();

    @Name("Список офферов из истотрии")
    @FindBy(".//div[contains(@class,'OfferCardArchiveOffersList')]//li[contains(@class, 'SerpListItem_type_offer')]")
    ElementsCollection<AtlasWebElement> historyOffers();

    @Name("Кнопка «Показать ещё» в списке офферов из истории")
    @FindBy(".//div[contains(@class,'OfferCardArchiveOffersList')]//a[contains(.,'Показать еще')]")
    AtlasWebElement historyOfferShowMore();

    @Name("Список ближайших метро")
    @FindBy(".//div[contains(@class,'CardMetroList__item')]")
    ElementsCollection<AtlasWebElement> nearMetros();

    @Name("Кнопка ещё в списке ближайших метро")
    @FindBy(".//div[@class='CardMetroList']//span[contains(.,'ещё')]")
    AtlasWebElement nearMetrosShowMore();

    @Name("Шорткат {{ value }}")
    @FindBy(".//nav[@class='CardShortcuts']//li[contains(.,'{{ value }}')]")
    AtlasWebElement shortcut(@Param("value") String value);

    @Name("Карточка тепловой карты {{ value }}")
    @FindBy(".//div[@class='SwipeableSlider__item'][contains(.,'{{ value }}')]")
    AtlasWebElement cardHeatMap(@Param("value") String value);

    @Name("Шорткат на тепловой карте {{ value }}")
    @FindBy("//div[contains(@class,'Modal_visible')]//nav[contains(@class,'CardBrowser__shortcuts')]//li[contains(.,'{{ value }}')]")
    AtlasWebElement heatMapShortcut(@Param("value") String value);

    @Name("Модуль пожаловаться")
    @FindBy("//div[contains(@class, 'Modal_visible OfferComplain__modal')]")
    ComplainBlock complainModal();

    @Name("Текст описания")
    @FindBy(".//p[contains(@class,'OfferCardDescription__text')]")
    AtlasWebElement descriptionText();

    @Name("Секция {{ value }}")
    @FindBy("//div[contains(@class,'CardSection_expandable') and contains(.,'{{ value }}')]")
    CardSectionExpandable cardSection(@Param("value") String value);

    @Name("Открытый попап с тепловыми картотчками")
    @FindBy("//div[contains(@class,'OfferCard__browser')]")
    MobilePopup popupVisible();

    @Name("Видимый модулль")
    @FindBy("//div[contains(@class,'Modal_visible')]")
    Button modalVisible();

    @Name("Локация оффера")
    @FindBy("//div[@class='OfferCardLocation__newbuilding']")
    Link location();

    @Name("Информация о застройщике")
    @FindBy("//div[@class = 'CardDevInfo']")
    Link devInfo();

    @Name("Блок ЕГРН отчетов")
    @FindBy("//div[@id='egrn-report-free']")
    EgrnBlock egrnBlock();

    @Name("Купленные отчеты")
    @FindBy(".//a[contains(@class,'OfferCardEGRNReportPurchasedTile__container')]")
    ElementsCollection<Link> purchasedReports();

    default SimilarTouchOffer firstSimilarOffer() {
        return similarOffers().waitUntil(hasSize(greaterThan(0))).get(0);
    }
}
