package ru.auto.tests.desktop.element.desktopreviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface StickyHeader extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'ReviewStickyHeader__title')]")
    VertisElement title();

    @Name("Кнопка «N комментариев»")
    @FindBy(".//div[contains(@class, 'ReviewStickyHeader__comments')]")
    VertisElement commentsButton();

    @Name("Ненажатая кнопка «Палец вверх»")
    @FindBy(".//button[contains(@class, 'ReviewRate__voteUp') and not(contains(@class, 'Button_hovered'))]")
    VertisElement unpressedUpButton();

    @Name("Нажатая кнопка «Палец вверх»")
    @FindBy(".//button[contains(@class, 'Button_hovered ReviewRate__voteUp')]")
    VertisElement pressedUpButton();

    @Name("Ненажатая кнопка «Палец вниз»")
    @FindBy(".//button[contains(@class, 'ReviewRate__voteDown') and not(contains(@class, 'Button_hovered'))]")
    VertisElement unpressedDownButton();

    @Name("Нажатая кнопка «Палец вниз»")
    @FindBy(".//button[contains(@class, 'Button_hovered ReviewRate__voteDown')]")
    VertisElement pressedDownButton();
}