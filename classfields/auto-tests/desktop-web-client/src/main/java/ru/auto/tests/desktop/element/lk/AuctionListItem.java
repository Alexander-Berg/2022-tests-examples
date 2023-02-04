package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

import static org.hamcrest.Matchers.hasSize;

public interface AuctionListItem extends VertisElement, WithButton {

    @Name("Результат аукциона")
    @FindBy("//div[@class = 'C2bAuctionItemFinished__description'] |" +
            "//div[@class = 'C2bAuctionItemOldFinished__stepDescription']")
    VertisElement auctionResult();
}
