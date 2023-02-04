package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.PromoContent;
import ru.auto.tests.desktop.element.PromoSidebar;

public interface PromoPage extends BasePage {

    @Name("Содержимое страницы")
    @FindBy("//div[@class = 'content']")
    PromoContent content();

    @Name("Сайдбар")
    @FindBy("//div[contains(@class, 'Sidebar')]")
    PromoSidebar sidebar();
}