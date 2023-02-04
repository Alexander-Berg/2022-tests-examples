package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.mag.AddToGarageBubble;
import ru.auto.tests.desktop.element.mag.Comments;
import ru.auto.tests.desktop.element.mag.Navigation;
import ru.auto.tests.desktop.element.mag.Sales;

public interface MagPage extends BasePage {

    String MARK = "bmw";
    String MAG_AD_CAROUSEL = "mag.ad-carousel";
    String WITH_DRAFT_MODEL = "withDraftModel";

    @Name("Комментарии")
    @FindBy("//div[@id = 'reviewComments']")
    Comments comments();

    @Name("Объявления")
    @FindBy("//div[contains(@class, 'OffersCarouselLazy')]")
    Sales sales();

    @Name("Навигация")
    @FindBy("//nav[contains(@class, 'ListingTagNavigation')]")
    Navigation navigation();

    @Name("Характеристики")
    @FindBy("//div[contains(@class, 'TTHBlock')]")
    VertisElement tth();

    @Name("Бабл добавления в гараж")
    @FindBy("//div[contains(@class, 'ArticleAddGarageContent__popup_visible')]")
    AddToGarageBubble addToGarageBubble();

    @Name("Иконка добавления в гараж")
    @FindBy("//div[@class = 'ArticleAddGarageContent']")
    VertisElement addToGarageIcon();

}
