package ru.auto.tests.desktop.mobile.element.mobilereviews;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Comment extends VertisElement {

    @Name("Id комментария")
    @FindBy(".//div[contains(@class, 'ReviewCommentsItem__anchor')]")
    VertisElement id();

    @Name("Текст комментария")
    @FindBy(".//span[contains(@class, 'ReviewCommentsItem__message')]")
    VertisElement text();

    @Name("Поле ввода ответа на комментарий")
    @FindBy(".//div[contains(@class, 'ReviewCommentsForm')]//textarea")
    VertisElement replyText();

    @Name("Кнопка отправки ответа")
    @FindBy(".//div[contains(@class, '__textareaIcon')]")
    VertisElement sendReplyButton();

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//span[.= '{{ text }}']")
    VertisElement button(@Param("text") String Text);

    @Name("Список ответов на комментарий")
    @FindBy(".//div[contains(@class, 'ReviewCommentsItem__wrapper--mobile')]")
    ElementsCollection<Comment> repliesList();

    @Step("Получаем ответ с индексом {i}")
    default Comment getReply(int i) {
        return repliesList().should(hasSize(greaterThan(i))).get(i);
    }
}
