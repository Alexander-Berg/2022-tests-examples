package ru.auto.tests.desktop.element.lk.reseller;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithAutoFreshPopup;
import ru.auto.tests.desktop.component.WithAutoProlongFailedBanner;
import ru.auto.tests.desktop.component.WithAutoProlongInfo;
import ru.auto.tests.desktop.component.WithAutoProlongPopup;
import ru.auto.tests.desktop.component.WithAutoruOnlyBadge;
import ru.auto.tests.desktop.component.WithButton;

public interface ResellerSalesListItem extends VertisElement, WithAutoFreshPopup, WithAutoProlongPopup, WithAutoruOnlyBadge,
        WithAutoProlongInfo, WithAutoProlongFailedBanner, WithButton {

    @Name("Цена")
    @FindBy(".//div[contains(@class, 'SalesItemPriceNewDesign')]")
    Price price();

    @Name("Фото")
    @FindBy(".//div[contains(@class, 'ResellerSalesItemPhoto')]")
    Photo photo();

    @Name("Колонка главной информации")
    @FindBy(".//div[contains(@class, 'ResellerSalesItemMainInfo__column_main')]")
    MainInfoColumn mainInfoColumn();

    @Name("Колонка с датами")
    @FindBy(".//div[contains(@class, 'ResellerSalesItemMainInfo__column_dates')]")
    DatesColumn datesColumn();

    @Name("Колонка с кнопками управления")
    @FindBy(".//div[contains(@class, 'ResellerSalesItem__controls')]")
    ControlsColumn controlsColumn();

    @Name("Статус")
    @FindBy(".//div[contains(@class, 'ResellerSalesItemStatus')]")
    Status status();

    @Name("График")
    @FindBy(".//div[contains(@class, 'SalesStats_type_reseller')]")
    VertisElement chart();

    @Name("Скрыть/показать график")
    @FindBy(".//span[contains(@class, 'ResellerSalesItemMainInfo__statsToggle')]")
    VertisElement chartToggleButton();

    @Name("Услуга «{{ vasName }}»")
    @FindBy(".//div[contains(@class, 'ResellerSalesItemVas') and .//div[contains(., '{{ vasName }}')]]")
    VertisElement vas(@Param("vasName") String vasName);
}
