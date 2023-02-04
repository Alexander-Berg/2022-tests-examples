package ru.yandex.realty.step;

import io.qameta.allure.Step;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.yandex.realty.page.SubscriptionPage;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.page.SubscriptionPage.SAVE_BUTTON;
import static ru.yandex.realty.step.CommonSteps.FIRST;

public class SubscriptionSteps extends WebDriverSteps {
    public SubscriptionPage onSubscriptionPage() {
        return on(SubscriptionPage.class);
    }

    @Step("Выбираем «Получать уведомления {{ value }}»")
    public void selectItem(String item) {
        onSubscriptionPage().subscriptionPopup().selectButton().click();
        onSubscriptionPage().subscriptionPopup().notificationItem(item).click();
        onSubscriptionPage().subscriptionPopup().selectButton().should(hasText(containsString(item)));
    }

    @Step("Меняем поле email в настройках на рандомное")
    public String changeEmail() {
        String email = Utils.getRandomEmail();
        onSubscriptionPage().subscriptionPopup().clearEmailInput().click();
        onSubscriptionPage().subscriptionPopup().emailInput().sendKeys(email);
        onSubscriptionPage().subscriptionPopup().button(SAVE_BUTTON).click();
        return email;
    }

    @Step("Открываем попап подписок")
    public void openSubscriptionPopup() {
        onSubscriptionPage().subscription(FIRST).hover();
        onSubscriptionPage().subscription(FIRST).spanLink("Настроить").click();
        onSubscriptionPage().subscriptionPopup().waitUntil(isDisplayed());
        //ждем анимацию попапа
        waitSomething(2, TimeUnit.SECONDS);
    }

    @Step("Открываем попап удаления")
    public void openDeletePopup() {
        onSubscriptionPage().subscription(FIRST).hover();
        onSubscriptionPage().subscription(FIRST).spanLink("Удалить").click();
        onSubscriptionPage().deletePopup().waitUntil(isDisplayed());
        //ждем анимацию попапа
        waitSomething(2, TimeUnit.SECONDS);
    }
}
