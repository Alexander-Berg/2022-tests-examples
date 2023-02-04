package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.listing.CreditsBanner;
import ru.auto.tests.desktop.element.lk.CreditsForm;

public interface WithCredit {

    @Name("Баннер кредитов в листинге")
    @FindBy("//div[contains(@class, 'CreditCrossBannerDumb')]")
    CreditsBanner creditBanner();

    @Name("Поп-ап короткой заявки на кредит")
    @FindBy("//div[contains(@class, 'Modal_visible')][.//div[contains(@class, 'CreditApplicationForm')]]")
    CreditsForm creditApplicationPopup();

}
