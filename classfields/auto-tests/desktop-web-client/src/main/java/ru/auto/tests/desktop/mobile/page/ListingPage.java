package ru.auto.tests.desktop.mobile.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCredit;
import ru.auto.tests.desktop.element.listing.TagsBlock;
import ru.auto.tests.desktop.mobile.component.WithCallbackPopup;
import ru.auto.tests.desktop.mobile.component.WithCrossLinksBlock;
import ru.auto.tests.desktop.mobile.component.WithFiltersPopup;
import ru.auto.tests.desktop.mobile.component.WithGeoPopup;
import ru.auto.tests.desktop.mobile.component.WithGeoRadiusPopup;
import ru.auto.tests.desktop.mobile.component.WithMmmPopup;
import ru.auto.tests.desktop.mobile.component.WithOptionsPopup;
import ru.auto.tests.desktop.mobile.component.WithParamsPopup;
import ru.auto.tests.desktop.mobile.component.WithReviews;
import ru.auto.tests.desktop.mobile.component.WithReviewsPlusMinusPopup;
import ru.auto.tests.desktop.mobile.component.WithSalesList;
import ru.auto.tests.desktop.mobile.component.WithSavedSearch;
import ru.auto.tests.desktop.mobile.component.WithSortBar;
import ru.auto.tests.desktop.mobile.component.WithVideos;
import ru.auto.tests.desktop.mobile.element.Filters;
import ru.auto.tests.desktop.mobile.element.SaleListItem;
import ru.auto.tests.desktop.mobile.element.cardpage.NotePopup;
import ru.auto.tests.desktop.mobile.element.listing.DailyOffers;
import ru.auto.tests.desktop.mobile.element.listing.FilterReset;
import ru.auto.tests.desktop.mobile.element.listing.GeoRadiusCounters;
import ru.auto.tests.desktop.mobile.element.listing.InfiniteListing;
import ru.auto.tests.desktop.mobile.element.listing.ListingHeader;
import ru.auto.tests.desktop.mobile.element.listing.NewestUsed;
import ru.auto.tests.desktop.mobile.element.listing.SpecialSales;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;

/**
 * Created by kopitsa on 15.09.17.
 */

public interface ListingPage extends BasePage, WithSortBar, WithVideos, WithSalesList, WithCrossLinksBlock,
        WithParamsPopup, WithMmmPopup, WithGeoPopup, WithGeoRadiusPopup, WithCallbackPopup, WithFiltersPopup,
        WithOptionsPopup, WithSavedSearch, WithCredit, WithReviews, WithReviewsPlusMinusPopup {

    int SALES_PER_PAGE = 19;
    int SALES_PER_PAGE_OLD = 37;
    int TOP_SALES_COUNT = 3;

    String IN_CREDIT = "В кредит";

    @Name("Заголовок")
    @FindBy("//div[@class = 'ListingHead'] | " +
            "//div[@class = 'commercial-listing__head'] | " +
            "//div[contains(@class, 'ListingAmpHead')]")
    ListingHeader listingHeader();

    @Name("Заглушка пустого листинга")
    @FindBy("//div[@class = 'ListingEmptyBanner']")
    VertisElement emptyStub();

    @Name("Фильтры")
    @FindBy("//div[@class = 'listing-filter' or @class = 'ListingFilter' or @class = 'ListingQuickFilters'] | " +
            "//div[@class = 'ListingAmpFilter'] | " +
            "//section[contains(@class, 'ListingHeadMobile')]")
    Filters filters();

    @Name("Кнопка «Показать ещё»")
    @FindBy("//a[contains(@class, 'amp-next-page-link')]")
    VertisElement showMoreButton();

    @Name("Список сгруппированных офферов")
    @FindBy("//div[@class = 'ListingItemBig' and .//div[contains(@class, 'ListingItemBig__groupInfo')]]")
    ElementsCollection<SaleListItem> groupSalesList();

    @Name("Список проданных объявлений")
    @FindBy(".//div[@class = 'ListingItemRegular' and .//div[contains(@class, 'ListingItemSoldInfo')]]")
    ElementsCollection<SaleListItem> soldSalesList();

    @Name("Список объявлений с услугой ТОП")
    @FindBy("//div[@class = 'ListingItem commercial-listing-premium-item'] | " +
            "//div[@class = 'ListingItemBig' and .//*[contains(@class, 'IconSvg_vas-icon-top')]]")
    ElementsCollection<SaleListItem> topSalesList();

    @Name("Кнопка «Предыдущие»")
    @FindBy("//div[contains(@class, 'index-presets-content__more_prev')] | " +
            "//button[contains(@class, 'PageListing__prevButton')]")
    VertisElement prevPageButton();

    @Name("Блок «Ещё в других городах» (бесконечный листинг)")
    @FindBy("//div[contains(@class, 'ListingInfiniteMobile__container')]")
    InfiniteListing infiniteListing();

    @Name("Блок «Сброс фильтров»")
    @FindBy("//div[contains(@class, 'ListingResetFiltersSuggest_mobile')]")
    FilterReset filterResetBlock();

    @Name("Блок спецпредложений")
    @FindBy("//div[contains(@class, 'WidgetSpecialOffers')]")
    SpecialSales specialSales();

    @Name("Поп-ап заметки")
    @FindBy("//div[contains(@class, 'Modal_visible')]//div[@class='Modal__content']")
    NotePopup notePopup();

    @Name("Предложения дня")
    @FindBy("//div[contains(@class, 'ListingPremiumOffers')]")
    DailyOffers dailyOffers();

    @Name("FAB")
    @FindBy("//div[contains(@class, 'ListingFab')]")
    VertisElement fab();

    @Name("FAB «Параметры»")
    @FindBy("//div[contains(@class, 'ListingFab__filters')]")
    VertisElement fabParams();

    @Name("FAB «Сохранение поиска»")
    @FindBy("//div[contains(@class, 'ListingFab__subscriptions')]")
    VertisElement fabSubscriptions();

    @Name("Свежие б/у в листинге новых")
    @FindBy(".//div[contains(@class, 'ListingCarouselNewestUsed')]")
    NewestUsed newestUsed();

    @Step("Получаем сгруппированное объявление с индексом {i}")
    default SaleListItem getGroupSale(int i) {
        return groupSalesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Гео-кольца")
    @FindBy("//div[contains(@class, 'ListingGeoRadiusCounters')]")
    GeoRadiusCounters geoRadiusCounters();

    @Name("Промо-баннер")
    @FindBy("//div[contains(@class, '_promoBanner')]")
    VertisElement promoBanner();

    @Name("Блок перелинковки «Автомобили по параметрам»")
    @FindBy("//div[contains(@class, 'TagsLinks_mobile')] | " +
            "//div[@class = 'AmpTagsLinks']")
    TagsBlock tagsBlock();

    @Step("Ждём обновления листинга")
    default void waitForListingReload() {
        waitSomething(2, TimeUnit.SECONDS);
    }
}
