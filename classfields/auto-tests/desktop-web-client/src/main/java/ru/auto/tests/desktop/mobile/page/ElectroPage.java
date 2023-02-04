package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.element.electro.JournalSection;
import ru.auto.tests.desktop.mobile.element.electro.OffersSection;
import ru.auto.tests.desktop.mobile.element.electro.PopularModelsSection;
import ru.auto.tests.desktop.mobile.element.electro.PostsSection;
import ru.auto.tests.desktop.mobile.element.electro.ReviewSection;
import ru.auto.tests.desktop.page.BasePage;

public interface ElectroPage extends BasePage {

    @Name("Секция статей")
    @FindBy("//section[1]")
    PostsSection postsSection();

    @Name("Секция «Популярные модели электромобилей»")
    @FindBy("//section[2]")
    PopularModelsSection popularModelsSection();

    @Name("Секция «Журнал»")
    @FindBy("//section[3]")
    JournalSection journalSection();

    @Name("Секция «Электромобили в продаже»")
    @FindBy("//section[4]")
    OffersSection offersSection();

    @Name("Секция «Отзывы владельцев электромобилей»")
    @FindBy("//section[6]")
    ReviewSection reviewsSection();

    @Name("Список популярных моделей")
    @FindBy("//a[contains(@class, 'PopularModelItem')]")
    ElementsCollection<VertisElement> popularModels();

}
