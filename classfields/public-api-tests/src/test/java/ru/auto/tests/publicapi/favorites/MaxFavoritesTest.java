package ru.auto.tests.publicapi.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.module.PublicApiWithDataModule;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by dskuznetsov on 13.08.18
 */


@DisplayName("POST /user/favorites/{category}/{offerId}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiWithDataModule.class)
@Issue("AUTORUTEST-3693")
public class MaxFavoritesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private Account account;

    @Test
    public void shouldAddMoreThen100Favorites() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();

        Integer countBefore = api.userFavorites().countFavorites().categoryPath(CARS).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess())).getCount();

        api.userFavorites().addFavorite().categoryPath(CARS).offerIdPath(offerId).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));

        Integer countAfter = api.userFavorites().countFavorites().categoryPath(CARS).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess())).getCount();

        assertThat(countBefore + 1).isEqualTo(countAfter);
    }
}