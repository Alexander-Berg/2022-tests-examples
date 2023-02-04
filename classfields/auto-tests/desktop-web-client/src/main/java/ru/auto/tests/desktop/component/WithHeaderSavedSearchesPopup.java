package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.HeaderSearchesPopup;

public interface WithHeaderSavedSearchesPopup {

    @Name("Поп-ап сохранённых поисков")
    @FindBy("//div[contains(@class, 'searches__list') or (@class = 'TopNavigationSubscriptions__popup')] | " +
            "//div[contains(@class, 'HeaderSubscriptionsPopup')]")
    HeaderSearchesPopup headerSavedSearchesPopup();
}
