package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface NpsPage extends BasePage {

    String RATE_BUTTON = "Оценить";

    @Name("Ошибка при нажатии «оценить» без оценки")
    @FindBy(".//div[contains(@class,'FormScoreSelectField__description')]")
    AtlasWebElement errorDescription();

    @Name("Оценка «{{ value }}»")
    @FindBy(".//div[contains(@class, 'FormScoreSelectField__rating') and contains(.,'{{ value }}')]")
    AtlasWebElement rating(@Param("value") String value);

    @Name("Тост")
    @FindBy("//div[contains(@role,'toast')]")
    AtlasWebElement toastSuccess();
}
