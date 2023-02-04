package ru.auto.tests.publicapi.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiEmailDelivery;
import ru.auto.tests.publicapi.model.AutoApiEmailDeliveryAssert;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.module.PublicApiSearchesModule;
import ru.auto.tests.publicapi.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static org.hamcrest.core.IsNull.notNullValue;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 22.09.17.
 */

@DisplayName("PUT /user/favorites/all/subscriptions/{id}/email")
@GuiceModules(PublicApiSearchesModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class UpdateEmailDeliveryCategoriesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private Account account;

    @Inject
    private PublicApiAdaptor adaptor;

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(TestData.defaultCategories());
    }

    @Test
    public void shouldEnableEmailDelivery() {
        String sessionId = adaptor.login(account).getSession().getId();
        String id = adaptor.addSearch(category, sessionId).getId();
        disableEmailDelivery(id, sessionId);
        api.userFavorites().upsertEmailDelivery().idPath(id).body(getEnabledEmailDelivery())
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));

        MatcherAssert.assertThat("В ответе отсутствуют способы доставки «deliveries»",
                adaptor.getSavedSearch(sessionId, id).getSearch().getDeliveries(), notNullValue());

        AutoApiEmailDeliveryAssert.assertThat(adaptor.getSavedSearch(sessionId, id).getSearch()
                .getDeliveries().getEmailDelivery()).hasEnabled(true);
    }

    @Test
    public void shouldDisableEmailDelivery() {
        String sessionId = adaptor.login(account).getSession().getId();
        String id = adaptor.addSearch(category, sessionId).getId();

        api.userFavorites().upsertEmailDelivery().idPath(id).body(getDisabledEmailDelivery())
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));

        MatcherAssert.assertThat("В ответе отсутствуют способы доставки «deliveries»",
                adaptor.getSavedSearch(sessionId, id).getSearch().getDeliveries(), notNullValue());

        AutoApiEmailDeliveryAssert.assertThat(adaptor.getSavedSearch(sessionId, id).getSearch()
                .getDeliveries().getEmailDelivery()).hasEnabled(false);
    }

    private AutoApiEmailDelivery getEnabledEmailDelivery() {
        return new AutoApiEmailDelivery().enabled(true).period("3600s");
    }

    private AutoApiEmailDelivery getDisabledEmailDelivery() {
        return new AutoApiEmailDelivery().enabled(false);
    }

    @Step("Отписываемся от e-mail уведомлений по поиску: {id}")
    private void disableEmailDelivery(String id, String sessionId) {
        api.userFavorites().upsertEmailDelivery()
                .idPath(id).body(getDisabledEmailDelivery())
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }
}