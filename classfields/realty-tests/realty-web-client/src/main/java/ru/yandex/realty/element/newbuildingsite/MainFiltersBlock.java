package ru.yandex.realty.element.newbuildingsite;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.saleads.InputField;
import ru.yandex.realty.element.saleads.SelectButton;

/**
 * Created by kopitsa on 10.07.17.
 */
public interface MainFiltersBlock extends SelectButton {

    String FROM = "от";
    String TO = "до";
    String COMMISSIONING_DATE = "Срок сдачи";
    String NUMBER_OF_ROOMS = "Количество комнат";

    @Name("Фильтр цены")
    @FindBy(".//div[contains(@class, 'FiltersFormField_name_price-with-type')]")
    InputField priceFilter();

    @Name("Фильтр площади")
    @FindBy(".//div[contains(@class, 'FiltersFormField_name_area')]")
    InputField areaFilter();
}
