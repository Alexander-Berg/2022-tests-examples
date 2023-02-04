package ru.yandex.realty.element.newbuildingsite;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface ChatMessage extends AtlasWebElement {

    String BIND_PHONE = "Привязать телефон";

    @Name("Кнопка «Хочу купить»")
    @FindBy(".//div[@data-id='A_WANT_TO_BUY']")
    AtlasWebElement wantToBuy();

    @Name("Кнопка в сообщении «{{ value }}»")
    @FindBy(".//div[contains(@class,'ChatMessage__button') and not(contains(@class, 'buttons')) " +
            "and contains(.,'{{ value }}')]")
    AtlasWebElement chatMessageButton(@Param("value") String value);
}
