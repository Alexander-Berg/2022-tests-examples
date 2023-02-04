package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ServiceButtons extends VertisElement {

    String STICKER_ACTIVE = "_sticker_active";
    String SPEC_ACTIVE = "_spec_active";
    String PREMIUM_ACTIVE = "_premium_active";
    String RECYCLE_ACTIVE = "_recycle_active";
    String TURBO_ACTIVE = "_turbo_active";
    String FRESH_ACTIVE = "_fresh_active";

    @Name("Список всех кнопок услуг")
    @FindBy(".//span[contains(@class, 'SaleButton')]")
    ElementsCollection<ServiceButton> allButtons();

    @Name("Кнопка с количеством в избранном для скидки")
    @FindBy(".//span[contains(@class, 'SaleButton')]/div[contains(@class, '_favorite')]")
    ServiceButton favorites();

    @Name("Кнопка услуги «Стикеры»")
    @FindBy(".//span[contains(@class, 'SaleButton')]/div[contains(@class, '_sticker')]")
    ServiceButton stickers();

    @Name("Кнопка услуги «Поднятие»")
    @FindBy(".//span[contains(@class, 'SaleButton')]/div[contains(@class, '_fresh')]")
    ServiceButton fresh();

    @Name("Иконка автопродления")
    @FindBy(".//div[contains(@class, '_recycle')]")
    VertisElement autocaptureIcon();

    @Name("Кнопка услуги «Спецпредложение»")
    @FindBy(".//span[contains(@class, 'SaleButton')]/div[contains(@class, '_spec')]")
    ServiceButton special();

    @Name("Кнопка услуги «Премиум»")
    @FindBy(".//span[contains(@class, 'SaleButton')]/div[contains(@class, '_premium')]")
    ServiceButton premium();

    @Name("Кнопка услуги «Турбо»")
    @FindBy(".//span[contains(@class, 'SaleButton')]/div[contains(@class, '_turbo')]")
    ServiceButton turbo();

    @Name("Аукцион")
    @FindBy(".//span[contains(@class, '_auc ')]")
    VertisElement auction();

}
