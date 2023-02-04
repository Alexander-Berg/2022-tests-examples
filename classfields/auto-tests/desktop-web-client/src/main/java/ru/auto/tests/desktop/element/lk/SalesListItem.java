package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithAutoFreshPopup;
import ru.auto.tests.desktop.component.WithAutoProlongFailedBanner;
import ru.auto.tests.desktop.component.WithAutoProlongInfo;
import ru.auto.tests.desktop.component.WithAutoProlongPopup;
import ru.auto.tests.desktop.component.WithAutoruOnlyBadge;

public interface SalesListItem extends VertisElement, WithAutoFreshPopup, WithAutoProlongPopup, WithAutoruOnlyBadge,
        WithAutoProlongInfo, WithAutoProlongFailedBanner {

    String DELETE = "Удалить";
    String LK_EDIT = "Редактировать";
    String ACTIVATE = "Активировать";
    String DEACTIVATE = "Снять с продажи";

    @Name("Заголовок")
    @FindBy(".//a[contains(@class, 'SalesItemTitle')]")
    VertisElement title();

    @Name("Иконка платности")
    @FindBy(".//span[contains(@class, 'PaidOffer')]")
    VertisElement paidIcon();

    @Name("Статус")
    @FindBy(".//div[contains(@class, 'SalesItem__warning')]")
    VertisElement status();

    @Name("Кнопка актуализации")
    @FindBy(".//button[contains(@class, 'SalesItem__buttonActualize')]")
    VertisElement actualizeButton();

    @Name("Кнопка «{{ text }}»")
    @FindBy(".//a[.='{{ text }}'] |" +
            ".//span[.='{{ text }}']")
    VertisElement button(@Param("text") String text);

    @Name("Поле ввода цены")
    @FindBy(".//input")
    VertisElement priceInput();

    @Name("Кнопка сохранения цены")
    @FindBy(".//button[contains(@class, 'SalesItemPrice__save')]")
    VertisElement savePriceButton();

    @Name("Иконка услуги «Выделение цветом»")
    @FindBy(".//div[contains(@class, 'SalesItemPrice__inputContainer')]/following-sibling::div[contains(@class, 'SalesItemVASColor')]")
    VertisElement colorIcon();

    @Name("Индикатор подключенной услуги «Выделение цветом»")
    @FindBy(".//label[contains(@class, 'SalesItemPrice__input_color')]")
    VertisElement activatedColorIcon();

    @Name("Индикатор подключенной услуги «Топ»")
    @FindBy(".//div[contains(@class, 'SalesItemVASTop')]")
    VertisElement activatedTopIcon();

    @Name("Иконка «Сердечко» со счетчиком общего количества добавления в избарнное")
    @FindBy(".//div[contains(@class, 'SalesItemGraph__infoColumn_favorites')]")
    VertisElement favoriteCounter();

    @Name("Кнопка «Поднять за ...»")
    @FindBy(".//button[.//div[contains(@class, 'SalesItemVASFresh__texts')]]")
    VertisElement freshButton();

    @Name("График")
    @FindBy(".//div[contains(@class, 'SalesChart')]")
    Chart chart();

    @Name("Блок услуг")
    @FindBy(".//div[@class = 'Vas']")
    Vas vas();

    @Name("Ошибка панорамы")
    @FindBy(".//a[contains(@class, 'PanoramaProcessingError')]")
    VertisElement panoramaError();

    @Name("Кнопка «Добавить панораму»")
    @FindBy(".//div[contains(@class, 'PanoramaPromoAddInApp')]")
    VertisElement addPanoramaButton();

    @Name("Блок безопасной сделки")
    @FindBy(".//div[@class='SalesItemSafeDeal']")
    SafeDealBlock safeDealBlock();

    @Name("Ссылка")
    @FindBy(".//a")
    VertisElement link();

}
