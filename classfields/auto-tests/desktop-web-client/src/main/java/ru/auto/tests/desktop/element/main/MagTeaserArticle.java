package ru.auto.tests.desktop.element.main;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface MagTeaserArticle extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//span[contains(@class, 'JournalTeaserListOfArticles__text')]")
    VertisElement title();
}
