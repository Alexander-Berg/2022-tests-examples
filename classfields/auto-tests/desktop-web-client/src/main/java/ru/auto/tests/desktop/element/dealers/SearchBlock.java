package ru.auto.tests.desktop.element.dealers;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface SearchBlock extends VertisElement, WithButton {

    @Name("Блок марок")
    @FindBy(".//div[contains(@class, 'DealerSearchForm__marks-list')]")
    VertisElement marks();

    @Name("Марка «{{ text }}»")
    @FindBy(".//span[contains(@class, 'DealerMarksItem_index_') and .= '{{ text }}']")
    VertisElement mark(@Param("text") String text);

    @Name("Список марок")
    @FindBy(".//span[contains(@class, 'DealerMarksItem_index')]")
    ElementsCollection<VertisElement> marksList();

    @Step("Получаем марку с индексом {i}")
    default VertisElement getMark(int i) {
        return marksList().should(hasSize(greaterThan(i))).get(i);
    }
}