package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.common.Link;
import ru.yandex.arenda.element.estimatecalculator.CallbackPopup;
import ru.yandex.arenda.element.estimatecalculator.PriceContainer;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CalculatorCostPage extends BasePage {

    String ADDRESS_ID = "ADDRESS";
    String ROOMS_ID = "ROOMS";
    String AREA_ID = "AREA";
    String FLOOR_ID = "FLOOR";

    String DESIGN_RENOVATION = "Дизайнерский";
    String EURO_RENOVATION = "Евро";
    String COSMETIC_RENOVATION = "Косметический";
    String GRANDMOM_RENOVATION = "Устаревший";
    String DONE_BUTTON = "Оценить";

    @Name("Чекбокс ремонта «{{ value }}»")
    @FindBy(".//div[contains(@class,'LandingCommonRenovationTypeRadioGroup__radioItem') and contains(.,'{{ value }}')]")
    AtlasWebElement renovationCheckbox(@Param("value") String value);

    @Name("Форма оцененной квартиры")
    @FindBy(".//div[contains(@class,'LandingCalculatorArendaRentPrice__priceContainer')]")
    PriceContainer priceContainer();

    @Name("Попап «Укажите номер телефона»")
    @FindBy(".//div[contains(@class,'LandingCalculatorArendaRentPricePhoneModal__popup')]")
    CallbackPopup callbackPopup();

    @Name("Ошибка «{{ value }}»")
    @FindBy(".//span[contains(@class, 'InputDescription__description') and contains(.,'{{ value }}')]")
    AtlasWebElement inputError(@Param("value") String value);

    @Name("Футер")
    @FindBy(".//div[contains(@class,'LandingCalculatorArenda__footer')]")
    Link footer();

    @Name("Элементы саджеста адреса")
    @FindBy(".//li[contains(@class,'SuggestList__item')]")
    ElementsCollection<AtlasWebElement> listItems();

    default void pickFirstFromSuggest() {
        listItems().waitUntil(hasSize(greaterThan(0))).get(0).click();
    }
}
