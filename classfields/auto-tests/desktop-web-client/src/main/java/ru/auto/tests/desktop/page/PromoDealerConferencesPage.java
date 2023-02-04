package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PromoDealerConferencesPage extends PromoDealerPage {

    @Name("Описание")
    @FindBy("//p[contains(@class, 'BasePromoPage__lead_indent_small')]")
    VertisElement description();

    @Name("Список прошедших конференций")
    @FindBy(".//li[@class = 'PromoPageAboutConferences__card']")
    ElementsCollection<VertisElement> conferencesList();

    @Name("Форма «Заказать мероприятие»")
    @FindBy("//div[@id = 'promo-page-form']")
    VertisElement orderForm();
}
