package ru.auto.tests.desktop.element.catalog;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface MarkModelGenBlock extends VertisElement {

    //* нужна, тег меняется динамически
    @Name("Элемент хлебных крошек")
    @FindBy(".//*[contains(@class,'search-form-v2-mmm__breadcrumbs-item')]")
    ElementsCollection<VertisElement> breadcrumbsItems();

    @Name("Элемент хлебных крошек '{{ text }}'")
    @FindBy(".//*[contains(@class,'search-form-v2-mmm__breadcrumbs-item_state') and text() ='{{ text }}']")
    VertisElement breadcrumbsItem(@Param("text") String text);

    @Name("Переключатель '{{ text }}'")
    @FindBy(".//span[contains(@class,'catalog__switcher')]//span[.='{{ text }}']")
    VertisElement switcher(@Param("text") String text);

    @Name("Список марок/моделей/поколений")
    @FindBy(".//div[contains(@class, 'search-form-v2-list_type') and " +
            "not(contains(@class, 'search-form-v2-list_invisible'))]//a[contains(@class,'search-form-v2-item')]")
    ElementsCollection<VertisElement> items();

    @Name("Список поколений/кузовов")
    @FindBy(".//a[contains(@class,'search-form-v2-list__card-item')]")
    ElementsCollection<VertisElement> generationsOrBodiesList();

    @Name("Кузов '{{ text }}'")
    @FindBy(".//a[contains(@class,'search-form-v2-list__card-item')]//div[text()='{{ text }}']")
    VertisElement body(@Param("text") String text);
}
