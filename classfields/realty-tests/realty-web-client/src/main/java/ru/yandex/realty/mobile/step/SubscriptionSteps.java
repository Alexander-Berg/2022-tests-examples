package ru.yandex.realty.mobile.step;

import io.qameta.allure.Step;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.yandex.realty.mobile.page.SubscriptionPage;

import java.util.concurrent.TimeUnit;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.mobile.element.subscriptions.SubscriptionPopup.ADJUST_SUBSCRIPTION;
import static ru.yandex.realty.mobile.element.subscriptions.SubscriptionPopup.DELETE_SUBSCRIPTION;
import static ru.yandex.realty.mobile.element.subscriptions.SubscriptionPopup.SAVE_BUTTON;
import static ru.yandex.realty.step.CommonSteps.FIRST;

public class SubscriptionSteps extends WebDriverSteps {
    public SubscriptionPage onSubscriptionPage() {
        return on(SubscriptionPage.class);
    }

    @Step("Меняем поле email в настройках на рандомное")
    public String changeEmail() {
        String email = Utils.getRandomEmail();
        onSubscriptionPage().subscriptionPopup().input().click();
        onSubscriptionPage().subscriptionPopup().clearCross().click();
        onSubscriptionPage().subscriptionPopup().input().sendKeys(email);
        onSubscriptionPage().subscriptionPopup().button(SAVE_BUTTON).click();
        return email;
    }

    @Step("Открываем попап подписок")
    public void openSubscriptionPopup() {
        onSubscriptionPage().subscription(FIRST).optionsButton().click();
        onSubscriptionPage().subscriptionPopup().waitUntil(isDisplayed());
        onSubscriptionPage().subscriptionPopup().button(ADJUST_SUBSCRIPTION).click();
        onSubscriptionPage().subscriptionPopup().button(SAVE_BUTTON).should(isDisplayed());
        //ждем анимацию попапа
        waitSomething(2, TimeUnit.SECONDS);
    }

    @Step("Открываем попап удаления")
    public void openDeletePopup() {
        onSubscriptionPage().subscription(FIRST).optionsButton().click();
        onSubscriptionPage().subscriptionPopup().waitUntil(isDisplayed());
        onSubscriptionPage().subscriptionPopup().button(DELETE_SUBSCRIPTION).click();
        //ждем анимацию попапа
        waitSomething(2, TimeUnit.SECONDS);
    }
}
