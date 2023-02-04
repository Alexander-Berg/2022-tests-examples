package ru.auto.tests.desktop.element.main;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.component.WithSelectGroup;

public interface CommercialBlock extends VertisElement, WithSelect, WithSelectGroup, WithButton {

    @Name("Блок ссылок")
    @FindBy(".//div[contains(@class, 'IndexLinks')]")
    VertisElement urlsBlock();

    @Name("Баннер")
    @FindBy(".//div[contains(@class, 'IndexCommercial__teaser')]/..//div[@class = 'IndexTeaser__content']")
    VertisElement banner();

    @Name("Название баннера")
    @FindBy(".//div[contains(@class, 'IndexCommercial__teaser')]/..//div[@class = 'IndexTeaser__title']")
    VertisElement bannerTitle();
}
