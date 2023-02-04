package ru.auto.tests.desktop.page.cabinet;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import org.hamcrest.Matchers;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.cabinet.PopupBillingBlock;
import ru.auto.tests.desktop.page.BasePage;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 2019-02-21
 */
public interface StartPage extends BasePage {

    @Name("Стартовая страница")
    @FindBy("//div[@class = 'Start']")
    VertisElement startPage();

    @Name("Поп-ап пополнения счета")
    @FindBy("//div[contains(@class, 'BalanceRechargeForm__container')]")
    PopupBillingBlock popupBillingBlock();

    @Name("Селект выбора плательщика")
    @FindBy(".//div[contains(@class, 'MenuItem') and contains(., '{{ value }}')]")
    VertisElement selectPayer(@Param("value") String value);

    @Name("Подключенные тарифы")
    @FindBy("//div[contains(@class, 'Popup_visible')]//a[contains(@class, 'Start__dropdown__item')]")
    ElementsCollection<VertisElement> tariffs();

    @Step("Выбираем первый из подключенных тарифов")
    default VertisElement firstTariff() {
        return tariffs().should(Matchers.hasSize(Matchers.greaterThan(0))).get(0);
    }
}
