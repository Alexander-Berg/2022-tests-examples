package ru.yandex.realty.element.searchHistory;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.RealtyElement;
import ru.yandex.realty.element.saleads.InputField;

/**
 * Created by ivanvan on 30.08.17.
 */
public interface SubscriptionSettingBlock extends InputField, Button {

    String SUBSCRIBE = "Подписаться";

    @Name("Кнопка удаления")
    @FindBy(".//button[contains(@class, 'SearchHistoryItem__remove')]")
    RealtyElement deleteButton();
}
