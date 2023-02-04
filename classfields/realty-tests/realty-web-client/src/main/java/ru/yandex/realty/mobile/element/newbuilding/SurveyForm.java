package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.saleads.InputField;

public interface SurveyForm extends Button, InputField {

    @Name("Инпут заявки «{{ value }}»")
    @FindBy(".//div[@class='survey__line' and contains(.,'{{ value }}')]//input")
    AtlasWebElement inputField(@Param("value") String value);

    @Name("Чекбокс обработки персональных данных")
    @FindBy(".//span[contains(.,'Принимаю условия обработки персональных данных')]//input")
    AtlasWebElement checkboxAccept();
}
