package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.page.BasePage;

public interface LkCreditsDraftPage extends BasePage {

    @Name("Блок информации о кредите на машину")
    @FindBy("//div[contains(@class, 'PageMyCreditsDraft__carInfo')]")
    VertisElement carInfo();

}
