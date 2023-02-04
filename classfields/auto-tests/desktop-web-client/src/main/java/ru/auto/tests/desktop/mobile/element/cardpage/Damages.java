package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Damages extends VertisElement {

    @Name("Повреждение {{ num }}")
    @FindBy(".//div[contains(@class, 'VehicleBodyDamagesSchemeFrame__damageDot') and contains(., '{{ num }}')]")
    VertisElement damage(@Param("num") String num);

    @Name("Выбранное повреждение")
    @FindBy(".//div[contains(@class, 'VehicleBodyDamagesList__dot_active')]/..")
    Damage selectedDamage();
}