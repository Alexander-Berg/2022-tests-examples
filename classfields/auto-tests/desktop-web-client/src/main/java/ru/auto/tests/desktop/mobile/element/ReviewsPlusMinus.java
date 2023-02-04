package ru.auto.tests.desktop.mobile.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface ReviewsPlusMinus extends VertisElement {

    @Name("Вкладка «{{ text }}»")
    @FindBy(".//label[contains(@class, 'Radio_type_button') and .//span[text() = '{{ text }}']]")
    VertisElement tab(@Param("text") String text);

    @Name("Список плюсов/минусов во вкладке")
    @FindBy(".//div[@class = 'CardReviewsFeatures__feature']")
    ElementsCollection<VertisElement> plusMinusList();

    @Step("Получаем плюс/минус с индексом {i}")
    default VertisElement getPlusMinus(int i) {
        return plusMinusList().should(hasSize(greaterThan(i))).get(i);
    }
}