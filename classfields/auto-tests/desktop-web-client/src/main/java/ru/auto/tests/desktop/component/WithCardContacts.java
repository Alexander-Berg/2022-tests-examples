package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.card.CardContacts;

public interface WithCardContacts {

    @Name("Блок контактов оффера")
    @FindBy("//div[contains(@class, 'card__contact') or @class='CardOwner']")
    CardContacts contacts();
}
