package ru.auto.tests.realty.vos2.subscriptions;


import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.runners.GuiceDataProviderRunner;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.adaptor.Vos2ApiAdaptor;
import ru.auto.tests.realty.vos2.anno.Vos;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;

import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithUserIsNotFound;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;

@DisplayName("PUT /api/realty/user/subscriptions/{userID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class UserSubscriptionsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient vos2;

    @Inject
    @Vos
    private Account account;

    @Inject
    private Vos2ApiAdaptor adaptor;

    @Test
    public void shouldSuccessSubscribeUserForNotifications() {
        vos2.userSubscriptions().subscriptionsRoute().userIDPath(account.getId()).allQuery(true)
                .execute(validatedWith(shouldBeCode(SC_OK)));

        Assertions.assertThat(adaptor.getUserNotifications(account.getId())).hasDisabled(false);
    }

    @Test
    public void shouldSuccessUnsubscribeUserForNotifications() {
        vos2.userSubscriptions().subscriptionsRoute().userIDPath(account.getId()).allQuery(false)
                .execute(validatedWith(shouldBeCode(SC_OK)));

        Assertions.assertThat(adaptor.getUserNotifications(account.getId())).hasDisabled(true);
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        String randomUserId = getRandomLogin();
        vos2.userSubscriptions().subscriptionsRoute().userIDPath(getRandomLogin()).allQuery(false)
                .execute(validatedWith(shouldBe404WithUserIsNotFound(randomUserId)));
    }
}
