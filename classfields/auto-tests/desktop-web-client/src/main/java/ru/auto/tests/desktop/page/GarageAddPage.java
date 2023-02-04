package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.garage.DreamCarBlock;
import ru.auto.tests.desktop.element.garage.MyCarBlock;

public interface GarageAddPage extends BasePage {

    String ADD_TO_GARAGE = "Добавить в гараж";
    String PUT_INTO_GARAGE = "Поставить";
    String ADD_INPUT_PLACEHOLDER = "Госномер или VIN";
    String PUT_VIN = "Введите VIN";

    @Name("Блок «Мой автомобиль»")
    @FindBy(".//div[@class = 'PageGarageAddCard__section']")
    MyCarBlock myCarBlock();

    @Name("Блок «Машина мечты»")
    @FindBy(".//div[contains(@class, 'PageGarageAddCard__section_dreamcar')]")
    DreamCarBlock dreamCarBlock();

    @Name("Блок «Моя бывшая»")
    @FindBy(".//div[@class = 'PageGarageAddCard__section_excar']")
    MyCarBlock myExCarBlock();

}
