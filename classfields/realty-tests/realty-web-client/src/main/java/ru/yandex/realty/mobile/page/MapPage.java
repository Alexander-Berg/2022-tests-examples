package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.mobile.element.Link;
import ru.yandex.realty.mobile.element.map.MapOffer;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public interface MapPage extends BasePage, Link {

    String NO_LAYER = "Без слоёв";

    @Name("Список офферов на карте сбоку")
    @FindBy(".//div[contains(@class,'SerpSlider__item')]")
    ElementsCollection<MapOffer> mapOffersList();

    @Name("Список офферов на карте сбоку")
    @FindBy(".//div[contains(@class,'SiteMapSnippet__container')]")
    ElementsCollection<MapOffer> mapNewbuildingOffersList();

    @Name("Пины на карте")
    @FindBy(".//a[contains(@class,'MapPlacemarkSerp')]")
    ElementsCollection<AtlasWebElement> pinsList();

    @Name("Паранжа подсказки")
    @FindBy(".//div[@class='MapCommuteWizard__content']")
    RealtyElement paranja();

    @Name("Тайтл легенды")
    @FindBy("//span[@class = 'HeatMapLegend__body-title']")
    RealtyElement legendTitle();

    @Name("Тайтл легенды карты школ")
    @FindBy("//span[@class = 'SchoolLegend__title']")
    RealtyElement schoolLegendTitle();

    @Name("Слои")
    @FindBy("//*[contains(@class, 'layers')]")
    RealtyElement layers();

    @Name("Скрыть пины")
    @FindBy("//i[contains(@class, 'no-pins')]")
    RealtyElement hidePins();

    @Name("Тепловая карта")
    @FindBy("//ymaps[contains(@style, 'heatmap')]")
    RealtyElement heatmap();

    default MapOffer offer(int i) {
        return mapOffersList().should(hasSize(greaterThan(i))).get(i);
    }

    default AtlasWebElement pin(int i) {
        return pinsList().should(hasSize(greaterThan(i))).get(i);
    }

    default MapOffer newBuildingMapOffer(int i) {
        return mapNewbuildingOffersList().should(hasSize(greaterThan(i))).get(i);
    }

}
