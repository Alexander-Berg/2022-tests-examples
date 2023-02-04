package ru.auto.tests.desktop.mobile.element.header;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface Header extends VertisElement, WithButton {

    @Name("Кнопка сайдбара")
    @FindBy(".//div[contains(@class, 'header__navburger')] | " +
            ".//div[@class = 'HeaderNavButton'] | " +
            ".//button[contains(@class, 'Header__burger')] | " +
            ".//div[contains(@class, 'Header2__col-burger')]")
    VertisElement sidebarButton();

    @Name("Логотип")
    @FindBy(".//a[contains(@class, 'header__logo')] | " +
            ".//a[contains(@class, 'HeaderTitle')] | " +
            ".//a[contains(@class, 'Header__logo')] | " +
            ".//a[contains(@class, 'Header2__logo-link')]")
    VertisElement logo();

    @Name("Кнопка возврата на предыдущую страницу")
    @FindBy(".//a[contains(@class, '__return-title')] | " +
            ".//div[contains(@class, 'Header2__col-return')]")
    VertisElement backButton();

    @Name("Кнопка добавления объявления")
    @FindBy(".//a[contains(@class, 'header__add-button')] | " +
            ".//a[contains(@class, 'HeaderAddSaleButton')] | " +
            ".//div[contains(@class, 'Header2__col-actions')]")
    VertisElement addSaleButton();

    @Name("Кнопка сохранения поиска")
    @FindBy(".//button[./*[contains(@class, 'subscription ')]]")
    VertisElement saveSearchButton();

    @Name("Кнопка удаления поиска")
    @FindBy(".//button[./*[contains(@class, 'subscription-active')]]")
    VertisElement deleteSearchButton();
}
