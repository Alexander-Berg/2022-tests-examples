package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.CrossLinksBlock;

public interface WithCrossLinksBlock {

    @Name("Блок перелинковки")
    @FindBy("//div[contains(@class, 'CrossLinks')]")
    CrossLinksBlock crossLinksBlock();
}
