package ru.auto.tests.desktop.element.desktopreviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ListingReview extends VertisElement {

    @Name("Ссылка на профиль автора отзыва")
    @FindBy(".//div[contains(@class, 'ReviewSnippetDesktop__userAvatar')]")
    VertisElement authorUrl();

}
