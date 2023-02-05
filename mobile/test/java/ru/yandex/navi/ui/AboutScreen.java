package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import ru.yandex.navi.tf.Direction;

public final class AboutScreen extends BaseScreen {
    public static final String LICENSE_AGREEMENT = "Лицензионное соглашение";
    public static final String PRIVACY_POLICY = "Политика конфиденциальности";

    @AndroidFindBy(uiAutomator = ".text(\"О программе\")")
    @iOSXCUITFindBy(accessibility = "О программе")
    public MobileElement title;

    @AndroidFindBy(id = "about_logo_icon")
    @iOSXCUITFindBy(className = "XCUIElementTypeImage")
    private MobileElement logoIcon;

    @AndroidFindBy(id = "about_agreement_button")
    @iOSXCUITFindBy(accessibility = "Лицензионное соглашение")
    public MobileElement licenseAgreement;

    @AndroidFindBy(id = "privacy_policy_button")
    @iOSXCUITFindBy(accessibility = "Политика конфиденциальности")
    public MobileElement privacyPolicy;

    private AboutScreen() {
        super();
        setView(title);
    }

    public static AboutScreen getVisible() {
        AboutScreen screen = new AboutScreen();
        screen.checkVisible();
        return screen;
    }

    @Step("Expectation: Отображается иконка приложения Яндекс.Навигатор, "
            + "номер версии приложения и ссылки на правовую документацию")
    public void expectCorrectLayout() {
        user.shouldSee(logoIcon);
        user.shouldSee("^Версия", Direction.DOWN);
        user.shouldSeeInScrollable(licenseAgreement);
        user.shouldSeeInScrollable(privacyPolicy);
    }
}
