package ru.yandex.realty.element.searchHistory;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.RealtyElement;

/**
 * Created by ivanvan on 05.07.17.
 */
public interface SearchHistoryItem extends AtlasWebElement {

    @Name("Ссылка на историю")
    @FindBy(".//a[contains(@class, 'SearchHistoryItem__title')]")
    RealtyElement historyLink();

    @Name("Блок настройки подписки")
    @FindBy(".//div[contains(@class, 'SearchHistoryItem__controls')]")
    SubscriptionSettingBlock subscriptionSettingBlock();
}
