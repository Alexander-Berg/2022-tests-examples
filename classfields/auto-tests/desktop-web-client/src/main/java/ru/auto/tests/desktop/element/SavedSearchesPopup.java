package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithSelect;

public interface SavedSearchesPopup extends VertisElement, WithButton, WithSelect, WithInput {

    @Name("Ссылка на поиск")
    @FindBy(".//a[contains(@class, 'SubscriptionItemDesktop__title')]")
    VertisElement searchUrl();

    @Name("Кнопка удаления сохраненного поиска")
    @FindBy(".//*[contains(@class, 'remove-icon')]")
    VertisElement deleteButton();
}