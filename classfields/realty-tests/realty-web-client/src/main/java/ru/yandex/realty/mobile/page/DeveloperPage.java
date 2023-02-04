package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.mobile.element.Link;
import ru.yandex.realty.mobile.element.developer.Callback;
import ru.yandex.realty.mobile.element.developer.Slide;

public interface DeveloperPage extends BasePage {

    @Name("Обратный звонок")
    @FindBy("//div[contains(@class, 'backCall')]")
    Callback callback();

    @Name("Слайды")
    @FindBy("//a[contains(@class, 'slide')]")
    ElementsCollection<Slide> slides();

    @Name("Табы")
    @FindBy("//div[contains(@class, 'tab-')]")
    ElementsCollection<Link> tabs();

    @Name("Телефон")
    @FindBy("//a[@data-test='PhoneButton']")
    AtlasWebElement call();

    @Name("Сниппет новостройки")
    @FindBy(".//li[contains(@class,'SitesSerpItem')]")
    Link newBuildingSnippet();

}
