package ru.auto.tests.desktop.mobile.element.main;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Stories extends VertisElement {

    @Name("Список историй")
    @FindBy(".//li[contains(@class, 'Stories__story')]")
    ElementsCollection<VertisElement> storiesList();

    @Name("Просмотренная история")
    @FindBy(".//li[contains(@class, 'StoryPreview_viewed')]")
    VertisElement viewedStory();

    @Step("Получаем историю с индексом {index}")
    default VertisElement getStory(int index) {
        return storiesList().should(hasSize(greaterThan(index))).get(index);
    }
}