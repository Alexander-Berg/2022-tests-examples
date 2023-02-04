package ru.auto.tests.desktop.element.electro;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface JournalSection extends Section {

    String SHOW_MORE_MATERIALS = "Показать больше материалов";

    @Name("Список постов")
    @FindBy(".//a[contains(@class, '_snippet')]")
    ElementsCollection<VertisElement> posts();

}
