package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FavoritesPage extends ListingPage {

    String DELETE_ONLY_THIS = "Удалить только это";

    @Name("Заглушка")
    @FindBy("//div[@class = 'LikePage__empty'] | " +
            "//div[@class = 'PageLike__empty'] | " +
            "//div[@class = 'PageLikeOther__empty']")
    VertisElement stub();

    @Name("Кнопка «Войти»")
    @FindBy("//a[contains(@class, 'Button_color_blue')]")
    VertisElement loginButton();
}