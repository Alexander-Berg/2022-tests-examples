package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 15.02.18
 */
public interface VasService extends VertisElement, WithButton {

    @Name("Название услуги")
    @FindBy(".//div[contains(@class, 'sales__vas-package-title') or contains(@class, 'sales__vas-service-title')] | " +
            ".//span[contains(@class, 'Link')]")
    VertisElement title();

    @Name("Статус услуги")
    @FindBy(".//div[contains(@class, 'VasItemPackage__subtitle')]")
    VertisElement status();

    @Name("Иконка услуги")
    @FindBy(".//span[contains(@class, 'VasIcon')]")
    VertisElement icon();

    @Name("Период действия услуги продвижения")
    @FindBy(".//div[contains(@class, 'VasItemPackage__activePeriod')]")
    VertisElement activePeriod();

    @Name("Кнопка покупки")
    @FindBy(".//div[contains(@class, '__prices')]/button")
    VertisElement buyButton();

    @Name("Неактивный тумблер автоподнятия")
    @FindBy(".//label[contains(@class, 'Toggle') and not(contains(@class, 'Toggle_checked'))]")
    VertisElement autoFreshTogglerInactive();

    @Name("Активный тумблер автоподнятия")
    @FindBy(".//label[contains(@class, 'Toggle_checked')]")
    VertisElement autoFreshTogglerActive();
}
