package ru.auto.tests.desktop.element.cabinet.calculator;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Auction extends VertisElement {

    @Name("Текущая позиция")
    @FindBy("./td[contains(@class, 'AuctionTableItem__col_position')]/div[contains(@class, 'AuctionTableItem__position')]")
    VertisElement currentPosition();

    @Name("Кнопка понизить ставку")
    @FindBy("./td[contains(@class, 'AuctionTableItem__col_control')][1]/button")
    VertisElement minusBetButton();

    @Name("Текущая ставка")
    @FindBy("./td[contains(@class, 'AuctionTableItem__col_currentBet')]")
    VertisElement currentBet();

    @Name("Кнопка повысить ставку")
    @FindBy("./td[contains(@class, 'AuctionTableItem__col_control')][2]/button")
    VertisElement plusBetButton();

    @Name("Ставка для первого места")
    @FindBy("./td[contains(@class, 'AuctionTableItem__col_firstPlaceBet')]")
    FirstPlaceBetButton firstPlaceBet();

    @Name("Снять с аукциона")
    @FindBy("./td[contains(@class, 'AuctionTableItem__col_control')][3]/button")
    VertisElement leaveAuctionButton();

    @Name("Кнопка автостратегии")
    @FindBy(".//button[contains(@class, '_autostrategyButton')]")
    VertisElement autostrategyButton();

}
