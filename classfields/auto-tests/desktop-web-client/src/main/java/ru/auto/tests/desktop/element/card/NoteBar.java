package ru.auto.tests.desktop.element.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;

public interface NoteBar extends VertisElement, WithInput {

    @Name("Заметка")
    @FindBy(".//div[contains(@class, 'OfferNoteView__text')]")
    VertisElement note();

    @Name("Кнопка удаления заметки")
    @FindBy(".//span[@class = 'OfferNoteView__clear']")
    VertisElement deleteButton();

    @Name("Кнопка сохранения заметки")
    @FindBy(".//form[@class='sale-note__edit']//button | " +
            ".//button[contains(@class, 'OfferNote-module__button')] |" +
            ".//button[contains(@class, 'OfferNote__button')]")
    VertisElement saveButton();
}