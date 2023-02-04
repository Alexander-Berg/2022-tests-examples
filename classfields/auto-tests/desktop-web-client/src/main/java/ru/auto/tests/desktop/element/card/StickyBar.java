package ru.auto.tests.desktop.element.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithFavoritesButton;

public interface StickyBar extends VertisElement, WithFavoritesButton {

    @Name("Адрес")
    @FindBy(".//span[contains(@class, 'CardSellerNamePlace__place')]")
    VertisElement address();

    @Name("Ссылка «... в наличии»")
    @FindBy(".//a[contains(@class, 'CardSellerNamePlace__count')]")
    VertisElement inStockUrl();

    @Name("Кнопка «Написать»")
    @FindBy(".//button[contains(@class, 'PersonalMessage_type_button')]")
    VertisElement sendMessageButton();

    @Name("Кнопка «Показать телефон»")
    @FindBy(".//button[contains(@class, '__phone')]")
    VertisElement showPhoneButton();

    @Name("Кнопка добавления в сравнение")
    @FindBy(".//div[contains(@class, 'ButtonCompare_size_l CardStickyBar__compareButton')]")
    VertisElement addToCompareButton();

    @Name("Кнопка удаления из сравнения")
    @FindBy(".//div[contains(@class, 'ButtonCompare_active CardStickyBar__compareButton')]")
    VertisElement deleteFromCompareButton();

    @Name("Кнопка перехода на предыдущее объявление")
    @FindBy(".//a[contains(@class, 'CardStickyBarPrevNext__link') and .//*[contains(@class, 'left')]]")
    VertisElement prev();

    @Name("Кнопка перехода на следующее объявление")
    @FindBy(".//a[contains(@class, 'CardStickyBarPrevNext__link') and .//*[contains(@class, 'right')]]")
    VertisElement next();

    @Name("Иконка «Проверенный дилер»")
    @FindBy(".//*[contains(@class, 'IconSvg_verified-dealer')]")
    VertisElement loyaltyIcon();
}
