package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ElectroModelPage extends BasePage {

    String PUT_INTO_GARAGE = "Поставить в Гараж";
    String GO_TO_GARAGE = "Перейти в Гараж";
    String COMPARE = "Сравнить";
    String GO_TO_COMPARE = "К сравнению";

    @Name("Название")
    @FindBy("//span[contains(@class, '_heroTitle')]")
    VertisElement title();

    @Name("Описание")
    @FindBy("//div[contains(@class, '_description')]")
    VertisElement description();

    @Name("Табличка с тех. параметрами")
    @FindBy("//table[@class = 'PageElectroCardDesktop__table']")
    VertisElement techParamsTable();

    @Name("Кнопка добавления/перехода в гараж")
    @FindBy("//button[contains(@class, '_heroActionButton_type_garage')]")
    VertisElement toGarage();

    @Name("Кнопка сравнить/перейти к сравнению")
    @FindBy("//div[contains(@class, 'ButtonCompare')]")
    VertisElement toCompare();

    @Name("Список офферов в продаже")
    @FindBy("//div[contains(@class, '_offer')]")
    ElementsCollection<VertisElement> offers();

    @Name("Кнопка перехода ко всем офферам")
    @FindBy("//a[contains(@class, '_button_type_block')]")
    VertisElement showAllOffers();

}
