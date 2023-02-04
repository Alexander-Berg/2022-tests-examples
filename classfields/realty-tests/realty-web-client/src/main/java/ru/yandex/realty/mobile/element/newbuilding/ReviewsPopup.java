package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.mobile.element.CloseCross;

public interface ReviewsPopup extends CloseCross, Button {

    String FINISH_REVIEW = "Оставить отзыв";

    @Name("Тубмлер «Анонимно»")
    @FindBy(".//div[contains(@class,'ReviewsForm__anon')]")
    AtlasWebElement anonButton();

    @Name("Текстовый блок")
    @FindBy(".//textarea")
    AtlasWebElement textarea();

    @Name("Звездочка «{{ value }}»")
    @FindBy(".//div[@data-rating='{{ value }}']")
    AtlasWebElement star(@Param("value") String value);
}
