package ru.auto.tests.desktop.element.card;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface Damages extends VertisElement {

    @Name("Список повреждений")
    @FindBy(".//div[contains(@class, 'VehicleBodyDamagesSchemeFrame__damageDot')]")
    ElementsCollection<VertisElement> pins();

    @Step("Получаем повреждение с индексом {i}")
    default VertisElement getPin(int i) {
        return pins().should(hasSize(greaterThan(i))).get(i);
    }
}