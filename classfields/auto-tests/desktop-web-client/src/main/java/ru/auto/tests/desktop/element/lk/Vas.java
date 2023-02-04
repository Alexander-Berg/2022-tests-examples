package ru.auto.tests.desktop.element.lk;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCheckbox;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Vas extends VertisElement, WithCheckbox {

    String VALID_FOR = "Действует ещё";
    String SHOWING_IN_STORIES = "Показ в Историях";
    String RISING_TO_TOP = "Поднятие в ТОП";
    String SPECIAL_OFFER = "Спецпредложение";

    @Name("Список вкладок услуг")
    @FindBy(".//div[contains(@class, 'VasTabs__tab')]")
    ElementsCollection<VertisElement> tabsList();

    @Name("Опция «{{ text }}»")
    @FindBy(".//div[@class = 'VasItem' and .//div[. = '{{ text }}']]")
    VertisElement option(@Param("text") String text);

    @Name("Чекбокс опции «{{ text }}»")
    @FindBy(".//div[@class = 'VasItem' and .//div[. = '{{ text }}']]//label[contains(@class, 'Checkbox')]")
    VertisElement optionCheckbox(@Param("text") String text);

    @Name("Статус автопродления опции «{{ text }}»")
    @FindBy(".//div[@class = 'VasItem' and .//div[. = '{{ text }}']]" +
            "//div[contains(@class, 'VasAutoProlongStatus__status_on')]")
    VertisElement optionAutoprolongStatus(@Param("text") String text);

    @Name("Срок действия подключённой опции «{{ text }}»")
    @FindBy(".//div[@class = 'VasItem' and .//div[. = '{{ text }}']]//span[contains(@class, 'DaysLeft')]")
    VertisElement optionDaysLeft(@Param("text") String text);

    @Name("Кнопка «Применить»")
    @FindBy(".//button[contains(@class, 'Button_color_green ')]")
    VertisElement buyButton();

    @Step("Получаем вкладку с индексом {i}")
    default VertisElement getTab(int i) {
        return tabsList().should(hasSize(greaterThan(i))).get(i);
    }
}
