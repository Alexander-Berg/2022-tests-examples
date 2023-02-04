package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.Textbook;

public interface WithTextbook {

    @Name("Блок «Учебник Авто.ру»")
    @FindBy("//div[contains(@class, 'Textbook IndexBlock')] |" +
            "//div[contains(@class, 'Textbook PageCard__sectionVertical')]")
    Textbook textbook();
}