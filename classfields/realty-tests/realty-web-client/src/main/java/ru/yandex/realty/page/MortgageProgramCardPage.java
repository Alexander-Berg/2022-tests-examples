package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.AlfaModal;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.mobile.page.BasePage;

public interface MortgageProgramCardPage extends BasePage, AlfaModal {

    String TAKE_APPLICATION = "Подать заявку";

    @Name("Калькулятор")
    @FindBy(".//div[contains(@class,'MortgageProgramCard__calculator')]")
    Button calculator();
}
