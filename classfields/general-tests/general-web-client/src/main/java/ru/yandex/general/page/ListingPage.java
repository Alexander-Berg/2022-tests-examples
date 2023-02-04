package ru.yandex.general.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.AllFiltersPopup;
import ru.yandex.general.element.Chips;
import ru.yandex.general.element.Filters;
import ru.yandex.general.element.Link;
import ru.yandex.general.element.ListingFilter;
import ru.yandex.general.element.ListingSnippet;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

public interface ListingPage extends BasePage, Link {

    String FILTERS = "Фильтры";
    String CONDITION = "Состояние";
    String PRICE = "Цена";
    String FREE = "Даром";
    String ONLY_WITH_PHOTO = "Только с фото";
    String POST = "Разместить";
    String PRICE_FROM = "от";
    String PRICE_TO = "до";
    String APPLY = "Применить";
    String CANCEL = "Отмена";
    String RESET_ALL = "Сбросить все";
    String MORE_OFFERS_AUTORU = "Больше объявлений на Авто.ру";
    String MORE_OFFERS_REALTY = "Больше объявлений на Я.Недвижимости";
    String AUTO = "Авто.ру";
    String REALTY = "Недвижимость";
    String WORK = "Работа";
    String SEARCH_CATEGORY_AUTO = "Категория поиска: Авто";
    String SEARCH_CATEGORY_REALTY = "Категория поиска: Недвижимость";

    @Name("Список элементов саджеста")
    @FindBy("//div[contains(@class, 'MenuItem__menuItem')]")
    ElementsCollection<VertisElement> suggestList();

    @Name("Фильтр «{{ value }}»")
    @FindBy("//div[contains(@class, 'ListingFilter__controlItem')][contains(., '{{ value }}')]")
    ListingFilter filter(@Param("value") String value);

    @Name("Сниппеты")
    @FindBy("//div[@role = 'listItem' or @role = 'gridItem']")
    ElementsCollection<ListingSnippet> snippets();

    @Name("Сниппеты в карусели Авто.ру/Недвижимости")
    @FindBy("//div[contains(@class, 'WizardOffersCarousel__item')]")
    ElementsCollection<Link> wizardSnippets();

    @Name("Первый сниппет")
    @FindBy("//div[@role = 'listItem' or @role = 'gridItem'][contains(@style, 'top: 0px; left: 0px;')]")
    ListingSnippet firstSnippet();

    @Name("Блок фильтров")
    @FindBy("//div[contains(@class, 'OfferListingMain__filters')]")
    Filters filters();

    @Name("Попап «Все фильтры»")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    AllFiltersPopup allFiltersPopup();

    @Name("Кнопка созранения поиска в избранное")
    @FindBy(".//span[contains(@class,'OfferListingButtonSaveSearch__button')]")
    VertisElement saveSearch();

    @Name("Кнопка созранения поиска в избранное")
    @FindBy(".//span[contains(@class,'HeaderSaveSearchBlock__saveButton')]")
    VertisElement saveSearchFloatHeader();

    @Name("Поисковый запрос с опечаткой")
    @FindBy("//div[contains(@Class, '_changedField')]/span")
    VertisElement misspellText();

    @Name("Основные категории на Главной")
    @FindBy("//div[contains(@class, 'HomeMain__categories')]")
    Link homeMainCategories();

    @Name("Баннер «{{ value }}")
    @FindBy("//div[contains(@class, 'HomeVertisBanners__item')][contains(., '{{ value }}')]")
    VertisElement banner(@Param("value") String value);

    @Name("Сайдбар с рекламой")
    @FindBy("//aside[contains(@class, 'OfferListingContent__sidebar')]")
    VertisElement adsSidebar();

    @Name("Листинг списком")
    @FindBy("//div[contains(@class, 'VirtualGrid__container')][@role = 'list']")
    VertisElement listListing();

    @Name("Листинг плиткой")
    @FindBy("//div[contains(@class, 'VirtualGrid__container')][@role = 'grid']")
    VertisElement gridListing();

    @Name("Чипсина")
    @FindBy("//div[contains(@class, 'undoChip')]")
    Chips chips();

    @Name("Хэдер карусели выдачи по АвтоРу/Недвижки")
    @FindBy("//span[contains(@class, 'WizardOffersCarousel__heading')]")
    VertisElement wizardCarouselHeader();

    @Name("Чипсина «{{ value }}»")
    @FindBy("//div[contains(@class, 'undoChip')][contains(., '{{ value }}')]")
    Chips chips(@Param("value") String value);

    @Name("Уточняющая категория «{{ value }}»")
    @FindBy("//div[contains(@class, 'OfferListingCategoryItem__item')][contains(., '{{ value }}')]")
    VertisElement mainCategory(@Param("value") String value);

    @Name("Рекламный спиппет")
    @FindBy("//div[contains(@class, '_adSnippet')]")
    VertisElement adsSnippet();

    default void openExtFilter() {
        filter(FILTERS).click();
        waitSomething(1, TimeUnit.SECONDS);
    }

    default ListingSnippet snippetFirst() {
        snippets().waitUntil(hasSize(greaterThan(0)));
        return firstSnippet();
    }

    default ListingSnippet snippetSecond() {
        return snippets().waitUntil(hasSize(greaterThan(1))).get(1);
    }

}
