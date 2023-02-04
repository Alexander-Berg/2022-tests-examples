package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.lk.houseservice.HouseServiceMeterPopup;
import ru.yandex.arenda.element.lk.houseservice.MetersInput;

public interface LkOwnerHouseServiceMetersPage extends BasePage {

    String COUNTER_NUMBER_ID = "COUNTER_NUMBER";
    String TARIFF_ID = "TARIFF";
    String CURRENT_VALUE_1_ID = "CURRENT_VALUE_1";
    String COUNTER_PHOTO_1_ID = "COUNTER_PHOTO_1";
    String CURRENT_VALUE_2_ID = "CURRENT_VALUE_2";
    String COUNTER_PHOTO_2_ID = "COUNTER_PHOTO_2";
    String CURRENT_VALUE_3_ID = "CURRENT_VALUE_3";
    String COUNTER_PHOTO_3_ID = "COUNTER_PHOTO_3";
    String DELIVER_FROM_DAY_ID = "DELIVER_FROM_DAY";
    String DELIVER_TO_DAY_ID = "DELIVER_TO_DAY";
    String INSTALLED_PLACE_ID = "INSTALLED_PLACE";

    @Name("Кнопка добавить счетчик")
    @FindBy(".//div[contains(@class,'AddItemSnippet__snippet')]")
    AtlasWebElement addMeterButton();

    @Name("")
    @FindBy(".//div[contains(@class,'HouseServiceCreateCounterModal__popup')]")
    HouseServiceMeterPopup metersPopup();

    @Name("Инпуты с id=«{{ value }}»")
    @FindBy(".//form/div[.//input[@id='{{ value }}']]")
    MetersInput metersInput(@Param("value") String value);

    @Name("")
    @FindBy(".//h2[contains(@class,'FormCard__title')]")
    AtlasWebElement metersTitle();
}
