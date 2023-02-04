package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.MMMFilter;

public interface WithMMMFilter {

    @Name("Фильтр по марке, модели, модификации")
    @FindBy("//div[contains(@class, 'BreadcrumbsFilter')] | " +
            "//div[contains(@class, 'MMMMultiFilter')]")
    MMMFilter mmmFilter();
}
