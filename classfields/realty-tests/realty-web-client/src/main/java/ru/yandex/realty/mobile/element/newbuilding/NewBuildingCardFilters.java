package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.element.saleads.SelectionBlock;
import ru.yandex.realty.mobile.element.Fieldset;

public interface NewBuildingCardFilters extends Button, SelectionBlock, Fieldset, InputField {

    String AREA_TO_ID = "area-range_to";
    String AREA_FROM_ID = "area-range_from";
    String FLOR_FROM_ID="floor-range_from";
    String FLOR_TO_ID="floor-range_to";
    String KITCHENSPACE_FROM_ID="kitchenSpace-range_from";
    String KITCHENSPACE_TO_ID="kitchenSpace-range_to";

    @Name("Срок сдачи - {{ value }}")
    @FindBy("//select/option[contains(.,'{{ value }}')]")
    AtlasWebElement option(@Param("value") String value);

    @Name("Список - «{{ value }}»")
    @FindBy("//div[contains(@class,'SitePlansOffers__presetPriceRow')][contains(.,'{{ value }}')]")
    AtlasWebElement offersGroup(@Param("value") String value);

    @Name("Site plans snippet - список  планировок")
    @FindBy("//div[contains(@class,'CardPlansOffersSerp__offer')]")
    ElementsCollection<AtlasWebElement> offers();

    @Name("Card plans offer - список офферов в конкретной планировке")
    @FindBy("//div[contains(@class,'CardPlansOffersSerp__offer')]")
    ElementsCollection<AtlasWebElement> cardPlanOffer();

}

