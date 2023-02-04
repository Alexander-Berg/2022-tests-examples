package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.ButtonWithTitle;

public interface SimilarSitesTouchOffer extends ButtonWithTitle, Link {

    @Name("Ссылка похожего ЖК")
    @FindBy(".//div[contains(@class,'SerpListItem SerpListItem')]//a[contains(@class, 'SiteSnippetSearch__gallery')]")
    AtlasWebElement offerLink();

    @Name("Добавить в избранное")
    @FindBy(".//div[contains(@class,'SerpFavoriteAction SiteSnippetSearch__favoriteButton')]")
    AtlasWebElement addToFav();

    @Name("Тайтл")
    @FindBy(".//p[contains(@class,'SiteSnippetSearch__name')]")
    AtlasWebElement title();
}
