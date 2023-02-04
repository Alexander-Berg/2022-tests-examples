package ru.auto.tests.desktop.mobile.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.lk.AuctionListItem;
import ru.auto.tests.desktop.mobile.element.lk.ActualizationBlock;
import ru.auto.tests.desktop.mobile.element.lk.ProfessionalSellerBanner;
import ru.auto.tests.desktop.mobile.element.lk.ProfessionalSellerPopup;
import ru.auto.tests.desktop.mobile.element.lk.ReasonToRemoveFromSalePopup;
import ru.auto.tests.desktop.mobile.element.lk.SalesItem;
import ru.auto.tests.desktop.mobile.element.lk.SameSalePopup;
import ru.auto.tests.desktop.mobile.element.lk.Stub;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 29.01.18
 */
public interface LkPage extends BasePage {

    String ACTUAL_MSG = "Актуально\n?";
    String NEED_UPDATE_MSG = "Да, продаю\n?";
    String PROFILE = "Профиль";

    String ACTUALIZE_POPUP_TEXT = "Объявление актуально\n" +
            "Подтвердите актуальность своего объявления. Делать это можно каждый день — тогда ваше объявление " +
            "при поиске всегда будет находиться выше неактуализированных и его увидят больше покупателей.";

    String INFO_POPUP_TEXT = "Актуальность объявлений\nПодтвердите актуальность своего объявления. Делать это можно " +
            "каждый день — тогда ваше объявление при поиске всегда будет находиться выше неактуализированных и его " +
            "увидят больше покупателей.";

    @Name("Список объявлений")
    @FindBy("//div[contains(@class, 'SalesItem')]")
    ElementsCollection<SalesItem> salesList();

    @Name("Блок актуализации")
    @FindBy("//div[@class = 'SalesItem__actual']")
    ActualizationBlock actualizationBlock();

    @Name("Поп-ап «Причина снятия с продажи»")
    @FindBy("//div[contains(@class, 'Modal_visible')]//div[contains(@class, 'Modal__content')]")
    ReasonToRemoveFromSalePopup reasonToRemoveFromSalePopup();

    @Name("Заглушка")
    @FindBy("//div[contains(@class, 'SalesEmptyList')]")
    Stub stub();

    @Name("Поп-ап активации похожего объявления")
    @FindBy("//div[contains(@class, 'SameSellPopup')]")
    SameSalePopup sameSalePopup();

    @Name("Попап профессионального продавца")
    @FindBy("//div[contains(@class, 'Modal_visible')][.//div[contains(@class, 'ResellerPublicProfilePromoPopupMobile')]]")
    ProfessionalSellerPopup professionalSellerPopup();

    @Name("Баннер «Получайте больше актуальных звонков»")
    @FindBy("//div[contains(@class, 'ResellerPublicProfilePromoMobile')]")
    ProfessionalSellerBanner proffessionalSellerBanner();

    @Name("Список аукционных заявок пользователя")
    @FindBy("//div[contains(@class, 'CtbAuctionItemOld')] |" +
            "//div[@class = 'C2bAuctionItem'] |" +
            "//div[contains(@class, 'C2bAuctionItem_accepted')]|" +
            "//div[contains(@class, 'C2bAuctionItem_rejected')]")
    ElementsCollection<AuctionListItem> auctionList();

    @Step("Получаем объявление с индексом {i}")
    default SalesItem getSale(int i) {
        return salesList().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Получаем заявку с индексом «{i}»")
    default AuctionListItem getApplication(int i) {
        return auctionList().should(hasSize(greaterThan(i))).get(i);
    }

}
