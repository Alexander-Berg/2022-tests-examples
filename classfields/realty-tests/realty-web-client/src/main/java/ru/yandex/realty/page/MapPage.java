package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.ShowPhonePopup;
import ru.yandex.realty.element.map.AddressPopup;
import ru.yandex.realty.element.map.CommutePanel;
import ru.yandex.realty.element.map.CommuteSuggest;
import ru.yandex.realty.element.map.Sidebar;
import ru.yandex.realty.element.map.WizardTip;
import ru.yandex.realty.element.saleads.WithApartmentFilters;
import ru.yandex.realty.element.saleads.WithShowMoreLink;
import ru.yandex.realty.element.saleads.popup.ExtendedFiltersPopup;
import ru.yandex.realty.element.saleads.popup.SubscriptionPopup;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

/**
 * Created by kopitsa on 17.07.17.
 */
public interface MapPage extends BasePage, WithApartmentFilters, WithShowMoreLink {

    String CHOOSE_LAYER = "Слой";
    String TRAVEL_TIME = "Время на дорогу";

    @Name("Блок расширенных фильтров")
    @FindBy("//div[contains(@class,'FiltersForm__extra')]")
    ExtendedFiltersPopup extendFilters();

    @Name("Список иконок офферов")
    @FindBy("//ymaps[contains(@class, 'placemark')]//a")
    ElementsCollection<AtlasWebElement> offerPieChartList();

    @Name("Попап подсказки")
    @FindBy(".//div[contains(@class, 'Tip__inner')]")
    WizardTip wizardTip();

    @Name("Кнопка перехода на листинг")
    @FindBy("//div[contains(@class,'ShowListFiltersFormControl')]//button")
    AtlasWebElement toListingButton();

    @Name("Боковая панель")
    @FindBy(".//div[contains(@class,'MapSidebarv3_tab_serp') or contains(@class,'SearchMapWithSerp__sidebar')]")
    Sidebar sidebar();

    @Name("Спрятать фильтры")
    @FindBy(".//div[@class='MapSidebarv3__hider']")
    AtlasWebElement sidebarHider();

    @Name("Боковая панель избранных офферов")
    @FindBy(".//div[contains(@class,'FavoritesMapSidebar__sidebar')]")
    Sidebar favoriteSidebar();

    @Name("Попап подписки на цену оффера при наведении на стрелочку")
    @FindBy("//div[contains(@class, 'Popup_visible')]")
    SubscriptionPopup priceSubscriptionPopup();

    @Name("Реклама при клике на показать телефон в новостройках")
    @FindBy("//div[contains(@class,'Popup_visible')]//div[contains(@class,'SnippetContacts__popup')]")
    AtlasWebElement adPopup();

    @Name("Попап «Позвоните мне»")
    @FindBy("//div[@class='Portal']//div[contains(@class,'Modal__content')]")
    AtlasWebElement callBackPopup();

    @Name("Попап подписок в новостройках при добавлении в избранное")
    @FindBy("//div[contains(@class,'NewbuildingSubscriptionModal__modal')]//div[@class='Modal__content']")
    AtlasWebElement subscriptionPopupNb();

    @Name("Попап «Показать телефон»")
    @FindBy("//div[contains(@class,'Modal_visible') and contains(@class, 'PhoneModal')]" +
            "//div[contains(@class, 'Modal__content')]")
    ShowPhonePopup showPhonePopup();

    @Name("Отдалить карту")
    @FindBy("//div[contains(@class,'unzoom')]")
    AtlasWebElement unzoom();

    @Name("Найти адрес")
    @FindBy("//i[contains(@class,'Icon_type_search')]")
    AtlasWebElement findAddressButton();

    @Name("Попап поиска по адресу")
    @FindBy("//div[contains(@class,'SearchMap__panel')]")
    AddressPopup addressPopup();

    @Name("Кнопка карты «{{ value }}»")
    @FindBy("//div[contains(.,'{{ value }}') and contains(@class,'ymaps-control-button')]")
    AtlasWebElement mapButton(@Param("value") String value);

    @Name("Кнопка удаления нарисованной области")
    @FindBy("//button[contains(@class,'MapButtonWithRemove__removeButton')]")
    AtlasWebElement removeDrawArea();

    @Name("Скрыть пины")
    @FindBy("//div[contains(@class,'ymaps-control-button')][.//i[contains(@class,'no-pins')]]")
    AtlasWebElement noPinsButton();

    @Name("Кнопка «Скрыть тепловую карту»")
    @FindBy("//div[@class='MapLayersSelect']//button[contains(@class,'MapLayersSelect__closer_visible')]")
    AtlasWebElement heatMapCloser();

    @Name("Кнопка выбора слоя тепловой карты «{{ value }}»")
    @FindBy("//div[contains(@class,'MapSelectItem MapSelectItem_view_grey')][contains(.,'{{ value }}')]")
    AtlasWebElement heatMapLayerPopup(@Param("value") String value);

    @Name("Тайтл легенды тепловой карты")
    @FindBy("//div[@class='MapHeatmapLegend__title']")
    AtlasWebElement heatMapLegendTitle();

    @Name("Иконка подсказки")
    @FindBy("//div[@class='MapHeatmapLegendHint']")
    AtlasWebElement heatMapHintIcon();

    @Name("Попап подсказки")
    @FindBy("//div[contains(@class,'Popup_visible')]//div[contains(@class,'MapHeatmapLegendHint__hint')]")
    Link heatMapHintPopup();

    @Name("Саджест «Время на дорогу»")
    @FindBy("//div[contains(@class,'Suggest_type_simple ') and contains(@class,'MapCommuteSuggest')]")
    CommuteSuggest commuteSuggest();

    @Name("Панель «Время на дорогу»")
    @FindBy("//div[contains(@class,'SearchMap__panel_type_commute-panel')]")
    CommutePanel commutePanel();

    @Name("Карта")
    @FindBy("//div[@class='Map Map_ready']")
    AtlasWebElement map();

    //пока такой элемент потому что то один то другой. Поменять если единый
    @Name("Рекламка внизу слева")
    @FindBy(".//div[@class='FiltersForm__ad' or contains(@class,'SitesSearchMapWithSerpPage__ad')]")
    AtlasWebElement mapAdCorner();

    default AtlasWebElement mapOffer(int i) {
        return offerPieChartList().waitUntil(hasSize(greaterThan(i))).get(i);
    }

    default void selectHeatMap(String layer) {
        mapButton(CHOOSE_LAYER).waitUntil(WebElementMatchers.isDisplayed(), 20).click();
        heatMapLayerPopup(layer).click();
        mapButton(layer).waitUntil(WebElementMatchers.isDisplayed());
    }

    @Name("Проверяем что есть хотя бы один пин с ценой")
    default void hasPinsWithCost() {
        offerPieChartList().waitUntil(hasItem(hasText(containsString("млн"))));
    }
}
