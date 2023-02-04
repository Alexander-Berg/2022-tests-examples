package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithGeoSuggest;
import ru.auto.tests.desktop.element.DealFormSection;

public interface DealPage extends BasePage, WithGeoSuggest {

    @Name("Заголовок")
    @FindBy("//div[@class='PageDeal__title']")
    VertisElement title();

    @Name("Секция формы «{{ title }}»")
    @FindBy("//div[contains(@class, 'PageDealSection ') and .//div[contains(@class, 'PageDealSection__title') and .='{{ title }}']]")
    DealFormSection section(@Param("title") String title);

    @Name("Секция «Успех»")
    @FindBy("//div[@class='PageDealSuccess']")
    VertisElement success();

    @Name("QR-код")
    @FindBy("//canvas[@class='DealBankDetails__qrCode']")
    VertisElement qrCode();
}
