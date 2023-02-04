package ru.yandex.realty.element.offercard;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;

public interface EgrnBlock extends Link, Button {

    String ENTER_BUTTON = "Войти";
    String BUY_FULL_REPORT = "Купить полный отчёт за";
    String SEE_BUTTON = "Смотреть";

    @Name("Купленный отчет")
    @FindBy(".//div[contains(@class,'OfferEGRNReportPurchasedTile__container')]")
    ElementsCollection<Link> purchasedReports();

    @Name("Проверить данные из ЕГРН")
    @FindBy(".//div[contains(@class,'OfferEGRNReportPaidPromotion__bannerButton')]")
    AtlasWebElement checkDataButton();
}
