package ru.auto.tests.desktop.element.poffer.beta;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface BetaBadgesBlock extends VertisElement, WithButton, WithInput {

    String CUSTOM_TEXT_INPUT = "Укажите свой текст";
    String ADD_CUSTOM_BADGE_BUTTON = "Добавить свой";
    String ADD_BUTTON = "Добавить";

    @Name("Информация про цену и доступное количество стикеров")
    @FindBy(".//div[@class='QuickSaleBadges__summary']")
    VertisElement summary();
}