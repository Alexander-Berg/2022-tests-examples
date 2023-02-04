package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.samolet.SamoletGeoSelectorPopup;
import ru.yandex.realty.element.samolet.SpecProjectDeliveryDatePopup;
import ru.yandex.realty.element.samolet.FiltersBlock;
import ru.yandex.realty.element.samolet.SamoletOffer;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface SamoletPage extends WebPage, Button {

    @Name("Список офферов")
    @FindBy("//div[@data-test='SiteSnippetSearch']")
    ElementsCollection<SamoletOffer> offerList();

    @Name("Блок фильтров")
    @FindBy("//div[contains(@class, 'SamoletFilters__container')]")
    FiltersBlock searchFilters();

    @Name("Открытый попап")
    @FindBy("//div[contains(@class, 'visible') and contains(@class, 'Popup')]")
    SpecProjectDeliveryDatePopup popupWithItem();

    @Name("Список офферов на карте")
    @FindBy("//ymaps[contains(@class, 'placemark')]//a")
    ElementsCollection<AtlasWebElement> mapOfferList();

    @Name("Попап оффера на карте")
    @FindBy("//div[contains(@class,'SamoletMap__snippetWrapper')]")
    Link mapOfferPopup();

    @Name("Вся страница")
    @FindBy("//body")
    AtlasWebElement pageBody();

    default SamoletOffer offer(int i) {
        return offerList().waitUntil(hasSize(greaterThan(i))).get(i);
    }

    default AtlasWebElement mapOffer(int i) {
        return mapOfferList().waitUntil(hasSize(greaterThan(i))).get(i);
    }

    @Name("Геоселектор")
    @FindBy("//div[contains(@class,'SamoletMenu__geoSelector')]")
    AtlasWebElement geoSelectorButton();

    @Name("Попап геоселектора")
    @FindBy(".//div[contains(@class,'SamoletMenu__geoSelectorPopup')]")
    SamoletGeoSelectorPopup geoSelectorPopup();
}
