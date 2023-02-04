package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.mobile.element.specproject.SpecProjDeliveryDatePopup;
import ru.yandex.realty.mobile.element.specproject.SpecProjMenu;
import ru.yandex.realty.mobile.element.specproject.SpecProjOffer;
import ru.yandex.realty.mobile.element.specproject.SpecProjectFiltersBlock;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface SpecProjectPage extends BasePage {

    @Name("Меню Спецпроекта")
    @FindBy("//div[contains(@class,'SamoletMenu__container')]/div[contains(@class,'SamoletMenu__dropdown')]")
    SpecProjMenu specProjMenu();

    @Name("Меню Спецпроекта")
    @FindBy("//div[contains(@class,'SamoletMenu__menuBottom')]")
    SpecProjMenu specProjBottom();

    @Name("Кнопка выбора фильтров")
    @FindBy("//button[contains(@class, 'SamoletCatalog__paramsButton')][contains(.,'Параметры')]")
    RealtyElement showFiltersButton();

    @Name("Блок фильтров спецпроекта")
    @FindBy("//div[contains(@class, 'SamoletFilters__filtersContent')]")
    SpecProjectFiltersBlock filters();

    @Name("Список офферов")
    @FindBy("//div[contains(@class,'SamoletCatalog__serpItem')]")
    ElementsCollection<SpecProjOffer> offerList();

    @Name("Открытый попап")
    @FindBy("//div[contains(@class, 'visible') and contains(@class, 'Popup')]")
    SpecProjDeliveryDatePopup popupWithItem();

    @Name("Кнопка «Списком» на карте")
    @FindBy(".//div[contains(@class,'SamoletMap__link')][.='Списком']")
    AtlasWebElement onList();

    @Name("Пины на карте")
    @FindBy(".//a[contains(@class,'MapPlacemarkSerp')]")
    ElementsCollection<AtlasWebElement> mapPins();

    @Name("Попап оффера на карте")
    @FindBy(".//div[contains(@class,'SamoletMap__snippet')]")
    AtlasWebElement mapOfferPopup();

    default SpecProjOffer offer(int i) {
        return offerList().waitUntil(hasSize(greaterThan(i))).get(i);
    }
}
