package ru.auto.tests.desktop.mobile.page.mobilereviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.mobilereviews.WithAddReviewButton;
import ru.auto.tests.desktop.mobile.component.mobilereviews.WithPresets;
import ru.auto.tests.desktop.mobile.component.mobilereviews.WithReviewsListing;
import ru.auto.tests.desktop.mobile.element.mobilereviews.main.MarksBlock;
import ru.auto.tests.desktop.mobile.page.BasePage;

public interface ReviewsMainPage extends BasePage, WithAddReviewButton, WithReviewsListing, WithPresets {

    @Name("Категория «{{ category }}»")
    @FindBy(".//span[contains(@class, 'CategorySwitcher')]//label[. = '{{ category }}']")
    VertisElement categorySwitcher(@Param("category") String category);

    @Name("Блок «Отзывы по маркам»")
    @FindBy("//div[contains(@class, 'PageReviewsIndex__filtersWrapper')]")
    MarksBlock marksBlock();

    @Name("Топовый отзыв - «{{ text }}»")
    @FindBy("//div[contains(@class, 'ReviewSnippet_special') and .//div[.= '{{ text }}']]")
    VertisElement topReview(@Param("text") String text);

    @Name("Подкатегория «{{ text }}» мото/комТС")
    @FindBy("//div[contains(@class, 'PageReviewsIndex__typeSelector')]//a[.= '{{ text }}']")
    VertisElement subCategory(@Param("text") String text);
}
