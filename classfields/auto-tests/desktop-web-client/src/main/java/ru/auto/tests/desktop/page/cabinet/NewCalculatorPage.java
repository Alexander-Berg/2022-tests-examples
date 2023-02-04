package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithNotifier;
import ru.auto.tests.desktop.element.cabinet.calculator.AuctionOnboardingPopup;
import ru.auto.tests.desktop.element.cabinet.calculator.AutostrategyPopup;
import ru.auto.tests.desktop.element.cabinet.calculator.Balance;
import ru.auto.tests.desktop.element.cabinet.calculator.CalculatorCallsLimitsPopup;
import ru.auto.tests.desktop.element.cabinet.calculator.KomTCCalculatorBlock;
import ru.auto.tests.desktop.element.cabinet.calculator.MotoCalculatorBlock;
import ru.auto.tests.desktop.element.cabinet.calculator.NewCarsCalculatorBlock;
import ru.auto.tests.desktop.element.cabinet.calculator.UsedCarsCalculatorBlock;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 05.12.18
 */
public interface NewCalculatorPage extends WebPage, WithNotifier {

    @Name("Калькулятор")
    @FindBy("//div[ ./div[@class = 'CalculatorHeader__container']]")
    VertisElement calculator();

    @Name("Тарифы на размещение объявлений и дополнительные услуги")
    @FindBy("//div[@class = 'CalculatorHeader__container']//a")
    VertisElement linkOnTariffs();

    @Name("Блок «Легковые с пробегом»")
    @FindBy("//div[@class = 'CalculatorCategories__card'][contains(., 'Легковые с\u00a0пробегом')]")
    UsedCarsCalculatorBlock usedCarsBlock();

    @Name("Блок «Легковые новые»")
    @FindBy("//div[@class = 'CalculatorCategories__card'][contains(., 'Легковые новые')]")
    NewCarsCalculatorBlock newCarsBlock();

    @Name("Блок «Коммерческий транспорт»")
    @FindBy("//div[@class = 'CalculatorCategories__card'][contains(., 'Коммерческий транспорт')]")
    KomTCCalculatorBlock komTCBlock();

    @Name("Блок «Мото»")
    @FindBy("//div[@class = 'CalculatorCategories__card'][contains(., 'Мото')]")
    MotoCalculatorBlock motoBlock();

    @Name("Блок с балансом")
    @FindBy("//div[@class = 'CalculatorBalance__container']")
    Balance balance();

    @Name("Активный поп-ап")
    @FindBy("//div[contains(@class, 'Popup_visible')]//div[@class = 'ServicePopup']")
    VertisElement activePopup();

    @Name("Поп-ап «{{ popupText }}»")
    @FindBy("//div[(contains(@class, 'notifier_visible') or contains(@class, 'Notifier')) " +
            "and contains(., '{{ popupText }}')]")
    VertisElement serviceStatusPopup(@Param("popupText") String popupText);

    @Name("Поп-ап изменения «Ограничение расхода на звонки»")
    @FindBy("//div[contains(@class, 'CallsLimits__popup') and contains(@class, 'Popup_visible')]")
    CalculatorCallsLimitsPopup callsLimitPopup();

    @Name("Поп-ап автостратегии")
    @FindBy("//div[contains(@class, 'AutostrategyPopup')]")
    AutostrategyPopup autostrategyPopup();

    @Name("Поп-ап с туром про аукцион")
    @FindBy(".//div[contains(@class, 'OnboardingPopup')]")
    AuctionOnboardingPopup auctionOnboardingPopup();

}
