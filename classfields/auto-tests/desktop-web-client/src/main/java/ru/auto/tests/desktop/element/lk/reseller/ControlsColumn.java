package ru.auto.tests.desktop.element.lk.reseller;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface ControlsColumn extends VertisElement, WithButton {

    @Name("Иконка редактирования")
    @FindBy("./a[contains(@href, '/edit/')]")
    VertisElement editIcon();

    @Name("Иконка снятия с продажи")
    @FindBy("./span")
    VertisElement hideIcon();

    @Name("Иконка «Больше»")
    @FindBy("./div[contains(@class, 'ResellerSalesItem__controlWrapper_more')]")
    VertisElement moreIcon();

}
