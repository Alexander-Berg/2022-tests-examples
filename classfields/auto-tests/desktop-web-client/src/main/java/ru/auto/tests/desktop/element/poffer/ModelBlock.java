package ru.auto.tests.desktop.element.poffer;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface ModelBlock extends Block {

    @Name("Список моделей")
    @FindBy("//div[contains(@class, 'models-list__') and contains(@class, 'visible')]" +
            "//div[contains(@class, 'models-list__item')]")
    ElementsCollection<VertisElement> modelsList();

    @Name("Модель «{{ text }}» в списке")
    @FindBy(".//div[contains(@class, 'menu-item') and .= '{{ text }}']")
    VertisElement model(@Param("text") String Text);

    @Step("Получаем модель с индексом {i}")
    default VertisElement getListItem(int i) {
        return modelsList().should(hasSize(greaterThan(i))).get(i);
    }
}