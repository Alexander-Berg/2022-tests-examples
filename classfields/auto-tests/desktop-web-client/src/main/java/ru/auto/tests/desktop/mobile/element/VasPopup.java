package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 15.02.18
 */
public interface VasPopup extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, '__title')]")
    VertisElement title();

    @Name("Множитель")
    @FindBy(".//div[contains(@class, '__multiplier')]")
    VertisElement multiplier();

    @Name("Описание")
    @FindBy(".//div[contains(@class, '__description')]")
    VertisElement description();

    @Name("Статус")
    @FindBy(".//div[contains(@class, 'VasPromoItem__footer')]")
    VertisElement status();

    @Name("Кнопка «Подключить за ...»")
    @FindBy(".//div[contains(@class, '-payments')] | " +
            ".//button[contains(@class, 'buyButton')]")
    VertisElement payment();
}
