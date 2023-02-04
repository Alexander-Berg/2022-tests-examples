package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PromoAppPage extends PromoPage {

    @Name("Ссылка на Google Play")
    @FindBy("//a[contains(@class, 'AppStoreButton_type_googleplay')]")
    VertisElement googlePlayUrl();

    @Name("Ссылка на App Store")
    @FindBy("//a[contains(@class, 'AppStoreButton_type_appstore')]")
    VertisElement appStoreUrl();
}
