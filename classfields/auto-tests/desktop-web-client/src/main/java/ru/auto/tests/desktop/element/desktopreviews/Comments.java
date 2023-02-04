package ru.auto.tests.desktop.element.desktopreviews;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithPager;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Comments extends VertisElement, WithPager, WithInput {

    int COMMENTS_PER_PAGE = 25;

    @Name("Поле ввода текста комментария")
    @FindBy(".//textarea")
    VertisElement commentText();

    @Name("Кнопка отправки комментария")
    @FindBy(".//div[contains(@class, '__textareaIcon')]")
    VertisElement sendCommentButton();

    @Name("Список комментариев")
    @FindBy("./div/div[@class = 'ReviewCommentsList']/div[@class = 'ReviewCommentsItem__wrapper']")
    ElementsCollection<Comment> commentsList();

    @Name("Подсказка")
    @FindBy(".//span[contains(@class, 'ReviewCommentsForm__helper')]")
    VertisElement helper();

    @Step("Получаем комментарий с индексом {i}")
    default Comment getComment(int i) {
        return commentsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Список комментариев за исключением удалённых")
    @FindBy("./div/div[@class = 'ReviewCommentsList']/div[@class = 'ReviewCommentsItem__wrapper' " +
            "and ./div[@class != 'ReviewCommentsItem_deleted ReviewCommentsItem']]")
    ElementsCollection<Comment> nonDeletedCommentsList();

    @Step("Получаем комментарий с индексом {i}")
    default Comment getNonDeletedComment(int i) {
        return nonDeletedCommentsList().should(hasSize(greaterThan(i))).get(i);
    }
}