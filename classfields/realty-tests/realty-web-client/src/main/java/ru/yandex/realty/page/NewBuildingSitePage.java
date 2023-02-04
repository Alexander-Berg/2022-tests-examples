package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.Shortcuts;
import ru.yandex.realty.element.SubscriptionForm;
import ru.yandex.realty.element.newbuilding.CallbackPopup;
import ru.yandex.realty.element.newbuildingsite.ApartmentBlock;
import ru.yandex.realty.element.newbuildingsite.CardPhoneNb;
import ru.yandex.realty.element.newbuildingsite.DevSnippet;
import ru.yandex.realty.element.newbuildingsite.DeveloperChat;
import ru.yandex.realty.element.newbuildingsite.ExtendedFiltersBlock;
import ru.yandex.realty.element.newbuildingsite.MainFiltersBlock;
import ru.yandex.realty.element.newbuildingsite.ReviewBlock;
import ru.yandex.realty.element.newbuildingsite.SiteCardAbout;
import ru.yandex.realty.element.newbuildingsite.SitePlansModal;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.yandex.realty.element.newbuildingsite.AddEditReviewBlock.ADD_REVIEW;

public interface NewBuildingSitePage extends BasePage, Button, Shortcuts {

    int PXLS_TO_HIDEABLE_BLOCK = 1500;
    String CALL_ME = "Позвоните мне";
    String CALLBACK = "Обратный звонок";
    String SHOW_PHONE = "Показать телефон";

    @Name("Раскрыть планировки квартир")
    @FindBy("//div[@class = 'SitePlansGroupedList__apartments']")
    AtlasWebElement sitePlans();

    @Name("Кнопка «Выбрать из»")
    @FindBy("//span[contains(., 'Выбрать из')]")
    AtlasWebElement chooseFrom();

    @Name("Модуль планировок")
    @FindBy(".//div[contains(@class,'Modal_visible SitePlansModal')]")
    SitePlansModal sitePlansModal();

    @Name("Ссылка на жк")
    @FindBy("//h2[@class = 'SectionTitle BuildingInfoSiteHeader__title']/a")
    AtlasWebElement jkLink();

    @Name("Главный блок фильтров")
    @FindBy("//div[@class = 'CardFilters__main']")
    MainFiltersBlock mainFiltersBlock();

    @Name("Блок расширенных фильтров")
    @FindBy("//div[@class = 'CardFilters__extra']")
    ExtendedFiltersBlock extendedFiltersBlock();

    @Name("Нижний блок фильтров")
    @FindBy("//div[@class = 'CardFilters__bottom']")
    ExtendedFiltersBlock cardFiltersBottom();

    @Name("Категория квартиры {{ value }}")
    @FindBy("//tr[contains(@class,'CardOffersType')]")
    ElementsCollection<ApartmentBlock> apartmentCategory();

    @Name("Список квартир")
    @FindBy(".//tr[@class='CardOffersSerpItem']//a")
    ElementsCollection<AtlasWebElement> offerLinks();

    @Name("Блок отзывов")
    @FindBy("//div[@class = 'Reviews']")
    ReviewBlock reviewBlock();

    @Name("Попап для застройщика")
    @FindBy("//div[contains(@class, 'Reviews__dev-modal') and contains(@class, 'visible')]")
    Link toDeveloperPopup();

    @Name("ЖК от застройщика")
    @FindBy("//div[contains(@class,'CardDevSites__items')]//div[contains(@class,'SitesSerp__snippet')]")
    ElementsCollection<DevSnippet> fromDevList();

    default DevSnippet fromDev(int i) {
        return fromDevList().waitUntil(hasSize(greaterThan(i))).get(i);
    }

    @Name("Попап «Позвоните мне»")
    @FindBy("//div[contains(@class, 'visible') and contains(@class, 'BackCall__modal')]")
    CallbackPopup callbackPopup();

    @Name("Прокрутка")
    @FindBy("//div[contains(@class,'Spin_visible')]")
    AtlasWebElement spin();

    @Name("Карточка информации")
    @FindBy("//div[@class='SiteCardAbout']")
    SiteCardAbout siteCardAbout();

    @Name("Плавающая карточка")
    @FindBy("//div[contains(@class,'CardHideableBlock_visible')]")
    CardPhoneNb hideableBlock();

    @Name("Информация о застройщике")
    @FindBy("//div[@class = 'CardDev']")
    CardPhoneNb cardDev();

    @Name("Картинка галереи")
    @FindBy("//div[contains(@class,'GallerySlidePic')]")
    AtlasWebElement galleryPic();

    @Name("Блок информации сбоку в галерее")
    @FindBy("//div[contains(@class,'SiteCardGallery__aside')]")
    CardPhoneNb galleryAside();

    @Name("Закрыть галерею")
    @FindBy(".//button[contains(@class,'FSGalleryClose')]")
    AtlasWebElement closeGallery();

    @Name("Список похожих ЖК")
    @FindBy("//div[@class = 'CardSimilarSites__items']//div[contains(@class,'SitesSerp__snippet')]")
    ElementsCollection<AtlasWebElement> similarSites();

    @Name("Кнопка выбора типа тепловых карт")
    @FindBy("//div[contains(@class,'ymaps-control-button__content')]")
    AtlasWebElement heatMapButton();

    @Name("Фотографии с ходом строительства")
    @FindBy("//div[contains(@class, 'SiteCardProgress__gallery')]//div[contains(@class,'GalleryV2__slideWithSnapScroll')]")
    ElementsCollection<AtlasWebElement> progressPhotos();

    @Name("Форма «Подписка на скидки»")
    @FindBy("//form[contains(@class, 'NewbuildingSpecialSubscriptionForm')]")
    SubscriptionForm subscriptionForm();

    @Name("Блок «Ход строительства»")
    @FindBy("//div[contains(@id,'progress')]")
    AtlasWebElement progressBlock();

    @Name("Блок похожих «{{ value }}»")
    @FindBy("//div[contains(@class,'SwipeableBlock_size')][.//div[contains(.,'{{ value }}')]]")
    AtlasWebElement similarJkInSales(@Param("value") String value);

    @Name("Открытый чат")
    @FindBy("//div[contains(@class,'ChatApp_chat-opened')]")
    DeveloperChat developerChat();

    @Name("Ссылка на предложения на вторичке")
    @FindBy(".//div[contains(@class,'SitePlansSecondarySearch__secondarySearch')]")
    Link secondarySearchLink();

    default void addReview(String text) {
        reviewBlock().reviewArea().inputField().waitUntil(WebElementMatchers.isDisplayed())
                .sendKeys(text);
        reviewBlock().reviewArea().button(ADD_REVIEW).click();
        //Ждем записи отзыва
        waitSomething(5, SECONDS);
    }
}
