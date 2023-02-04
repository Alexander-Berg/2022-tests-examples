package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.newbuilding.CallbackPopup;
import ru.yandex.realty.element.newbuilding.SnippetElement;
import ru.yandex.realty.element.samolet.SpecProject;
import ru.yandex.realty.element.saleads.WithNewBuildingFilters;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;


/**
 * Created by vicdev on 17.04.17.
 */
public interface NewBuildingPage extends BasePage, WithNewBuildingFilters {

    @Name("Попап обратного зонка")
    @FindBy("//div[contains(@class, 'visible') and contains(@class, 'popup')]")
    CallbackPopup callbackPopup();

    @Name("Список новостроек")
    @FindBy("//div[contains(@class, 'SiteSnippetSearch SitesSerp__snippet')]")
    ElementsCollection<SnippetElement> snippetElements();

    @Name("Инфо о застройщике")
    @FindBy("//div[contains(@class, 'DevInfo')]")
    Link devInfo();

    @Name("Банннер над листингом (на 11.22 - баннер самолета)")
    @FindBy(".//div[contains(@class,'FiltersBannerSpecialProject__specialProjectWrapper')]")
    AtlasWebElement topListingBanner();

    @Name("Спецпроект")
    @FindBy(".//div[contains(@class,'SitesSerpSpecialPinnedBlock__container')]")
    SpecProject specProject();

    @Name("Лоадер листинга")
    @FindBy(".//div[contains(@class,'SitesSerp__loader')]")
    AtlasWebElement loader();

    default SnippetElement offer(int i) {
        return snippetElements().should(hasSize(greaterThan(i))).get(i);
    }

}
