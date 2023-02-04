package ru.auto.tests.publicapi.carfax;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiVinVinHistoryScore;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.*;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.CREATE_SCORE_FOR_NOT_BOUGHT_HISTORY;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.model.AutoApiVinVinHistoryScore.ScoreEnum.NEGATIVE;
import static ru.auto.tests.publicapi.model.AutoApiVinVinHistoryScore.ScoreEnum.POSITIVE;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("POST /carfax/score")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class CreateScoreTest {

    private final static AutoApiVinVinHistoryScore SCORE = new AutoApiVinVinHistoryScore().comment("")
            .platform("desktop")
            .score(NEGATIVE)
            .timestampCreate(new DateTime().getMillis())
            .vin("WDBRN40J35A685278")
            .wantMoneyBack(false);
    private static final String VIN = "JTMHV05J404104103";
    private static final String PLATFORM_DESKTOP = "desktop";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AccountManager am;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    @Owner(TIMONDL)
    public void shouldSee403WhenNoAuth() {
        api.carfax().createVinHistoryScore().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee400WhenNoVin() {
        api.carfax().createVinHistoryScore().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee403WhenScoreNoBoughtVinHistory() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiErrorResponse response = api.carfax().createVinHistoryScore()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .body(SCORE)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
                .as(AutoApiErrorResponse.class);

        assertThat(response).hasStatus(ERROR)
                .hasError(CREATE_SCORE_FOR_NOT_BOUGHT_HISTORY)
                .hasDetailedError("Tried to create score for not bought vin history");
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSuccessCreateScoreToVinHistory() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.buyVinHistory(sessionId, account.getId(), VIN);

        api.carfax().createVinHistoryScore()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .body(new AutoApiVinVinHistoryScore().platform(PLATFORM_DESKTOP).score(POSITIVE)
                        .timestampCreate(new DateTime().getMillis())
                        .vin(VIN)
                        .wantMoneyBack(false))
                .executeAs(validatedWith(shouldBeSuccess()));
    }
}
