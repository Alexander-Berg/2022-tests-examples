package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.SubCategories;

public interface WithSubCategories {

    @Name("Блок подкатегорий мото/комТС")
    @FindBy("//div[contains(@class, 'ListingCategory')]")
    SubCategories subCategories();
}