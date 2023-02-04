package ru.auto.tests.desktop.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithAuthPopup;
import ru.auto.tests.desktop.component.WithBestOfferPopup;
import ru.auto.tests.desktop.component.WithBreadcrumbs;
import ru.auto.tests.desktop.component.WithCardContacts;
import ru.auto.tests.desktop.component.WithContactsPopup;
import ru.auto.tests.desktop.component.WithCredit;
import ru.auto.tests.desktop.component.WithCrossLinksBlock;
import ru.auto.tests.desktop.component.WithListingFilter;
import ru.auto.tests.desktop.component.WithListingSortBar;
import ru.auto.tests.desktop.component.WithMag;
import ru.auto.tests.desktop.component.WithPager;
import ru.auto.tests.desktop.component.WithReviews;
import ru.auto.tests.desktop.component.WithSalesList;
import ru.auto.tests.desktop.element.ConfigurationsList;
import ru.auto.tests.desktop.element.HorizontalCarousel;
import ru.auto.tests.desktop.element.Popup;
import ru.auto.tests.desktop.element.card.PricePopup;
import ru.auto.tests.desktop.element.listing.CreditFilterPopup;
import ru.auto.tests.desktop.element.listing.FilterReset;
import ru.auto.tests.desktop.element.listing.GeoRadiusCounters;
import ru.auto.tests.desktop.element.listing.InfiniteListing;
import ru.auto.tests.desktop.element.listing.ListingSubscription;
import ru.auto.tests.desktop.element.listing.PresetBar;
import ru.auto.tests.desktop.element.listing.SalesListItem;
import ru.auto.tests.desktop.element.listing.StickySaveSearchPanel;
import ru.auto.tests.desktop.element.listing.TagCarousel;
import ru.auto.tests.desktop.element.listing.TagsBlock;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;

public interface ListingPage extends BasePage,
        WithBreadcrumbs,
        WithListingSortBar,
        WithPager,
        WithListingFilter,
        WithSalesList,
        WithCardContacts,
        WithContactsPopup,
        WithCrossLinksBlock,
        WithMag,
        WithBestOfferPopup,
        WithAuthPopup,
        WithCredit,
        WithReviews {

    int SALES_PER_PAGE = 37;

    @Name("Список сгруппированных объявлений")
    @FindBy(".//div[@class = 'ListingItemGroup']")
    ElementsCollection<SalesListItem> groupSalesList();

    @Step("Получаем сгруппированное объявление с индексом {i}")
    default SalesListItem getGroupSale(int i) {
        return groupSalesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Список объявлений с услугой ТОП")
    @FindBy("//div[@class = 'ListingItem' and .//*[contains(@class, 'vas-icon-top')]]")
    ElementsCollection<SalesListItem> topSalesList();

    @Step("Получаем ТОП-объявление с индексом {i}")
    default SalesListItem getTopSale(int i) {
        return topSalesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Список объявлений с услугой «Поднятие в поиске»")
    @FindBy("//div[@class = 'ListingItem' and .//*[contains(@class, 'vas-icon-fresh')]]")
    ElementsCollection<SalesListItem> freshSalesList();

    @Step("Получаем поднятое объявление с индексом {i}")
    default SalesListItem getFreshSale(int i) {
        return freshSalesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Список объявлений без услуги Премиум")
    @FindBy("//div[@class = 'ListingItem' and not(contains(., 'Показать телефон'))]")
    ElementsCollection<SalesListItem> nonPremiumSalesList();

    @Step("Получаем объявление без услуги Премиум с индексом {i}")
    default SalesListItem getNonPremiumSale(int i) {
        return nonPremiumSalesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Блок «Получайте новые объявления по вашим параметрам»")
    @FindBy("//div[contains(@class, 'ListingPromoSubscription')]")
    ListingSubscription listingSubscription();

    @Name("Новые автомобили в наличии у официальных дилеров")
    @FindBy(".//div[contains(@class, 'CarouselUniversal_dir_horizontal') " +
            "and .//div[contains(., 'Новые автомобили в наличии у официальных дилеров')]]")
    HorizontalCarousel salesFromDealers();

    @Name("Плашка пресета")
    @FindBy("//div[contains(@class, 'ListingCarsPresetClear')]")
    PresetBar presetBar();

    @Name("Блок «Автомобили этого класса»")
    @FindBy("//div[contains(@class, 'CarouselLazyOffers')][contains(., 'Автомобили того же класса')] | " +
            ".//div[contains(@class, 'CarouselUniversal_dir_horizontal')][contains(., 'Автомобили того же класса')]")
    HorizontalCarousel sameClassSales();

    @Name("Список популярных марок/моделей")
    @FindBy(".//a[contains(@class, 'ListingPopularMMM__itemName')]")
    ElementsCollection<VertisElement> popularMarksOrModelsList();

    @Step("Получаем популярную марку/модель с индексом {i}")
    default VertisElement getPopularMarkOrModel(int i) {
        return popularMarksOrModelsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Популярная марка/модель «{{ text }}»")
    @FindBy(".//a[contains(@class, 'ListingPopularMMM__itemName') and .= '{{ text }}']")
    VertisElement popularMarkOrModel(@Param("text") String Text);

    @Name("Диапазоны")
    @FindBy("//div[contains(@class, 'ListingSeoTagCarousel')]")
    TagCarousel tagCarousel();

    @Name("Залипающая панель «Сохранить поиск»")
    @FindBy("//div[@class = 'ListingSearchSave__body']")
    StickySaveSearchPanel stickySaveSearchPanel();

    @Name("Блок «Сброс фильтров»")
    @FindBy("//div[contains(@class, 'ListingResetFiltersSuggest_desktop')]")
    FilterReset filterResetBlock();

    @Name("Свежие б/у в листинге новых")
    @FindBy(".//div[contains(@class, 'ListingCarouselNewestUsed')]")
    HorizontalCarousel newestUsed();

    @Name("Проданное объявление")
    @FindBy(".//div[@class = 'ListingItem' and .//div[contains(@class, 'ListingItemSoldInfo')]]")
    SalesListItem soldSale();

    @Name("Блок «Ещё в других городах» (бесконечный листинг)")
    @FindBy("//div[contains(@class, 'ListingInfiniteDesktop__container')]")
    InfiniteListing infiniteListing();

    @Name("Блок «Конфигурации»")
    @FindBy("//div[contains(@class, 'ListingAllConfigurations')] | " +
            "//div[contains(@class, 'ListingAllConfigurationsAmp')]")
    ConfigurationsList configurationsList();

    @Name("Гео-кольца")
    @FindBy("//div[contains(@class, 'ListingGeoRadiusCounters')]")
    GeoRadiusCounters geoRadiusCounters();

    @Name("Попап фильтров кредита")
    @FindBy("//div[contains(@class, 'CreditFilterDetailsDump')]")
    CreditFilterPopup creditFilterPopup();

    @Name("Баннер в заголовоке листинга")
    @FindBy("//a[contains(@class, 'SmallElectroBanner')]")
    VertisElement listingHeadBanner();

    @Name("Электро баннер в листинге")
    @FindBy("//div[contains(@class, '_electroBanner')]")
    VertisElement electroBanner();

    @Name("Блок перелинковки «Автомобили по параметрам»")
    @FindBy("//div[@class = 'TagsLinks']")
    TagsBlock tagsBlock();

    @Name("Попап цены")
    @FindBy("//div[contains(@class, 'Popup_visible')][.//div[contains(@class, 'pricePopup')]]")
    PricePopup pricePopup();

    @Name("Попап с выбором авто из гаража на обмен")
    @FindBy("//div[contains(@class, 'OfferPriceExchangeGaragePopup') and contains(@class, 'Popup_visible')]")
    Popup exchangeCarPopup();

    @Step("Ждём обновления листинга")
    default void waitForListingReload() {
        waitSomething(2, TimeUnit.SECONDS);
        //wrap().waitUntil(not(WebElementMatchers.isDisplayed()));
    }
}
