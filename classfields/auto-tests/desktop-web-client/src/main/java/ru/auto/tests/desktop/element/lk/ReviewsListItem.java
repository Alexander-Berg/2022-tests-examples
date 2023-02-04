package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface ReviewsListItem extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//a[contains(@class, 'MyReview__title')]")
    VertisElement title();

    @Name("График")
    @FindBy(".//div[contains(@class, 'SalesChart')]")
    Chart chart();

    @Name("Блок услуг")
    @FindBy("//div[@class = 'Vas']")
    Vas vas();
}
