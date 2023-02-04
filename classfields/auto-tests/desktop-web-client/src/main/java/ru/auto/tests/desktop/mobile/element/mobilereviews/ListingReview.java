package ru.auto.tests.desktop.mobile.element.mobilereviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ListingReview extends VertisElement {

    @Name("Ссылка на профиль автора отзыва")
    @FindBy(".//a[contains(@class, '__userLink')]")
    VertisElement authorUrl();

    @Name("Ссылка на отзыв")
    @FindBy(".//a[contains(@class, 'ReviewSnippetMobile__click')]")
    VertisElement reviewUrl();

}
