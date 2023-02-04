package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.mag.AddToGarageBubble;
import ru.auto.tests.desktop.mobile.element.mag.Comments;
import ru.auto.tests.desktop.mobile.element.mag.Sales;

public interface MagPage extends BasePage {

    String TEST_ARTICLE = "/testovaya-statya-so-vsemi-blokami-dlya-testirovaniya/";

    @Name("Комментарии")
    @FindBy("//div[@id = 'comments']")
    Comments comments();

    @Name("Объявления")
    @FindBy("//div[contains(@class, 'OffersCarouselLazy')]")
    Sales sales();

    @Name("Характеристики")
    @FindBy("//div[contains(@class, 'TTHBlock')]")
    VertisElement tth();

    @Name("Блок голосования")
    @FindBy("//section[contains(@class, 'PollBlock')]")
    VertisElement poll();

    @Name("Бабл добавления в гараж")
    @FindBy("//div[contains(@class, 'ArticleAddGarage_visible')]")
    AddToGarageBubble addToGarageBubble();

}
