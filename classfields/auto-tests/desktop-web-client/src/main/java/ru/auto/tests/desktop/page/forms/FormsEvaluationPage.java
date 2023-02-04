package ru.auto.tests.desktop.page.forms;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.HorizontalCarousel;
import ru.auto.tests.desktop.element.forms.EvaluationResult;

public interface FormsEvaluationPage extends FormsPage {

    @Name("Форма")
    @FindBy("//div[@class = 'EvaluationPage']")
    VertisElement form();

    @Name("Результат оценки")
    @FindBy("//div[@class = 'EvaluationPage__result']")
    EvaluationResult evaluationResult();

    @Name("Блок «Новые авто с учётом вашего в трейд-ин»")
    @FindBy("//div[contains(@class, 'CarouselNewForTradeIn')]")
    HorizontalCarousel newForTradeIn();
}
