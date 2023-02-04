package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithSelect;
import ru.auto.tests.desktop.mobile.element.garage.DreamCarBlock;
import ru.auto.tests.desktop.mobile.element.garage.MyCarBlock;

public interface GarageAddPage extends BasePage, WithSelect {

    String LICENCE_PLATE_OR_VIN = "Госномер или VIN";
    String PUT_VIN = "Введите VIN";

    @Name("Блок «Мой автомобиль»")
    @FindBy(".//div[@class = 'PageGarageAddMobile__section']")
    MyCarBlock myCarBlock();

    @Name("Блок «Машина мечты»")
    @FindBy(".//div[contains(@class, 'PageGarageAddMobile__section_dreamcar')]")
    DreamCarBlock dreamCarBlock();

    @Name("Блок «Моя бывшая»")
    @FindBy(".//div[@class = 'GarageAddExInput']")
    MyCarBlock myExCarBlock();

    @Name("Кнопка «Отменить»")
    @FindBy(".//div[contains(@class, 'TopSearchField__cancel')]")
    VertisElement cancelButton();

}
