package ru.auto.tests.publicapi.draft;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiSuccessResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static java.lang.String.valueOf;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiSuccessResponse.StatusEnum.SUCCESS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 19.09.17.
 */

@DisplayName("DELETE /user/draft/{category}/{offerId}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DeleteDraftTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.draft().deleteDraft().categoryPath(CARS.name()).offerIdPath(valueOf(Utils.getRandomShortInt())).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldTwiceDeleteEmptyDraft() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createEmptyDraft(sessionId, CARS).getOfferId();

        AutoApiSuccessResponse successResponse = api.draft().deleteDraft().categoryPath(CARS.name()).offerIdPath(offerId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess()));
        assertThat(successResponse).hasStatus(SUCCESS);

        api.draft().deleteDraft().categoryPath(CARS.name()).offerIdPath(offerId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
        assertThat(successResponse).hasStatus(SUCCESS);
    }
}
