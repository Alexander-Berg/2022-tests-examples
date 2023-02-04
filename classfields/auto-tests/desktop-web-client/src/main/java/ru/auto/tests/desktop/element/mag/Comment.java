package ru.auto.tests.desktop.element.mag;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Comment extends VertisElement {

    @Name("Текст комментария")
    @FindBy(".//div[contains(@class, 'ReviewCommentsItemJournal__message')]")
    VertisElement text();

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//span[.= '{{ text }}']")
    VertisElement button(@Param("text") String Text);

    @Name("Список ответов на комментарий")
    @FindBy(".//div[@class = 'ReviewCommentsItemJournal']")
    ElementsCollection<Comment> repliesList();

}
