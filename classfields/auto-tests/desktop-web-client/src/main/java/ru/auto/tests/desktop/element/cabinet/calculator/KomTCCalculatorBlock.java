package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 18.12.18
 */
public interface KomTCCalculatorBlock extends VertisElement, WithTariffs, Services {

    String HEAVE_COMMERCIAL_VEHICLES_USED = "Легкий ком.\u00a0тс с\u00a0пробегом";
    String HEAVE_COMMERCIAL_VEHICLES_NEW = "Легкий ком.\u00a0тс новый";
    String HEAVE_COMMERCIAL_VEHICLES = "Тяжелый ком.\u00a0тс";
    String SPECIAL_VEHICLES = "Спецтехника";

    @Name("Вкладка «{{ name }}»")
    @FindBy(".//span[contains(@class, 'CalculatorCategorySingle__tabItem')][contains(., '{{ name }}')]")
    VertisElement tab(@Param("name") String name);

    @Name("Вкладка «{{ name }}»")
    @FindBy(".//span[contains(@class, 'ServiceNavigation__link_active')][contains(., '{{ name }}')]")
    VertisElement activeTab(@Param("name") String name);

    @Name("Кнопка «{{ name }}»")
    @FindBy(".//button[contains(., '{{ name }}')]")
    VertisElement button(@Param("name") String name);

    @Name("Сумма размещения")
    @FindBy(".//div[@class = 'CollapseCard__info']")
    VertisElement amount();

    @Name("Услуги")
    @FindBy(".//div[@class = 'CalculatorServicesTable__container'][./div[contains(., 'Услуги')]]")
    Services services();
}
