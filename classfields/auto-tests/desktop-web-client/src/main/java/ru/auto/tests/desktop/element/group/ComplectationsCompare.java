package ru.auto.tests.desktop.element.group;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface ComplectationsCompare extends VertisElement {

    @Name("Список комплектаций")
    @FindBy(".//th[contains(@class, 'CardGroupCompareHeader__item')]")
    ElementsCollection<VertisElement> complectationsList();

    @Name("Кнопка >")
    @FindBy(".//*[contains(@class, '__arrowRight')]")
    VertisElement nextButton();

    @Name("Кнопка <")
    @FindBy(".//*[contains(@class, '__arrowLeft')]")
    VertisElement prevButton();

    @Name("Группа опций «{{ text }}»")
    @FindBy(".//td[contains(@class, 'CardGroupCompare__cell_type_groupTitle') and .= '{{ text }}']")
    VertisElement optionsGroup(@Param("text") String Text);

    @Name("Плавающий блок со списком комплектаций")
    @FindBy(".//thead[contains(@class, 'CardGroupCompareHeader__container_stuck')]")
    VertisElement floatingComplectations();

    @Step("Получаем комплектацию с индексом {i}")
    default VertisElement getComplectation(int i) {
        return complectationsList().should(hasSize(greaterThan(i))).get(i);
    }
}