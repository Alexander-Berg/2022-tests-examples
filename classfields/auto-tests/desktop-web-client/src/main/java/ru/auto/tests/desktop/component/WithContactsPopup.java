package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.ContactsPopup;

public interface WithContactsPopup {

    @Name("Поп-ап контактов")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    ContactsPopup contactsPopup();
}
