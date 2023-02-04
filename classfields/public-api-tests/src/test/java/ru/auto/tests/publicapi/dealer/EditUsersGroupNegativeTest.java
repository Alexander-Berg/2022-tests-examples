package ru.auto.tests.publicapi.dealer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoCabinetGroup;
import ru.auto.tests.publicapi.model.AutoCabinetResource;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.CUSTOMER_ACCESS_FORBIDDEN;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.model.AutoCabinetResource.AccessEnum.ONLY;
import static ru.auto.tests.publicapi.model.AutoCabinetResource.AliasEnum.DASHBOARD;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount;

@DisplayName("PUT /dealer/users/group/{group_id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class EditUsersGroupNegativeTest {

    private static final int INVALID_GROUP_ID = 0;
    private AutoCabinetGroup usersGroup = new AutoCabinetGroup().name(getRandomString()).editable(true)
            .addGrantsItem(new AutoCabinetResource().access(ONLY).alias(DASHBOARD).name("Дашборд"));


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager accountManager;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    @Owner(TIMONDL)
    public void shouldSee403WhenNoAuth() {
        api.dealer().editUsersGroup().groupIdPath(INVALID_GROUP_ID).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee401WithoutSession() {
        api.dealer().editUsersGroup().reqSpec(defaultSpec())
                .groupIdPath(INVALID_GROUP_ID)
                .body(usersGroup)
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee403WithUserSession() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiErrorResponse response = api.dealer().editUsersGroup().reqSpec(defaultSpec())
                .groupIdPath(INVALID_GROUP_ID)
                .body(usersGroup)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR)
                .hasError(CUSTOMER_ACCESS_FORBIDDEN)
                .hasDetailedError(format("Permission denied to USERS:ReadWrite for user:%s", account.getId()));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee400WithoutBody() {
        String sessionId = adaptor.login(getDemoAccount()).getSession().getId();

        AutoApiErrorResponse response = api.dealer().editUsersGroup().reqSpec(defaultSpec())
                .groupIdPath(INVALID_GROUP_ID)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST);
        Assertions.assertThat(response.getDetailedError())
                .contains("The request content was malformed:\nExpect message object but got: null");
    }
}
