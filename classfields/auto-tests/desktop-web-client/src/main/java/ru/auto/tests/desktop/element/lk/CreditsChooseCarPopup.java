package ru.auto.tests.desktop.element.lk;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CreditsChooseCarPopup extends VertisElement, WithInput, WithButton {

    @Name("Список машин")
    @FindBy(".//div[contains(@class, 'CreditChooseCarListing__snippet')]")
    ElementsCollection<CreditsChooseCarPopupItem> carsList();

    @Step("Получаем машину с индексом {i}")
    default CreditsChooseCarPopupItem getCar(int i) {
        return carsList().should(hasSize(greaterThan(i))).get(i);
    }
}