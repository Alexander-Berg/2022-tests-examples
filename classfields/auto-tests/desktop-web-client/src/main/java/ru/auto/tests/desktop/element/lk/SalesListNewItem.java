package ru.auto.tests.desktop.element.lk;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithAutoFreshPopup;
import ru.auto.tests.desktop.component.WithAutoProlongFailedBanner;
import ru.auto.tests.desktop.component.WithAutoProlongPopup;
import ru.auto.tests.desktop.component.WithAutoruOnlyBadge;
import ru.auto.tests.desktop.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface SalesListNewItem extends VertisElement, WithAutoFreshPopup, WithAutoProlongPopup, WithAutoruOnlyBadge,
        WithAutoProlongFailedBanner, WithButton {

    String DELETE = "Удалить";
    String ACTIVATE = "Активировать";
    String PUBLISH_OFFER = "Опубликовать объявление";
    String IGNORE_TIMER_LOCATOR = "//div[@class = 'Timer OwnerVasItemDiscount__timeLeft']";
    String IGNORE_PANORAMA_LOCATOR = "//div[@class = 'PanoramaPromoAddInApp__anchor']";

    @Name("Заголовок")
    @FindBy(".//a[contains(@class, 'SalesItemNewDesign__vehicleTitle')]")
    VertisElement title();

    @Name("Статус")
    @FindBy(".//div[contains(@class, 'SalesItemNewDesignPhoto__badge')]")
    VertisElement status();

    @Name("Кнопка актуализации")
    @FindBy(".//button[contains(@class, 'ButtonActualize')]")
    VertisElement actualizeButton();

    @Name("Кнопка «Активировать»")
    @FindBy(".//span[contains(@class,'SalesItemNewDesignControls__itemWrapper')]//*[contains(@class,'IconSvg_views-16')]")
    VertisElement activateButton();

    @Name("Иконка «Сердечко» со счетчиком общего количества добавления в избарнное")
    @FindBy(".//div[contains(@class, 'OwnerVasBlock__statsInfoItem_favorites')]")
    VertisElement favoriteCounter();

    @Name("График")
    @FindBy(".//div[contains(@class, 'SalesStats__content')]")
    ChartNew chart();

    @Name("Блок услуг")
    @FindBy(".//div[@class = 'OwnerVasBlock__vasContainer']")
    VertisElement vas();

    @Name("Список услуг")
    @FindBy(".//div[contains(@class, 'OwnerVasItem__wrapper')]")
    ElementsCollection<VertisElement> vasList();

    @Name("Услуга «{{ text }}»")
    @FindBy(".//div[@class = 'OwnerVasItem__wrapper']" +
            "[.//div[@class = 'OwnerVasItem__description' and contains(., '{{ text }}')]]")
    VertisElement vasItem(@Param("text") String text);

    @Name("Ошибка панорамы")
    @FindBy(".//a[contains(@class, 'PanoramaProcessingError')]")
    VertisElement panoramaError();

    @Name("Кнопка «Добавить панораму»")
    @FindBy(".//div[contains(@class, 'PanoramaPromoAddInApp__anchor')]")
    VertisElement addPanoramaButton();

    @Name("Блок безопасной сделки")
    @FindBy(".//div[@class='SalesItemSafeDeal']")
    SafeDealBlock safeDealBlock();

    @Name("Кнопка редактирования")
    @FindBy(".//a[contains(@class,'SalesItemNewDesignControls__itemWrapper')]//*[contains(@class,'IconSvg_edit')]")
    VertisElement editButton();

    @Name("Кнопка удаления")
    @FindBy(".//span[contains(@class,'SalesItemNewDesignControls__itemWrapper')]//*[contains(@class,'IconSvg_delete_forever')]")
    VertisElement deleteButton();

    @Name("Кнопка снятие с продажи")
    @FindBy(".//span[contains(@class,'SalesItemNewDesignControls__itemWrapper')]//*[contains(@class,'IconSvg_hide')]")
    VertisElement deactivateButton();

    @Name("Кнопка с тремя точками")
    @FindBy(".//div[contains(@class,'Dropdown')]")
    VertisElement dotsButton();

    @Name("Кнопка редактирования цены")
    @FindBy(".//div[contains(@class,'SalesItemNewDesign__price')]//*[contains(@class, 'IconSvg_edit')]")
    VertisElement editPriceButton();

    @Name("Цена")
    @FindBy(".//div[contains(@class, 'SalesItemPriceNewDesign')]")
    VertisElement price();

    @Name("Бейдж рыночной стоимости")
    @FindBy(".//div[contains(@class,'SalesItemPriceBadgeNewDesign')]")
    VertisElement priceBadge();

    @Name("Иконка «Добавлено в избранное» (сердечко)")
    @FindBy(".//*[contains(@class, 'IconSvg_favorite')]")
    VertisElement favoriteIcon();

    @Name("Блок статуса")
    @FindBy(".//div[contains(@class, 'SalesItemNewDesignStatus')]")
    StatusBlock statusBlock();

    @Step("Получаем вас с индексом {i}")
    default VertisElement getVas(int i) {
        return vasList().should(hasSize(greaterThan(i))).get(i);
    }

}