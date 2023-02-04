package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.page.BasePage;

public interface ElectroModelPage extends BasePage {

    String PUT_INTO_GARAGE = "Поставить в Гараж";
    String GO_TO_GARAGE = "Перейти в Гараж";
    String COMPARE = "Сравнить";
    String GO_TO_COMPARE = "К сравнению";

    @Name("Название")
    @FindBy("//span[contains(@class, '_headingTitle')]")
    VertisElement title();

    @Name("Описание")
    @FindBy("//div[contains(@class, '_description')]")
    VertisElement description();

    @Name("Табличка с тех. параметрами")
    @FindBy("//table[@class = 'PageElectroCardTouch__table']")
    VertisElement techParamsTable();

    @Name("Кнопка добавления/перехода в гараж")
    @FindBy("//div[contains(@class, '_actions')]/button")
    VertisElement toGarage();

    @Name("Список офферов в продаже")
    @FindBy("//div[contains(@class, '_offer')]")
    ElementsCollection<VertisElement> offers();

    @Name("Кнопка перехода ко всем офферам")
    @FindBy("//a[contains(@class, '_button_withGrid')]")
    VertisElement showAllOffers();

}
