package ru.auto.tests.publicapi.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.SCROOGE;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 22.09.17.
 */

@DisplayName("DELETE /user/favorites/{category}/{offerId}")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DeleteFromFavoritesCategoriesTest {

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
    @SuppressWarnings("Duplicates")
    public void shouldDeleteFromFavorites() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();
        adaptor.addFavorites(sessionId, category, offerId);

        api.userFavorites().deleteFavorite().categoryPath(category.name()).offerIdPath(offerId)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    @Owner(SCROOGE)
    @SuppressWarnings("Duplicates")
    public void shouldDeleteFromFavoritesForAnonym() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String anonymSessionId = adaptor.session().getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();
        adaptor.addFavorites(anonymSessionId, category, offerId);

        api.userFavorites().deleteFavorite().categoryPath(category.name()).offerIdPath(offerId)
                .reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId).execute(validatedWith(shouldBeSuccess()));
    }
}
