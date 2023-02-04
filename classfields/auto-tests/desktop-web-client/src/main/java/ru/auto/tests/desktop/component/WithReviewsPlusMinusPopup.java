package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.ReviewsPlusMinusPopup;

public interface WithReviewsPlusMinusPopup {

    @Name("Поп-ап плюсов и минусов")
    @FindBy("//div[contains(@class, 'ReviewsFeaturesModal__content')]")
    ReviewsPlusMinusPopup reviewsPlusMinusPopup();
}