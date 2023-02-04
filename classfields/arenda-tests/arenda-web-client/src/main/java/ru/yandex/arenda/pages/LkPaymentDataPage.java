package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface LkPaymentDataPage extends BasePage {

    String INN_FIELD = "ИНН";
    String BIK_FIELD = "БИК";
    String ACCOUNT_NUMBER_FIELD = "Расчетный счёт";
    String FIO = "ФИО";

    @Name("Поле «{{ value }}»")
    @FindBy(".//div[contains(@class,'FormReadonlyField__readonlyField')][contains(.,'{{ value }}')]")
    AtlasWebElement field(@Param("value") String value);
}
