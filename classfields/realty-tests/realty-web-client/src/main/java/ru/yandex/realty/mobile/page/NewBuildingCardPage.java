package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.mobile.element.CardDev;
import ru.yandex.realty.mobile.element.CardSectionExpandable;
import ru.yandex.realty.mobile.element.MobilePopup;
import ru.yandex.realty.mobile.element.SimilarSitesTouchOffer;
import ru.yandex.realty.mobile.element.newbuilding.CallbackPopup;
import ru.yandex.realty.mobile.element.newbuilding.CardStickyActions;
import ru.yandex.realty.mobile.element.newbuilding.FavPopup;
import ru.yandex.realty.mobile.element.newbuilding.FeaturesBlock;
import ru.yandex.realty.mobile.element.newbuilding.NewBuildingCardFilters;
import ru.yandex.realty.mobile.element.newbuilding.NewBuildingCardPlans;
import ru.yandex.realty.mobile.element.newbuilding.NewBuildingGallery;
import ru.yandex.realty.mobile.element.newbuilding.NewbuildingCardDocuments;
import ru.yandex.realty.mobile.element.newbuilding.NewbuildingCardMortgages;
import ru.yandex.realty.mobile.element.newbuilding.NewbuildingCardProposals;
import ru.yandex.realty.mobile.element.newbuilding.ReviewsBlock;
import ru.yandex.realty.mobile.element.newbuilding.SubscribeBlock;
import ru.yandex.realty.mobile.element.newbuilding.SurveyForm;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.realty.step.CommonSteps.FIRST;

public interface NewBuildingCardPage extends BasePage, Button, InputField, Link {

    String CALLBACK = "Перезвоните мне";
    String CALL = "Позвонить";
    String CALL_ME = "Позвоните мне";
    String SHOW_ALL = "Показать все объекты";
    String PRICE = "Цена";
    String AREA = "Общая площадь";
    String KITCHEN_SPACE = "Площадь кухни";
    String FLOOR = "Этаж";
    String FROM = "от";
    String TO = "до";
    String SHOW = "Показать";
    String PER_METER = "м²";
    String DELIVERY_DATE = "Срок сдачи и корпус";
    String HOUSE_ID = "houseId";
    String ACCEPT = "Выбрать";

    @Name("Попап «Позвоните мне»")
    @FindBy("//div[contains(@class,'Modal_visible BackCall__modal')]")
    CallbackPopup callbackPopup();

    @Name("Плавающий блок «Позвонить»")
    @FindBy("//div[@class='CardPhone']")
    CardStickyActions stickyActions();

    @Name("Фильтры")
    @FindBy("//div[contains(@data-anchor,'flats')]")
    NewBuildingCardFilters filters();

    @Name("Фильтры")
    @FindBy("//div[contains(@class,'SitePlansFiltersModal__modal')]")
    NewBuildingCardFilters extFilters();

    @Name("Попап")
    @FindBy("//div[contains(@class,'Modal_visible') or contains (@class,'Popup_visible')]")
    MobilePopup popupVisible();

    @Name("Форма «Узнать о новых объявлениях»")
    @FindBy("//div[contains(@class,'Modal_visible')]//form")
    SurveyForm surveyForm();

    @Name("Форма подписки")
    @FindBy("//form[contains(@class,'NewbuildingSpecialSubscriptionForm__container')]")
    SubscribeBlock subscribeBlock();

    @Name("Блок «обратного звонка»")
    @FindBy("//div[contains(@class,'NewbuildingCardContent__callbackForm')]")
    CardDev newbuildingCallbackForm();

    @Name("Список карточек похожих новостроек")
    @FindBy("//div[contains(@class,'NewbuildingCardSimilarSitesList')]//li")
    ElementsCollection<SimilarSitesTouchOffer> similarSitesList();

    @Name("Список карточек от застройщика")
    @FindBy("//div[contains(@class,'NewbuildingCardSitesFromDeveloper')]//li")
    ElementsCollection<SimilarSitesTouchOffer> fromDeveloperSitesList();

    @Name("Блок списка особенностей")
    @FindBy("//div[contains(@class,'NewbuildingCardDescription__features')]")
    FeaturesBlock featuresBlock();

    @Name("Блок Описание")
    @FindBy("//div[contains(@class,'NewbuildingCardDescription__description')]")
    Link description();

    @Name("Блок списка ипотек")
    @FindBy("//div[contains(@class,'NewbuildingCardContent__mortgages')]")
    NewbuildingCardMortgages newbuildingCardMortgagesBlock();

    @Name("Блок документов")
    @FindBy("//div[contains(@class,'NewbuildingCardDocuments')]")
    NewbuildingCardDocuments newbuildingCardDocuments();

    @Name("Блок акций")
    @FindBy("//div[@class='NewbuildingCardProposals']")
    NewbuildingCardProposals newbuildingCardProposals();

    @Name("Секция {{ value }}")
    @FindBy("//div[contains(@class,'NewbuildingCardAccordion__title') and contains(.,'{{ value }}')]")
    CardSectionExpandable cardSection(@Param("value") String value);

    @Name("Навигационный шоркат {{ value }}")
    @FindBy(".//div[contains(@class, 'NewbuildingCardFirstScreen__navigationItem') and contains(., '{{ value }}')]")
    AtlasWebElement navbarShortcut(@Param("value") String value);

    @Name("Фотографии")
    @FindBy("//div[@class='SwipeGallery__thumb']//img")
    ElementsCollection<AtlasWebElement> photo();

    @Name("Галерея")
    @FindBy("//div[@class='SwipeableFSGallery']")
    NewBuildingGallery gallery();

    @Name("Шорткат {{ value }}")
    @FindBy(".//nav[@class='CardShortcuts']//li[contains(.,'{{ value }}')]")
    AtlasWebElement shortcut(@Param("value") String value);

    @Name("Слайдер тепловых карт")
    @FindBy("//div[contains(@class,'NewbuildingCardHeatmaps')]")
    AtlasWebElement slider();

    @Name("Сниппет тепловой карты {{ value }}")
    @FindBy(".//div[contains(@class,'HeatmapCard_')][contains(.,'{{ value }}')]")
    AtlasWebElement heatMapSnippet(@Param("value") String value);

    @Name("Слайдер сниппетов тепловых карт")
    @FindBy(".//div[@class = 'SwipeableSlider CardHeatmaps NewbuildingCardHeatmaps']/div")
    AtlasWebElement heatMapSlider();

    @Name("Шорткат на тепловой карте {{ value }}")
    @FindBy("//div[contains(@class,'Modal_visible')]//nav[contains(@class,'CardBrowser__shortcuts')]//li[contains(.,'{{ value }}')]")
    AtlasWebElement heatMapShortcut(@Param("value") String value);

    @Name("Аккордеон {{ value }}")
    @FindBy("//div[contains(@class,'NewbuildingCardContent__accordion')]//div[contains(.,'{{ value }}')]")
    AtlasWebElement accordionBlock(@Param("value") String value);

    @Name("Сердечко избранного в хедере")
    @FindBy("//div[contains(@class, 'NavBar')]//div[contains(@class, 'SerpFavoriteAction')]")
    RealtyElement headerFavIcon();

    @Name("Попап избранного")
    @FindBy("//div[contains(@class,'NewbuildingSubscriptionModal__modal')][contains(@class,'Modal_visible')]")
    FavPopup favPopup();

    @Name("Блок отзывов")
    @FindBy("//div[contains(@class,'NewbuildingCardReviews')]")
    ReviewsBlock reviewsBlock();

    @Name("Список карточек застройщков")
    @FindBy("//div[@class='CardDevInfo']")
    ElementsCollection<Link> cardsDevInfo();

    @Name("Модуль планировок")
    @FindBy(".//div[contains(@class,'SitePlansV2__plans')]")
    NewBuildingCardPlans sitePlansModal();

    @Name("Ссылка на предложения на вторичке")
    @FindBy(".//div[contains(@class,'SiteSecondarySearch__secondarySearch')]")
    Link secondarySearchLink();

    @Name("Синяя кнопка звонка на сниппетах ЖК")
    @FindBy("//button[@data-test='PhoneButton']")
    AtlasWebElement bluePhoneButton();

    default void openExtFiltersV2() {
        sitePlansModal().button(ACCEPT).click();
        sitePlansModal().extFiltersV2().click();
    }

    default SimilarSitesTouchOffer fromDev(int i) {
        return fromDeveloperSitesList().waitUntil(hasSize(greaterThan(0))).get(FIRST);
    }
}
