package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.card.CardVas;

public interface WithCardVas {

    @Name("Блок услуг")
    @FindBy("//div[contains(@class, 'card-vas ')] | " +
            "//div[@class = 'CardVAS']")
    CardVas cardVas();
}
