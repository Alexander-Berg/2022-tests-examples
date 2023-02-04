package ru.yandex.general.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.mobile.element.Chips;
import ru.yandex.general.mobile.element.FiltersPopup;
import ru.yandex.general.mobile.element.Button;
import ru.yandex.general.mobile.element.Link;
import ru.yandex.general.mobile.element.ListingFilter;
import ru.yandex.general.mobile.element.ListingSnippet;
import ru.yandex.general.mobile.element.Map;
import ru.yandex.general.mobile.element.Screen;
import ru.yandex.general.mobile.element.SearchBar;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface ListingPage extends BasePage, Button, Link {

    String SUBWAY = "Метро";
    String DISTRICT = "Район";
    String ALL_FILTERS = "Все фильтры";
    String CONDITION = "Состояние";
    String SHOW_BUTTON = "Показать";
    String FIND_OFFERS = "Найти объявления";
    String DONE = "Готово";
    String CANCEL = "Отмена";
    String PRICE_FROM = "от";
    String PRICE_TO = "до";
    String PRICE = "Цена";
    String SAVE_SEARCH = "Сохранить поиск";
    String ONLY_WITH_PHOTO = "Только с фото";
    String PARAMETERS = "Параметры";
    String FREE = "Даром";
    String MORE_OFFERS_AUTORU = "Больше объявлений на Авто.ру";
    String MORE_OFFERS_REALTY = "Больше объявлений на Я.Недвижимости";
    String UNDO = "Отменить";
    String AUTO = "Авто.ру";
    String REALTY = "Недвижимость";
    String WORK = "Работа";
    String AUTO_CHIPS = "Авто";
    String REALTY_CHIPS = "Недвижимость";

    @Name("Карта")
    @FindBy("//div[contains(@class, 'SearchSuggestMapScreen__content')]")
    Map map();

    @Name("Сёрч-бар")
    @FindBy("//div[contains(@class, 'searchBar')]")
    SearchBar searchBar();

    @Name("Прилипший сёрч-бар")
    @FindBy("//div[contains(@class, 'searchBarSticky')]")
    SearchBar stickySearchBar();

    @Name("Экран саджеста")
    @FindBy("//div[contains(@class, 'SuggestScreen__root')]")
    VertisElement suggestScreen();

    @Name("Экран саджеста адреса")
    @FindBy("//div[contains(@class, 'Screen__baseContent')][./div[contains(@class, 'SearchSuggestToponyms')]]")
    Screen addressSuggestScreen();

    @Name("Сортировка")
    @FindBy("//span[contains(@class, 'sortButton')]")
    VertisElement sortButton();

    @Name("Фильтр «{{ value }}»")
    @FindBy("//div[contains(@class, 'ListingFilter__item')][contains(., '{{ value }}')]")
    ListingFilter filter(@Param("value") String value);

    @Name("Список категорий")
    @FindBy("//div[contains(@class, 'OfferListingCategories')]")
    Link categories();

    @Name("Баннер «{{ value }}")
    @FindBy("//div[contains(@class, 'HomeVertisBanners__item')][contains(., '{{ value }}')]")
    VertisElement banner(@Param("value") String value);

    @Name("Категория на главной «{{ value }}»")
    @FindBy("//div[contains(@class, 'CategoriesListView')][contains(., '{{ value }}')]")
    VertisElement homeCategory(@Param("value") String value);

    @Name("Сниппет")
    @FindBy("//div[@role = 'listItem' or @role = 'gridItem']")
    ElementsCollection<ListingSnippet> snippets();

    @Name("Первый сниппет")
    @FindBy("//div[@role = 'listItem' or @role = 'gridItem'][contains(@style, 'top: 0px; left: 0px;')]")
    ListingSnippet firstSnippet();

    @Name("Кнопка сохранения поиска в избранное")
    @FindBy(".//span[contains(@class,'OfferListingControls__saveButton')]")
    VertisElement saveSearch();

    @Name("Кнопка сохранения поиска в избранное")
    @FindBy(".//div[contains(@class, 'SaveSearchButton')]")
    VertisElement saveSearchFloatHeader();

    @Name("Поисковый запрос с опечаткой")
    @FindBy("//span[contains(@class, '_changedField')]/span")
    VertisElement misspellText();

    @Name("Листинг списком")
    @FindBy("//div[contains(@class, 'VirtualGrid__container')][@role = 'list']")
    VertisElement listListing();

    @Name("Листинг плиткой")
    @FindBy("//div[contains(@class, 'VirtualGrid__container')][@role = 'grid']")
    VertisElement gridListing();

    @Name("Чипсина")
    @FindBy("//div[contains(@class, 'ListingUndoChip')]")
    Chips chips();

    @Name("Чипсина «{{ value }}»")
    @FindBy("//div[contains(@class, 'ListingUndoChip')][contains(., '{{ value }}')]")
    Chips chips(@Param("value") String value);

    @Name("Чипсина с выбранной категорией")
    @FindBy("//span[contains(@class, 'ListingCategoryItem__item')]")
    Chips categoryInChips();

    @Name("Рекламный спиппет")
    @FindBy("//div[contains(@class, '_adSnippet')]")
    VertisElement adsSnippet();

    @Name("Сниппеты в карусели Авто.ру/Недвижимости")
    @FindBy("//div[contains(@class, 'WizardOffersCarousel__item')]")
    ElementsCollection<ru.yandex.general.element.Link> wizardSnippets();

    @Name("Хэдер карусели выдачи по АвтоРу/Недвижки")
    @FindBy("//span[contains(@class, 'WizardOffersCarousel__heading')]")
    VertisElement wizardCarouselHeader();

    default FiltersPopup filters() {
        return wrapper(PARAMETERS);
    }

    default ListingSnippet snippetFirst() {
        snippets().waitUntil(hasSize(greaterThan(0)));
        return firstSnippet();
    }

    default ListingSnippet snippetSecond() {
        return snippets().waitUntil(hasSize(greaterThan(1))).get(1);
    }
}
