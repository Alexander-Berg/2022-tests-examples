package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.Shortcuts;
import ru.yandex.realty.element.ShowPhonePopup;
import ru.yandex.realty.element.newbuilding.CallbackPopup;
import ru.yandex.realty.element.offercard.DescriptionBlock;
import ru.yandex.realty.element.offercard.EgrnBlock;
import ru.yandex.realty.element.offercard.FSGalleryBlock;
import ru.yandex.realty.element.offercard.HideableBlock;
import ru.yandex.realty.element.offercard.MorePopup;
import ru.yandex.realty.element.offercard.OfferCardSummary;
import ru.yandex.realty.element.offercard.SimilarOffer;
import ru.yandex.realty.element.offers.ServicePayment;
import ru.yandex.realty.element.popups.NotePopup;
import ru.yandex.realty.element.saleads.InputField;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * Created by kopitsa on 28.07.17.
 */
public interface OfferCardPage extends BasePage, InputField, Button, Link, Shortcuts {

    String SHOW_PHONE = "Показать телефон";

    @Name("Вся карточка")
    @FindBy(".//div[contains(@class,'OfferCard__card--')]")
    AtlasWebElement allOfferCard();

    @Name("Пожаловаться на объявление")
    @FindBy("//div[@class='ComplainAction']//button")
    AtlasWebElement complainButton();

    @Name("Добавить заметку")
    @FindBy("//div[@class='NoteAction']//button")
    AtlasWebElement addNoteButton();

    @Name("Добавить в избранное")
    @FindBy("//div[@class='FavoriteAction']//button")
    AtlasWebElement addToFavButton();

    @Name("Микрокарточка")
    @FindBy("//div[contains(@class,'OfferCard__offerCardSummary')]")
    OfferCardSummary offerCardSummary();

    @Name("Попап доп действий при нажатии из микрокарточки")
    @FindBy(".//div[contains(@class,'Popup_visible Popup_direction_left-top')]")
    MorePopup topMorePopup();

    @Name("Попап доп действий при нажатии из плавающего блока")
    @FindBy(".//div[contains(@class,'Popup_visible Popup_direction_left-top')]")
    MorePopup hideableMorePopup();

    @Name("Плавающая карточка при 1200")
    @FindBy("//div[contains(@class,'OfferCard__bottomBlockContainer')]")
    HideableBlock hideableBlock();

    @Name("Блок описания")
    @FindBy("//div[contains(@class,'OfferCard__textDescription')]")
    DescriptionBlock descriptionBlock();

    @Name("Блок инфо об авторе")
    @FindBy("//div[@data-test='OfferCardAuthorInfo']")
    DescriptionBlock authorBlock();

    @Name("Галерея")
    @FindBy(".//div[contains(@class,'GalleryV2__gallery')]")
    AtlasWebElement galleryOpener();

    @Name("Открытая галерея")
    @FindBy(".//div[contains(@class,'FSGallery_view_dark')]")
    FSGalleryBlock fsGallery();

    @Name("Детали")
    @FindBy(".//div[contains(@class,'OfferCard__detailsFeatures')]")
    AtlasWebElement offerCardFeatures();

    @Name("Основные параметры")
    @FindBy(".//div[contains(@class,'OfferCardHighlights__root')]")
    AtlasWebElement offerCardHighlights();

    @Name("Блок расположение")
    @FindBy(".//div[@id = 'location']")
    AtlasWebElement offerCardLocation();

    @Name("Иконка «Телефон защищен»")
    @FindBy("//div[contains(@class, 'OfferCardOwnerPhone__redirectIcon')]")
    AtlasWebElement iconPhoneProtect();

    @Name("Оплата услуг")
    @FindBy("//div[@class = 'OfferVasServices__list']")
    ServicePayment servicePayment();

    @Name("Хлебная крошка")
    @FindBy("//div[contains(@class,'BreadcrumbsNew__container')]")
    AtlasWebElement breadCrumbs();

    @Name("Хлебная крошка «{{ value }}»")
    @FindBy("//li[contains(@class,'BreadcrumbsNew__item')]//a[contains(.,'{{ value }}')]")
    AtlasWebElement breadCrumb(@Param("value") String value);

    @Name("Попап заметки")
    @FindBy("//div[contains(@class,'Modal_visible OfferCardActions__noteModal')]")
    NotePopup notePopup();

    @Name("Попап «Позвоните мне»")
    @FindBy("//div[contains(@class,'Modal_visible BackCall__modal')]")
    CallbackPopup callbackPopup();

    @Name("Попап «Показать телефон»")
    @FindBy("//div[contains(@class,'Modal_visible PhoneModal')]")
    ShowPhonePopup showPhonePopup();

    @Name("Список похожих объявлений")
    @FindBy("//div[contains(@class,'OfferCard__relatedOffers')]//div[contains(@class,'OfferCardSliderSnippet__snippet')]")
    ElementsCollection<SimilarOffer> similarOffers();

    @Name("Блок ЕГРН")
    @FindBy("//div[@id='egrn-report']")
    EgrnBlock egrnBlock();

    @Name("Список планировок")
    @FindBy(".//a[contains(@class,'CardPlansOffersSerp__link')]")
    ElementsCollection<AtlasWebElement> cardPlansOffers();

    default SimilarOffer similarOffer(int i) {
        return similarOffers().should(hasSize(greaterThan(i))).get(i);
    }

    default void openGallery() {
        galleryOpener().click();
        fsGallery().closeButton().waitUntil(WrapsElementMatchers.isDisplayed());
    }
}
