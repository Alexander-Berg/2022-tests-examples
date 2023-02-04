package ru.yandex.general.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ProfileSidebar extends VertisElement, Button, Image {

    @Name("Список активных полосок индикатора скора")
    @FindBy(".//div[contains(@class, 'ScoreIndicator__active')]")
    ElementsCollection<VertisElement> activeScoreIndicatorList();

    @Name("Кол-во подписчиков")
    @FindBy(".//div[contains(@class, 'PublicProfileSubscribesInfo__column')][1]/span")
    VertisElement followersCount();

    @Name("Кол-во подписок")
    @FindBy(".//div[contains(@class, 'PublicProfileSubscribesInfo__column')][2]/span")
    VertisElement followingCount();

}
