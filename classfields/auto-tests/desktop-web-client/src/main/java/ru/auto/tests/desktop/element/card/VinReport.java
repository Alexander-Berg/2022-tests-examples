package ru.auto.tests.desktop.element.card;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface VinReport extends VertisElement, WithButton {

    String SHOW_FREE_REPORT = "Показать бесплатный отчёт";
    String BUY_FULL_REPORT = "Купить полный отчёт";

    @Name("Поле ввода комментария")
    @FindBy(".//textarea")
    VertisElement commentInput();

    @Name("Кнопка отправки комментария")
    @FindBy(".//button[.//*[contains(@class, 'VinReportCommentForm__sendButton')]]")
    VertisElement sendCommentButton();

    @Name("Список комментариев")
    @FindBy(".//div[@class = 'VinReportComments__comment']")
    ElementsCollection<CardReportComment> commentsList();

    @Name("Список пунктов для комментирования")
    @FindBy(".//div[@class = 'VinReportFreeBlockItem__line_comment']")
    ElementsCollection<CardReportComment> pointFreeCommentHistory();

    @Step("Получаем комментарий с индексом «{i}»")
    default CardReportComment getComment(int i) {
        return commentsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Получаем пункт для комментирования с индексом «{i}»")
    default CardReportComment getPointFreeHistory(int i) {
        return pointFreeCommentHistory().should(hasSize(greaterThan(i))).get(i);
    }
}
