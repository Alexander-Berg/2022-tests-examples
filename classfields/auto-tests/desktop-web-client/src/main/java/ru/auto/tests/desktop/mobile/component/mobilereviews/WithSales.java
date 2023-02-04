package ru.auto.tests.desktop.mobile.component.mobilereviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.mobilereviews.Sales;

public interface WithSales {

    @Name("Объявления")
    @FindBy("//div[contains(@class, 'CarouselUniversal_dir_horizontal')]")
    Sales sales();
}