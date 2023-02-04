package ru.auto.tests.desktop.element.mag;

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

    @Name("Аватар авторизованного пользователя")
    @FindBy("//div[contains(@class, 'ReviewCommentsItemJournal__avatar_empty')]")
    VertisElement ownerAvatar();

    @Name("Блок ленивой авторизации")
    @FindBy(".//div[contains(@class, ' ReviewCommentsJournalLazyPhone')]")
    CommentAuthBlock commentAuthBlock();

    @Name("Поле ввода текста комментария")
    @FindBy(".//div[contains(@class, 'ReviewCommentsForm_global')]//textarea")
    VertisElement commentText();

    @Name("Кнопка отправки комментария первого уровня")
    @FindBy(".//div[contains(@class, 'ReviewCommentsForm_global')]" +
            "//div[contains(@class, 'ReviewCommentsFormJournal__submitBtn')]")
    VertisElement sendCommentButton();

    @Name("Активное поле ввода ответа на комментарий")
    @FindBy(".//div[contains(@class, 'TextArea_focused')]//textarea")
    VertisElement replyInput();

    @Name("Кнопка отправки комментария второго уровня")
    @FindBy("(.//div[contains(@class, 'ReviewCommentsFormJournal__submitBtn')])[last()]")
    VertisElement sendReplyButton();

    @Name("Список комментариев первого уровня")
    @FindBy(".//div[@class = 'ReviewCommentsList']//div[@class = 'ReviewCommentsItemJournal']")
    ElementsCollection<Comment> commentsListFirstLevel();

    @Name("Список комментариев второго уровня")
    @FindBy(".//div[@class = 'ReviewCommentsList']//div[@class = 'ReviewCommentsList']" +
            "//div[@class = 'ReviewCommentsItemJournal']")
    ElementsCollection<Comment> commentsListSecondLevel();

    @Step("Получаем комментарий первого уровня с индексом {i}")
    default Comment getCommentFirstLevel(int i) {
        return commentsListFirstLevel().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Получаем комментарий второго уровня с индексом {i}")
    default Comment getCommentSecondLevel(int i) {
        return commentsListSecondLevel().should(hasSize(greaterThan(i))).get(i);
    }
}