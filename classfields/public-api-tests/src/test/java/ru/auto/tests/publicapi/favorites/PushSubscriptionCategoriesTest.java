package ru.auto.tests.publicapi.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiPushDelivery;
import ru.auto.tests.publicapi.module.PublicApiSearchesModule;
import ru.auto.tests.publicapi.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static org.hamcrest.core.IsNull.notNullValue;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("PUT /user/favorites/all/subscriptions/{id}/push")
@GuiceModules(PublicApiSearchesModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PushSubscriptionCategoriesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(TestData.defaultCategories());
    }

    @Test
    public void shouldEnabledSubscription() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String id = adaptor.addSearch(category, sessionId).getId();

        disablePushDelivery(id, sessionId);
        api.userFavorites().upsertPushDelivery().idPath(id)
                .body(new AutoApiPushDelivery().enabled(true))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess()));

        MatcherAssert.assertThat("В ответе отсутствуют способы доставки «deliveries»",
                adaptor.getSavedSearch(sessionId, id).getSearch().getDeliveries(), notNullValue());

        Assertions.assertThat(adaptor.getSavedSearch(sessionId, id).getSearch()
                .getDeliveries().getPushDelivery().getEnabled()).isEqualTo(true);
    }

    @Test
    public void shouldDisabledSubscription() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String id = adaptor.addSearch(category, sessionId).getId();

        api.userFavorites().upsertPushDelivery().idPath(id)
                .body(new AutoApiPushDelivery().enabled(false))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess()));

        MatcherAssert.assertThat("В ответе отсутствуют способы доставки «deliveries»",
                adaptor.getSavedSearch(sessionId, id).getSearch().getDeliveries(), notNullValue());

        Assertions.assertThat(adaptor.getSavedSearch(sessionId, id).getSearch()
                .getDeliveries().getPushDelivery().getEnabled()).isEqualTo(false);
    }

    @Test
    public void shouldEnableSubscriptionForAnonym() {
        String sessionId = adaptor.session().getSession().getId();
        String id = adaptor.addSearch(category, sessionId).getId();

        disablePushDelivery(id, sessionId);
        api.userFavorites().upsertPushDelivery().idPath(id)
                .body(new AutoApiPushDelivery().enabled(true))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));

        MatcherAssert.assertThat("В ответе отсутствуют способы доставки «deliveries»",
                adaptor.getSavedSearch(sessionId, id).getSearch().getDeliveries(), notNullValue());

        Assertions.assertThat(adaptor.getSavedSearch(sessionId, id).getSearch()
                .getDeliveries().getPushDelivery().getEnabled()).isEqualTo(true);
    }

    @Test
    public void shouldDisableSubscriptionForAnonym() {
        String sessionId = adaptor.session().getSession().getId();
        String id = adaptor.addSearch(category, sessionId).getId();

        api.userFavorites().upsertPushDelivery().idPath(id)
                .body(new AutoApiPushDelivery().enabled(false))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));

        MatcherAssert.assertThat("В ответе отсутствуют способы доставки «deliveries»",
                adaptor.getSavedSearch(sessionId, id).getSearch().getDeliveries(), notNullValue());

        Assertions.assertThat(adaptor.getSavedSearch(sessionId, id).getSearch()
                .getDeliveries().getPushDelivery().getEnabled()).isEqualTo(false);
    }

    @Step("Отписываемся от пуш уведомлений по поиску: {id}")
    private void disablePushDelivery(String id, String sessionId) {
        api.userFavorites().upsertPushDelivery().idPath(id).body(new AutoApiPushDelivery().enabled(false))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }

}
