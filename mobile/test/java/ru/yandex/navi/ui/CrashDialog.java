package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.HowToUseLocators;

import static io.appium.java_client.pagefactory.LocatorGroupStrategy.ALL_POSSIBLE;

public class CrashDialog extends BaseScreen {
    @HowToUseLocators(androidAutomation = ALL_POSSIBLE)
    @AndroidFindBy(uiAutomator = ".text(\"В работе приложения \"Яндекс.Навигатор\" произошел сбой\")")
    @AndroidFindBy(uiAutomator = ".text(\"В приложении \"Яндекс.Навигатор\" произошла ошибка.\")")
    private MobileElement dialogTitle;

    public CrashDialog() {
        super();
        setView(dialogTitle);
    }
}
