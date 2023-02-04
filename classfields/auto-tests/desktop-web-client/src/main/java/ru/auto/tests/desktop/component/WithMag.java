package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.Mag;

public interface WithMag {

    @Name("Блок «Журнал»")
    @FindBy("//div[@class = 'Journal Index__block'] | " +
            "//section[@class = 'Journal Index__block'] | " +
            "//div[@id = 'block-magazine']")
    Mag mag();
}