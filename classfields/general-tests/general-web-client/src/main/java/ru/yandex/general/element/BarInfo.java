package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BarInfo extends VertisElement {

    @Name("Кол-во просмотров")
    @FindBy(".//div[contains(@class, '_stat_')][1]")
    VertisElement viewsCount();

    @Name("Кол-во контактов")
    @FindBy(".//div[contains(@class, '_stat_')][2]")
    VertisElement contactsCount();

    @Name("Кол-во добавлений в избранное")
    @FindBy(".//div[contains(@class, '_stat_')][3]")
    VertisElement favoritesAddedCount();

}
