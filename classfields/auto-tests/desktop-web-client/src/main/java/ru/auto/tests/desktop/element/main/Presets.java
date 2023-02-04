package ru.auto.tests.desktop.element.main;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Presets extends VertisElement {

    @Name("Кнопка обновления пресета")
    @FindBy(".//div[@class = 'IndexPresets__load-more']/button")
    VertisElement refreshButton();

    @Name("Список пресетов")
    @FindBy(".//label[contains(@class, 'Radio_type_button')]/button")
    ElementsCollection<VertisElement> presetsList();

    @Name("Пресет «{{ text }}»")
    @FindBy(".//label[contains(@class, 'Radio_type_button') and .//span[text() = '{{ text }}']]")
    VertisElement preset(@Param("text") String text);

    @Name("Список объявлений пресета»")
    @FindBy(".//li[contains(@class, 'CarouselUniversal__item IndexPresets__item')]")
    ElementsCollection<PresetsItem> salesList();

    @Name("Ссылка «Смотреть все»")
    @FindBy(".//a[@class = 'Index__all-link']")
    VertisElement showAllUrl();

    @Name("Кнопка «>»")
    @FindBy(".//div[contains(@class, 'CarouselUniversal__navButton_next')] | " +
            ".//div[contains(@class, 'NavigationButton_next')]")
    VertisElement nextButton();

    @Name("Кнопка «<»")
    @FindBy(".//div[contains(@class, 'CarouselUniversal__navButton_prev')] | " +
            ".//div[contains(@class, 'NavigationButton_prev')]")
    VertisElement prevButton();

    @Step("Получаем пресет с индексом {i}")
    default VertisElement getPreset(int i) {
        return presetsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Получаем объявление с индексом {i}")
    default PresetsItem getSale(int i) {
        return salesList().should(hasSize(greaterThan(i))).get(i);
    }
}