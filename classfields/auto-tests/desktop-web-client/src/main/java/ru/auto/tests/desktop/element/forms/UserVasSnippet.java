package ru.auto.tests.desktop.element.forms;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface UserVasSnippet extends VertisElement {

    @Name("Опция «{{ text }}»")
    @FindBy(".//div[contains(@class, 'VasFormUserSnippetItem') and .//div[. = '{{ text }}']]")
    UserVasSnippetOption option(@Param("text") String text);

    @Name("Кнопка «Разместить»")
    @FindBy(".//button[contains(@class, 'VasFormUserSnippet__button')]")
    VertisElement submitButton();
}