package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 05.03.18
 */
public interface SavedSearch extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//a[contains(@class, 'SubscriptionItemMobile__title')]")
    VertisElement title();

    @Name("Кнопка удаления сохраненного поиска")
    @FindBy(".//*[contains(@class, 'remove-icon')]")
    VertisElement deleteButton();

}
