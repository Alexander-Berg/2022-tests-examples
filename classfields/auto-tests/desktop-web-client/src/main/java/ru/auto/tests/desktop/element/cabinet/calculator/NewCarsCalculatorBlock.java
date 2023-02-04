package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 07.12.18
 */
public interface NewCarsCalculatorBlock extends VertisElement, WithTariffs, Services {

    @Name("Сумма размещения")
    @FindBy(".//div[@class = 'CollapseCard__info']")
    VertisElement amount();

    @Name("Кнопка «{{ name }}»")
    @FindBy(".//button[contains(., '{{ name }}')]")
    VertisElement button(@Param("name") String name);

    @Name("Блок «Ограничение расхода на звонки»")
    @FindBy(".//div[contains(@class, '_limitContainer')][.//div[contains(@class, 'CallsLimits')]]")
    CallsLimitBlock callsLimitBlock();

    @Name("Количество звонков за период")
    @FindBy(" .//label[contains(@class, 'CalculatorCategoryCalls__tableInput')]//input")
    VertisElement numberOfCalls();

    @Name("Услуги")
    @FindBy(".//div[@class = 'CalculatorServicesTable__container'][./div[contains(., 'Услуги')]]")
    Services services();

    @Name("Блок аукциона")
    @FindBy(".//div[contains(@class, 'Auction CalculatorCategoryCalls__container')]")
    AuctionBlock auctionBlock();

    @Name("Неактивный Тумблер «Приоритетное размещение»")
    @FindBy(".//label[@class = 'Toggle CalculatorCategoryCalls__priorityPlacementSwitcher']")
    VertisElement priorityPlacementToggleInactive();

    @Name("Активный тумблер «Приоритетное размещение»")
    @FindBy(".//label[contains(@class, 'priorityPlacementSwitcher Toggle_checked')]")
    VertisElement priorityPlacementToggleActive();
}
