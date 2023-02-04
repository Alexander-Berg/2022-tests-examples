package ru.auto.tests.publicapi.carfax;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.api.CarfaxApi.DeleteCommentCarfaxOper.BLOCK_ID_PATH;
import static ru.auto.tests.publicapi.api.CarfaxApi.DeleteCommentCarfaxOper.VIN_PATH;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeEmptyJson;

@DisplayName("DELETE /carfax/user/comment")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DeleteCommentTest {

    private static final String VIN = "X9FLXXEEBLES67719";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AccountManager accountManager;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Test
    @Owner(TIMONDL)
    public void shouldSee200DeleteComment() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String blockId = getRandomString();
        String offerId = RawReportUtils.createTestOffer(adaptor, account, sessionId);
        adaptor.waitUserOffersActive(sessionId, CARS, VIN);

        adaptor.createCarfaxComment(sessionId, VIN, blockId, getRandomString(100));

        api.carfax().deleteCommentCarfax().reqSpec(defaultSpec())
                .reqSpec(requestSpecBuilder -> {
                    requestSpecBuilder.addQueryParam(VIN_PATH, VIN);
                    requestSpecBuilder.addQueryParam(BLOCK_ID_PATH, blockId);
                })
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeEmptyJson()));
    }
}
