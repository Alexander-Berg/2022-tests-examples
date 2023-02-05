package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.qameta.allure.Step;

import java.time.Duration;

public final class AliceActionsPopup extends BaseScreen {
    @AndroidFindBy(id = "alice_actions_view")
    private MobileElement view;

    private AliceActionsPopup() {
        super();
        setView(view);
    }

    public static AliceActionsPopup getVisible() {
        AliceActionsPopup popup = new AliceActionsPopup();
        popup.checkVisible(Duration.ofSeconds(5));
        return popup;
    }

    @Step("Нажать на '{button}' в плашке Алисы")
    public void click(String button) {
        user.clicks(button);
    }
}
