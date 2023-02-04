package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.page.BasePage;

public interface EgrnReportPage extends BasePage {

    @Name("Ошибка формирования отчета")
    @FindBy(".//div[contains(@class,'EGRNPaidReportErrorScreen__container')]")
    AtlasWebElement errorPage();

    @Name("Заголовок «{{ value }}»")
    @FindBy(".//h2[contains(@class,'EGRNPaidReportNavigatableSection__title')]")
    AtlasWebElement h2Title(@Param("value") String value);

    @Name("Блок с заголовком «{{ value }}»")
    @FindBy(".//div[contains(@class,'EGRNPaidReportBlock__container')][contains(., '{{ value }}')]")
    Link block(@Param("value") String value);

    @Name("Секция сбоку «{{ value }}»")
    @FindBy(".//div[contains(@class,'EGRNPaidReportSideNavigator__sections')]/a[contains(.,'{{ value }}')]")
    AtlasWebElement section(@Param("value") String value);

    @Name("Подсказка в обременениях «{{ value }}»")
    @FindBy(".//span[contains(@class,'OfferEGRNEncumbranceTile__title')][contains(.,'{{ value }}')]/span")
    AtlasWebElement hint(@Param("value") String value);

    @Name("Попап")
    @FindBy(".//div[contains(@class,'Popup_visible')]")
    AtlasWebElement popupVisible();
}
