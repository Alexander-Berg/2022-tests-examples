package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.SoldPopup;

public interface WithSoldPopup {

    @Name("Поп-ап снятия с продажи")
    @FindBy("//div[contains(@class, 'OfferHideDialog')]")
    SoldPopup soldPopup();
}