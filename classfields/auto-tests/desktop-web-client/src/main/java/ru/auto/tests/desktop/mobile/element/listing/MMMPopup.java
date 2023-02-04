package ru.auto.tests.desktop.mobile.element.listing;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;
import ru.auto.tests.desktop.mobile.component.WithRadioButton;
import ru.auto.tests.desktop.mobile.element.WithInput;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface MMMPopup extends VertisElement, WithRadioButton, WithInput, WithCheckbox, WithButton {

    @Name("Заголовок")
    @FindBy("//div[contains(@class, 'FiltersPopup__title')]")
    VertisElement title();

    @Name("Поиск")
    @FindBy("//div[@class = 'SearchTextInput']//input")
    VertisElement search();

    @Name("Кнопка закрытия поп-апа")
    @FindBy(".//div[contains(@class, 'FiltersPopup__close')]")
    VertisElement closeButton();

    @Name("Кнопка «Сбросить»")
    @FindBy("//div[contains(@class, 'FiltersPopup__reset')]")
    VertisElement resetButton();

    @Name("Список марок")
    @FindBy(".//div[contains(@class, 'ListItem__name')]")
    ElementsCollection<VertisElement> marksList();

    @Step("Получаем марку с индексом {i}")
    default VertisElement getMark(int i) {
        return marksList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Список моделей")
    @FindBy(".//div[contains(@class, 'ListItem__name')]")
    ElementsCollection<VertisElement> modelsList();

    @Step("Получаем модель с индексом {i}")
    default VertisElement getModel(int i) {
        return modelsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Марка «{{ text }}» в списке популярных марок")
    @FindBy(".//div[contains(@class, 'FilterableListMarks__section') and .//div[.= 'Популярные']]" +
            "//div[@class = 'ListItem' and .= '{{ text }}']")
    MMMPopupMark popularMark(@Param("text") String text);

    @Name("Марка «{{ text }}» в списке всех марок")
    @FindBy(".//div[contains(@class, 'FilterableListMarks__section') and .//div[.= 'Все']]" +
            "//div[(@class = 'ListItem' or @class = 'ListItem ListItem__root' or @class = 'ListItem ListItem__child') and .= '{{ text }}']")
    MMMPopupMark allMark(@Param("text") String text);

    @Name("Модель «{{ text }}» в списке популярных марок")
    @FindBy(".//div[contains(@class, 'FilterableListModels__section') and .//div[.= 'Популярные']]" +
            "//div[(@class = 'ListItem' or @class = 'ListItem ListItem__root' or @class = 'ListItem ListItem__child') and .= '{{ text }}']")
    MMMPopupModel popularModel(@Param("text") String model);

    @Name("Модель «{{ text }}» в списке всех марок")
    @FindBy(".//div[contains(@class, 'FilterableListModels__section') and .//div[.= 'Все']]" +
            "//div[(@class = 'ListItem' or @class = 'ListItem ListItem__root' or @class = 'ListItem ListItem__child') and .= '{{ text }}']")
    MMMPopupModel allModel(@Param("text") String model);

    @Name("Поколение «{{ text }}»")
    @FindBy(".//div[contains(@class, 'FilterableListGenerationItem') and .//div[.= '{{ text }}']]|" +
            ".//div[contains(@class, 'PortalItem VersusHead__portalItem') and .//div[.= '{{ text }}']]")
    MMMPopupGeneration generation(@Param("text") String text);

    @Name("Популярные марки")
    @FindBy(".//div[contains(@class, 'FilterableListMarks__list')]")
    VertisElement popularMarks();

    @Name("Популярные модели")
    @FindBy(".//div[contains(@class, 'FilterableListModels__list')]")
    VertisElement popularModels();

    @Name("Кнопка применения фильтров")
    @FindBy(".//div[contains(@class, 'FiltersPopup__bottom')]//button")
    VertisElement applyFiltersButton();

    @Name("Кнопка применения фильтров «{{ text }}»")
    @FindBy(".//div[contains(@class, 'FiltersPopup__bottom')]//button[.= '{{ text }}']")
    VertisElement applyFiltersButton(@Param("text") String text);
}