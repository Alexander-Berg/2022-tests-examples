package ru.auto.tests.desktop.element.cabinet.feeds;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface DownloadHistory extends VertisElement {

    @Name("Список фидов")
    @FindBy("//tr[contains(@class, 'FeedsHistory__itemRow')]")
    ElementsCollection<DownloadHistoryItem> feedsList();

    @Step("Получаем фид с индексом {i}")
    default DownloadHistoryItem getFeed(int i) {
        return feedsList().should(hasSize(greaterThan(i))).get(i);
    }
}