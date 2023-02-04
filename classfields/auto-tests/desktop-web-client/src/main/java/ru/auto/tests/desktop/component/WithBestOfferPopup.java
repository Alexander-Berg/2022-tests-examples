package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.BestOfferPopup;

public interface WithBestOfferPopup {

    @Name("Поп-ап «Подбор дилеров с лучшими предложениями»")
    @FindBy("//div[contains(@class, 'MatchApplicationButton__modal') and contains(@class, 'visible')]")
    BestOfferPopup bestOfferPopup();
}