package ru.auto.tests.desktop.mobile.element.mobilereviews;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithPager;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Comments extends VertisElement, WithPager {

    int COMMENTS_PER_PAGE = 25;

    @Name("Поле ввода текста комментария")
    @FindBy(".//textarea")
    VertisElement commentText();

    @Name("Кнопка отправки комментария")
    @FindBy(".//div[contains(@class, '__textareaIcon')]")
    VertisElement sendCommentButton();

    @Name("Список комментариев")
    @FindBy("./div/div[@class = 'ReviewCommentsList']/div[contains(@class, 'ReviewCommentsItem__wrapper--mobile')]")
    ElementsCollection<Comment> commentsList();

    @Step("Получаем комментарий с индексом {i}")
    default Comment getComment(int i) {
        return commentsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Список комментариев за исключением удалённых")
    @FindBy("./div/div[@class = 'ReviewCommentsList']/div[contains(@class, 'ReviewCommentsItem__wrapper--mobile') " +
            "and ./div[@class != 'ReviewCommentsItem_deleted ReviewCommentsItem']]")
    ElementsCollection<Comment> nonDeletedCommentsList();

    @Step("Получаем комментарий с индексом {i}")
    default Comment getNonDeletedComment(int i) {
        return nonDeletedCommentsList().should(hasSize(greaterThan(i))).get(i);
    }

}
