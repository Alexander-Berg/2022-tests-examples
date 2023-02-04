package ru.auto.tests.publicapi.feeds;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiFeedprocessorFeedFeedTask;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.AUTH_ERROR;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.NOT_FOUND;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.SIGNATURE_MALFORMED;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_PARAMS_DETAILS;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getMajorAccount;

@DisplayName("GET /feeds/history/{task_id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class FeedsHistoryTest {

    private final static Integer VALID_TASK_ID = 22720930;
    private final static Integer ANOTHER_DEALER_TASK_ID = 22720186;
    private final static Integer NOT_EXISTENT_TASK_ID = 1;
    private final static Integer INVALID_TASK_ID = 0;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private AccountManager accountManager;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    @Owner(TIMONDL)
    public void shouldSee403WhenNoAuth() {
        api.feeds().getTaskDetails().taskIdPath(VALID_TASK_ID).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee401WithoutSession() {
        api.feeds().getTaskDetails().taskIdPath(VALID_TASK_ID).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee401WithUserSession() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiErrorResponse response = api.feeds().getTaskDetails().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .taskIdPath(VALID_TASK_ID)
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR)
                .hasError(AUTH_ERROR)
                .hasDetailedError(AUTH_ERROR.getValue());
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee404WithNotExistentTask() {
        String sessionId = adaptor.login(getMajorAccount()).getSession().getId();

        AutoApiErrorResponse response = api.feeds().getTaskDetails().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .taskIdPath(NOT_EXISTENT_TASK_ID)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR)
                .hasError(NOT_FOUND)
                .hasDetailedError(format("Details for task %s not found", 1));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee404WithAnotherDealerTask() {
        String sessionId = adaptor.login(getMajorAccount()).getSession().getId();

        AutoApiErrorResponse response = api.feeds().getTaskDetails().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .taskIdPath(ANOTHER_DEALER_TASK_ID)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR)
                .hasError(NOT_FOUND)
                .hasDetailedError(format("Details for task %s not found", ANOTHER_DEALER_TASK_ID));
    }

    @Test
    @Issue("AUTORUAPI-6355")
    @Owner(TIMONDL)
    public void shouldSee400WithInvalidTaskId() {
        String sessionId = adaptor.login(getMajorAccount()).getSession().getId();

        AutoApiErrorResponse response = api.feeds().getTaskDetails().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .taskIdPath(INVALID_TASK_ID)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR)
                .hasError(BAD_PARAMS_DETAILS)
                .hasDetailedError(SIGNATURE_MALFORMED.getValue());
    }

    @Test
    @Owner(TIMONDL)
    public void shouldHasNoDiffWithProduction() {
        String sessionId = adaptor.login(getMajorAccount()).getSession().getId();
        AutoApiFeedprocessorFeedFeedTask taskInfo = adaptor.createFeedprocessorTaskForDealer(sessionId);

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.feeds().getTaskDetails().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .taskIdPath(taskInfo.getId())
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }

}
