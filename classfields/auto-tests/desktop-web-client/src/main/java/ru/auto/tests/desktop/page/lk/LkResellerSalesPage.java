package ru.auto.tests.desktop.page.lk;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithActivatePopup;
import ru.auto.tests.desktop.component.WithReviewsPromo;
import ru.auto.tests.desktop.component.WithShare;
import ru.auto.tests.desktop.component.WithSoldPopup;
import ru.auto.tests.desktop.element.cabinet.WithMenuPopup;
import ru.auto.tests.desktop.element.lk.reseller.CallHistoryPopup;
import ru.auto.tests.desktop.element.lk.reseller.EditPricePopup;
import ru.auto.tests.desktop.element.lk.reseller.Filters;
import ru.auto.tests.desktop.element.lk.reseller.MoreMenu;
import ru.auto.tests.desktop.element.lk.reseller.ProfessionalSellerBanner;
import ru.auto.tests.desktop.element.lk.reseller.ProfessionalSellerPopup;
import ru.auto.tests.desktop.element.lk.reseller.ProfessionalSellerTooltip;
import ru.auto.tests.desktop.element.lk.reseller.ResellerSalesListItem;
import ru.auto.tests.desktop.page.BasePage;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface LkResellerSalesPage extends BasePage, WithShare, WithMenuPopup, WithReviewsPromo, WithSoldPopup, WithActivatePopup {

    @Name("Список объявлений пользователя")
    @FindBy("//div[@class='ResellerSalesItem']")
    ElementsCollection<ResellerSalesListItem> salesList();

    @Name("Поп-ап редактирования цены")
    @FindBy("//div[contains(@class, 'SalesItemPriceNewDesign__popup')]")
    EditPricePopup editPricePopup();

    @Name("Поп-ап истории звонков")
    @FindBy(".//div[contains(@class, 'OfferCallHistoryModal')]")
    CallHistoryPopup callHistoryPopup();

    @Name("Поп-ап профессионального продавца")
    @FindBy(".//div[contains(@class, 'Modal_visible')][.//div[contains(@class, 'ResellerPublicProfilePromoModal')]]")
    ProfessionalSellerPopup professionalSellerPopup();

    @Name("Баннер профессионального продавца")
    @FindBy(".//div[contains(@class, 'ResellerPublicProfilePromo')]")
    ProfessionalSellerBanner proffessionalSellerBanner();

    @Name("Тултип профессионального продавца")
    @FindBy(".//div[@class='ResellerPublicProfilePromoTooltip']")
    ProfessionalSellerTooltip professionalSellerTooltip();

    @Name("Вкладка «{{ text }}»")
    @FindBy(".//span[contains(@class, 'PageResellerSales__tabs')]//button[.='{{ text }}']")
    VertisElement tab(@Param("text") String text);

    @Name("Фильтры объявлений")
    @FindBy(".//div[@class='SalesFiltersNewDesign']")
    Filters filters();

    @Name("Залипающие фильтры объявлений")
    @FindBy(".//div[contains(@class, 'SalesFiltersNewDesign__stickyContainer_on')]")
    Filters filtersSticky();

    @Name("Меню доп. действий")
    @FindBy("//div[contains(@class, '_moreMenu')]")
    MoreMenu moreMenu();

    @Step("Получаем объявление с индексом {i}")
    default ResellerSalesListItem getSale(int i) {
        return salesList().should(hasSize(greaterThan(i))).get(i);
    }

}
