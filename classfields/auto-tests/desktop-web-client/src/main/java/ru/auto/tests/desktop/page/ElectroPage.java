package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.electro.JournalSection;
import ru.auto.tests.desktop.element.electro.OffersSection;
import ru.auto.tests.desktop.element.electro.PopularModelsSection;
import ru.auto.tests.desktop.element.electro.PostsSection;
import ru.auto.tests.desktop.element.electro.ReviewSection;

public interface ElectroPage extends BasePage {

    @Name("Секция статей")
    @FindBy("//section[@class = 'Layout'][1]")
    PostsSection postsSection();

    @Name("Секция «Популярные модели электромобилей»")
    @FindBy("//section[@class = 'Layout'][2]")
    PopularModelsSection popularModelsSection();

    @Name("Секция «Журнал»")
    @FindBy("//section[@class = 'Layout'][3]")
    JournalSection journalSection();

    @Name("Секция «Электромобили в продаже»")
    @FindBy("//section[@class = 'Layout'][4]")
    OffersSection offersSection();

    @Name("Секция «Отзывы владельцев электромобилей»")
    @FindBy("//section[@class = 'Layout'][5]")
    ReviewSection reviewsSection();

    @Name("Список популярных моделей")
    @FindBy("//a[contains(@class, 'PopularModelItem')]")
    ElementsCollection<VertisElement> popularModels();

}
