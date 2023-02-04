package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface SameButNew extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'IndexBlock__title')]")
    VertisElement title();

    @Name("Список объявлений")
    @FindBy(".//a[contains(@class, 'IndexBlock__item ')]")
    ElementsCollection<VertisElement> salesList();

    @Step("Получаем объявление с индексом {i}")
    default VertisElement getSale(int i) {
        return salesList().should(hasSize(greaterThan(i))).get(i);
    }
}