package ru.auto.tests.desktop.element.poffer;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface BadgesBlock extends VertisElement, WithButton, WithInput {

    @Name("Список бейджей")
    @FindBy("//div[contains(@class, 'quick-sale-badges__item ')]")
    ElementsCollection<VertisElement> badgesList();

    @Name("Кнопка добавления кастомного бейджа")
    @FindBy(".//div[contains(@class, 'quick-sale-badges__show-input')]")
    VertisElement addButton();

    @Name("Кнопка сохранения бейджа")
    @FindBy(".//button[contains(@class, 'button_add-badge')]")
    VertisElement submitButton();

    @Name("Цена")
    @FindBy(".//div[contains(@class, 'quick-sale-badges__price')]")
    VertisElement price();

    @Step("Получаем бейдж с индексом {i}")
    default VertisElement getBadge(int i) {
        return badgesList().should(hasSize(greaterThan(i))).get(i);
    }

}