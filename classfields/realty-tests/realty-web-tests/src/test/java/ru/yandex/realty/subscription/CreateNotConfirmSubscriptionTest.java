package ru.yandex.realty.subscription;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.SubscriptionSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.SUBSCRIPTION;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Подписки. Работа с неподтвержденными подписками")
@Link("https://st.yandex-team.ru/VERTISTEST-1475")
@Feature(SUBSCRIPTION)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class CreateNotConfirmSubscriptionTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Inject
    private SubscriptionSteps subscriptionSteps;

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем подписку есть почта и и активная. Клик «отправить еще раз» -> «Подтверждение отправлено»")
    public void shouldSeeNotConfirmSubscription() {
        api.createYandexAccount(account);
        String email = getRandomEmail();
        api.createNotConfirmSubscription(account.getId(), email);
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).open();
        subscriptionSteps.onSubscriptionPage().subscription(FIRST).should(hasText(containsString(email)));
        subscriptionSteps.onSubscriptionPage().subscription(FIRST).resendConfirmButton().should(isDisplayed()).click();
        subscriptionSteps.onSubscriptionPage().subscription(FIRST)
                .should(hasText(containsString("Подтверждение отправлено")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Удаляем неподтвержденную подписку")
    public void shouldDeleteSubscription() {
        api.createYandexAccount(account);
        String email = getRandomEmail();
        api.createNotConfirmSubscription(account.getId(), email);
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).open();
        subscriptionSteps.openDeletePopup();
        subscriptionSteps.onSubscriptionPage().deletePopup().button("Удалить").click();
        api.shouldSeeEmptySubscriptionList(account.getId());
    }
}
