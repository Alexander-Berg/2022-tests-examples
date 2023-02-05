package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;

public final class SearchResultsCard extends BaseScreen {
    @AndroidFindBy(uiAutomator = ".text(\"Результаты поиска\")")
    @iOSXCUITFindBy(accessibility = "Результаты поиска")
    private MobileElement view;

    @AndroidFindBy(id = "search_line_additional_close_button")
    private MobileElement buttonClose;

    private SearchResultsCard() {
        super();
        setView(view);
    }

    public static SearchResultsCard get() {
        return new SearchResultsCard();
    }

    @Step("Закрыть карточку с результатами поиска")
    public void clickClose() {
        user.clicks(buttonClose);
    }
}
