package ru.auto.tests.desktop.mobile.element.garage;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface InsuranceBlock extends VertisElement, WithButton {

    @Name("Список страховок")
    @FindBy(".//div[contains(@class, 'GarageCardInsuranceList__item')]")
    ElementsCollection<Insurance> list();

    @Name("Кнопка показать архив")
    @FindBy(".//div[@class='GarageCardInsuranceList__showArchive']")
    VertisElement showArchive();
}