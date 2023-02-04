package ru.auto.tests.desktop.element.poffer;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface MarkBlock extends Block {

    @Name("Список  марок")
    @FindBy("//div[contains(@class, 'marks-list__') and contains(@class, 'visible')]" +
            "//div[contains(@class, 'marks-list__item')]")
    ElementsCollection<VertisElement> marksList();

    @Name("Марка «{{ text }}» в списке")
    @FindBy(".//div[contains(@class, 'menu-item') and .= '{{ text }}']")
    VertisElement mark(@Param("text") String Text);

    @Step("Получаем марку с индексом {i}")
    default VertisElement getListItem(int i) {
        return marksList().should(hasSize(greaterThan(i))).get(i);
    }
}