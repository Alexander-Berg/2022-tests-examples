package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 14.01.19
 */
public interface WithTariffs extends VertisElement {

    @Name("Активный тариф")
    @FindBy(".//tr[@class = 'CalculatorQuotaTableItem__row--active']")
    Tariff activeTariff();

    @Name("Безлимит")
    @FindBy(".//tr[@class = 'CalculatorQuotaTableItem__row--highlighted']")
    Tariff unlimTariff();

    @Name("Тарифы")
    @FindBy(".//tr[@class = 'CalculatorQuotaTableItem__row']")
    ElementsCollection<Tariff> tariffs();

    @Name("Кнопка «{{ name }}»")
    @FindBy(".//button[contains(., '{{ name }}')]")
    VertisElement button(@Param("name") String name);

    default Tariff tariff(int i) {
        return tariffs().should(hasSize(greaterThan(i))).get(i);
    }
}
