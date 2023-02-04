package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.card.CardHeader;

public interface WithCardHeader {

    @Name("Заголовок объявления")
    @FindBy(".//div[@class = 'CardHead'] | " +
            ".//div[contains(@class, 'CardHead_new')]")
    CardHeader cardHeader();
}
