package ru.auto.tests.desktop.element.desktopreviews;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Comment extends VertisElement, WithButton {

    @Name("Id комментария")
    @FindBy(".//div[contains(@class, 'ReviewCommentsItem__anchor')]")
    VertisElement id();

    @Name("Текст комментария")
    @FindBy(".//span[contains(@class, 'ReviewCommentsItem__message')] | " +
            ".//div[contains(@class, 'ReviewCommentsItemJournal__message')]")
    VertisElement text();

    @Name("Поле ввода ответа на комментарий")
    @FindBy("//div[@class = 'ReviewCommentsForm']//textarea")
    VertisElement replyText();

    @Name("Кнопка отправки ответа")
    @FindBy("//div[contains(@class, 'ReviewCommentsForm__textareaIcon')]")
    VertisElement sendReplyButton();

    @Name("Список ответов на комментарий")
    @FindBy(".//div[@class = 'ReviewCommentsItem__wrapper']")
    ElementsCollection<Comment> repliesList();

    @Step("Получаем ответ с индексом {i}")
    default Comment getReply(int i) {
        return repliesList().should(hasSize(greaterThan(i))).get(i);
    }
}
