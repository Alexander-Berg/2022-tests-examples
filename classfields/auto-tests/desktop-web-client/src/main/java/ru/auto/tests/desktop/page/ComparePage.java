package ru.auto.tests.desktop.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithContactsPopup;
import ru.auto.tests.desktop.component.WithRadioButton;
import ru.auto.tests.desktop.element.compare.AddModelPopup;
import ru.auto.tests.desktop.element.compare.Model;
import ru.auto.tests.desktop.element.compare.Sale;
import ru.auto.tests.desktop.element.compare.Stub;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface ComparePage extends BasePage, WithContactsPopup, WithRadioButton, WithCheckbox {

    @Name("Содержимое страницы")
    @FindBy("//div[@id = 'LayoutIndex']")
    VertisElement content();

    @Name("Заглушка")
    @FindBy("//div[contains(@class, 'EmptyComparison')]")
    Stub stub();

    @Name("Список объявлений")
    @FindBy("//th[contains(@class, 'ComparisonHeader__item ')]")
    ElementsCollection<Sale> salesList();

    @Step("Получаем объявление с индексом {i}")
    default Sale getSale(int i) {
        return salesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Список моделей")
    @FindBy("//th[contains(@class, 'ComparisonHeader__item ')]")
    ElementsCollection<Model> modelsList();

    @Step("Получаем модель с индексом {i}")
    default Model getModel(int i) {
        return modelsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Ссылка «N в продаже»")
    @FindBy(".//a[.= '{{ text }}']")
    VertisElement onSaleUrl(@Param("text") String text);

    @Name("Ссылка «Объявления» в столбце «{{ text }}»")
    @FindBy("(.//td[contains(@class, 'ComparisonRow__cell')])[{{ text }}]//a[.= 'Объявления']")
    VertisElement salesUrl(@Param("text") String text);

    @Name("Кнопка «Добавить модель»")
    @FindBy("//div[contains(@class, 'ComparisonHeader__addItemButton')]")
    VertisElement addModelButton();

    @Name("Поп-ап «Добавить модель»")
    @FindBy("//div[contains(@class, 'Curtain__container')]")
    AddModelPopup addModelPopup();

    @Name("Плавающая панель")
    @FindBy("//thead[contains(@class, 'ComparisonHeader__container_stuck')]/tr[contains(@class, 'ComparisonHeader')]")
    VertisElement floatingPanel();
}