package ru.auto.tests.desktop.component.desktopreviews;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.desktopreviews.ListingReview;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface WithReviewsListing {

    @Name("Список отзывов")
    @FindBy(".//div[contains(@class, 'ReviewsList__item')]")
    io.qameta.atlas.webdriver.ElementsCollection<ListingReview> reviewsList();

    @Name("Кнопка «Смотреть все»")
    @FindBy(".//div[contains(@class, 'PageReviewsIndex__all')] | " +
            ".//a[contains(@class, '__showAllButton')]")
    VertisElement showAllButton();

    @Name("Первый сниппет журнала")
    @FindBy(".//div[contains(@class, 'ReviewsList__item')][2]" +
            "/following-sibling::div[contains(@class, 'JournalSnippet__wrapper')]")
    VertisElement firstJournalSnippet();

    @Name("Второй сниппет журнала")
    @FindBy(".//div[contains(@class, 'ReviewsList__item')][4]" +
            "/following-sibling::div[contains(@class, 'JournalSnippet__wrapper')]")
    VertisElement secondJournalSnippet();

    @Step("Получаем отзыв с индексом {i}")
    default ListingReview getReview(int i) {
        return reviewsList().should(hasSize(greaterThan(i))).get(i);
    }
}