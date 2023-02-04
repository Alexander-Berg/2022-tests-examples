package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.arenda.element.common.Link;

//разные элементы с недвиги
public interface RealtyPage extends WebPage {

    @Name("Подхедер")
    @FindBy("//nav")
    Link nav();

    @Name("Главный хедер")
    @FindBy("//div[@class= 'HeaderExpandedMenu']")
    Link headerExpanded();

}
