package ru.auto.tests.desktop.element.main;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface MarksBlock extends VertisElement {

    @Name("Переключатель «{{ text }}»")
    @FindBy(".//span[contains(@class, 'RadioGroup_type_button')]//button[. = '{{ text }}']")
    VertisElement switcher(@Param("text") String text);

    @Name("Выбранный переключатель «{{ text }}»")
    @FindBy(".//label[contains(@class, 'Radio_checked') and . = '{{ text }}'] | " +
            ".//div[contains(@class, 'IndexSelector__tabs')]//button[contains(@class, 'Button_checked') " +
            "and . = '{{ text }}']")
    VertisElement selectedSwitcher(@Param("text") String text);

    @Name("Список логотипов марок")
    @FindBy(".//a[@class = 'Link IndexSuperMark']")
    ElementsCollection<VertisElement> marksLogosList();

    @Step("Получаем логотип с индексом {i}")
    default VertisElement getMarkLogo(int i) {
        return marksLogosList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Список марок")
    @FindBy(".//a[@class = 'IndexMarks__item']")
    ElementsCollection<VertisElement> marksList();

    @Step("Получаем марку с индексом {i}")
    default VertisElement getMark(int i) {
        return marksList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Ссылка «Все марки»")
    @FindBy(".//div[@class = 'IndexMarks__show-all']")
    VertisElement allMarksUrl();

    @Name("Марка «{{ text }}»")
    @FindBy(".//a[@class = 'IndexMarks__item' and . = '{{ text }}']")
    VertisElement mark(@Param("text") String text);

    @Name("Кузов «{{ text }}»")
    @FindBy(".//div[contains(@class, 'IndexBodyTypes__item_selected') " +
            "and ./div[@class = 'IndexBodyTypes__item-name' and .= '{{ text }}']]")
    VertisElement body(@Param("text") String text);

    @Name("Пресет «{{ text }}»")
    @FindBy(".//div[@class = 'IndexSelector__preset' and ./div[@class = 'IndexSelector__presetTitle' " +
            "and . = '{{ text }}']]")
    VertisElement preset(@Param("text") String text);

    @Name("Выделенный пресет «{{ text }}»")
    @FindBy(".//div[contains(@class, 'IndexSelector__preset_selected') " +
            "and ./div[@class = 'IndexSelector__presetTitle' and . = '{{ text }}']]")
    VertisElement selectedPreset(@Param("text") String text);

    @Name("Слайдер от")
    @FindBy(".//div[contains(@class, 'Slider__toggler_from')]")
    VertisElement sliderFrom();

    @Name("Слайдер до")
    @FindBy(".//div[contains(@class, 'Slider__toggler_to')]")
    VertisElement sliderTo();

    @Name("Кнопка «Показать» или «Ничего не найдено»")
    @FindBy(".//div[@class = 'IndexSelector__submit']/button")
    VertisElement resultsButton();

    @Name("Баннер")
    @FindBy(".//a[contains(@class, 'IndexSelector') and contains(@class, 'Banner')] | " +
            ".//a[contains(@class, 'Electro') and contains(@class, 'Banner')] | " +
            ".//div[contains(@class, 'IndexSelector')]/a[contains(@class, 'Banner')]"
    )
    VertisElement banner();
}
