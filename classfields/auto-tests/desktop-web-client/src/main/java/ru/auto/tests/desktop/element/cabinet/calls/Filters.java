package ru.auto.tests.desktop.element.cabinet.calls;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithRadioButton;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.component.cabinet.WithCalendar;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 24.05.18
 */
public interface Filters extends VertisElement, WithInput, WithSelect, WithRadioButton, WithCheckbox, WithButton,
        WithCalendar {

    String TYPE_WORD_OR_PHRASE_FOR_SEARCH = "Введите слово или фразу для поиска звонков";
    String CHOOSE_WHO_SHOULD_SAY_PHRASE = "Выберите кто должен произнести фразу";
    String SEARCH_IN_OPERATOR_TEXT = "Искать в тексте оператора";
    String SEARCH_IN_CLIENT_TEXT = "Искать в тексте клиента";
    String HIDE = "Свернуть";

    @Name("Меню «{{ name }}»")
    @FindBy(".//div[@class = 'calls__menu']//a[contains(., '{{ name }}')]")
    VertisElement menu(@Param("name") String name);

    @Name("Период")
    @FindBy(".//div[contains(@class, 'date-filter')]")
    PeriodBlock period();

    @Name("Кнопка «Экспорт»")
    @FindBy(".//a[contains(@class, 'calls__export')]")
    VertisElement export();

    @Name("Кнопка открытия календаря")
    @FindBy(".//button[contains(@class, 'DateRange__button')]")
    VertisElement calendarButton();

    @Name("Кнопка «Сбросить»")
    @FindBy(".//div[contains(@class, 'resetButton')]")
    VertisElement resetButton();

    @Name("Кнопка «Все параметры»")
    @FindBy(".//span[contains(@class, '_toggleButton')]")
    VertisElement allParameters();

    @Name("Фильтры по офферам")
    @FindBy(".//div[@class = 'CallsFilters__offerFilters']")
    Filters offerFilters();

    @Name("Список тегов")
    @FindBy("//div[contains(@class, 'CallsFiltersTags__item')]")
    ElementsCollection<VertisElement> tagsList();

    @Name("Каунтер")
    @FindBy(".//div[contains(@class, '_counter')]")
    VertisElement counter();

    @Name("Кнопка поиска")
    @FindBy(".//button//*[contains(@class, '_search')]")
    VertisElement search();

    @Step("Получаем тег с индексом {i}")
    default VertisElement getTag(int i) {
        return tagsList().should(hasSize(greaterThan(i))).get(i);
    }
}
