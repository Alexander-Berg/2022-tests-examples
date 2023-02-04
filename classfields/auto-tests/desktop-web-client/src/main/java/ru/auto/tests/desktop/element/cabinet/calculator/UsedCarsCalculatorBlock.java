package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 07.12.18
 */

public interface UsedCarsCalculatorBlock extends VertisElement, WithTariffs, Services {

    @Name("Сумма размещения")
    @FindBy(".//div[@class = 'CollapseCard__info']")
    VertisElement amount();

    @Name("Кнопка «{{ name }}»")
    @FindBy(".//button[contains(., '{{ name }}')]")
    VertisElement button(@Param("name") String name);

    @Name("Услуги")
    @FindBy(".//div[@class = 'CalculatorServicesTable__container'][./div[contains(., 'Услуги')]]")
    Services services();

    @Name("Добавить автомобилей с ценой {{ name }}")
    @FindBy(".//tr[contains(., '{{ name }}')]//label//input")
    VertisElement input(@Param("name") String name);

    @Name("Блок «Ограничение расхода на звонки»")
    @FindBy(".//div[contains(@class, '_limitContainer')][.//div[contains(@class, 'CallsLimits')]]")
    CallsLimitBlock callsLimitBlock();

}
