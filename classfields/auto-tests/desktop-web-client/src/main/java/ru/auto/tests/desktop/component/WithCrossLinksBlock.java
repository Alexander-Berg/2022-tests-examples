package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.CrossLinksBlock;

public interface WithCrossLinksBlock {

    @Name("Блок перелинковки")
    @FindBy("//div[contains(@class, 'CrossLinks') or contains(@class, 'cross-links')]")
    CrossLinksBlock crossLinksBlock();
}
