package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.ReviewsPlusMinusPopup;

public interface WithReviewsPlusMinusPopup {

    @Name("Поп-ап плюсов и минусов")
    @FindBy("//div[contains(@class, 'Modal_visible')]//div[contains(@class, 'Modal__container')]")
    ReviewsPlusMinusPopup reviewsPlusMinusPopup();
}