package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.Vas;
import ru.auto.tests.desktop.mobile.element.VasPopup;

public interface WithVas {

    @Name("Блок Vas")
    @FindBy("//div[@class = 'sales__vas'] | " +
            "//div[contains(@class, 'VasBlock')]")
    Vas vas();

    @Name("Поп-ап услуги продвижения")
    @FindBy("//div[contains(@class, 'Modal_visible')]//div[contains(@class, 'Modal__content')] | " +
            "//div[contains(@class, 'image-gallery-slide center VasGallery__slideContainer')]")
    VasPopup vasPopup();
}