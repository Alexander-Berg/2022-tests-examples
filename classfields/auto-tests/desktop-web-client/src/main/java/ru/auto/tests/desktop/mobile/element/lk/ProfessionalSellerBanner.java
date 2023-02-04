package ru.auto.tests.desktop.mobile.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ProfessionalSellerBanner extends VertisElement {

    @Name("Иконка закрытия")
    @FindBy(".//*[contains(@class, 'closerIcon')]")
    VertisElement close();

}
