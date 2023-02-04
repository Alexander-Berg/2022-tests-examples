package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WithCardBadges {

    @Name("Блок стикеров")
    @FindBy("//div[contains(@class, 'CardDescription__badges')]")
    VertisElement badges();

    @Name("Список стикеров")
    @FindBy("//div[contains(@class, 'CardDescription__badgesItem')]")
    io.qameta.atlas.webdriver.ElementsCollection<VertisElement> badgesList();
}
