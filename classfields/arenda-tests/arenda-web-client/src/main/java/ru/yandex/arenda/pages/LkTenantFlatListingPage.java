package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.arenda.element.lk.tenantlk.TenantListingFlatSnippet;
import ru.yandex.arenda.element.lk.tenantlk.ToAppPopup;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface LkTenantFlatListingPage extends BasePage {

    @Name("Попап оплаты в приложении")
    @FindBy("//div[contains(@class,'InstallMobileAppModal__popup')]")
    ToAppPopup toAppPopup();

    @Name("Квартиры собственника")
    @FindBy("//div[contains(@class,'UserFlatSnippet__container')]")
    ElementsCollection<TenantListingFlatSnippet> tenantFlatSnippets();

    @Name("Мэйн контент")
    @FindBy("//main")
    AtlasWebElement mainContent();

    default TenantListingFlatSnippet firstTenantSnippet() {
        return tenantFlatSnippets().waitUntil(hasSize(greaterThan(0))).get(0);
    }
}
