package ru.yandex.realty.element.ipoteka;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.RealtyElement;

public interface MortgageProgram extends RealtyElement, Link, Button {

    String REGISTER_BUTTON = "Оформить";
    String MORE_BUTTON = "Подробнее";

    @Name("Кнопка расширения программы")
    @FindBy(".//button[contains(@class,'MortgageProgramSnippetSearch__expandButton')]")
    AtlasWebElement expand();

}
