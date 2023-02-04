package ru.yandex.realty.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;

/**
 * Created by vicdev on 18.04.17.
 */
public interface SnippetElement extends Button, Link {

    String SHOW_PHONE = "Показать телефон";
    String CALLBACK = "Обратный звонок";

    @Name("Кнопка показа телефона")
    @FindBy(".//button[contains(@class, 'SnippetContacts__button')]")
    AtlasWebElement showPhoneButton();
}
