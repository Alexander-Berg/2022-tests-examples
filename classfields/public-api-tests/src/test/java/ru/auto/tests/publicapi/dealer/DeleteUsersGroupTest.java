package ru.auto.tests.publicapi.dealer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.restassured.ResponseSpecBuilders;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoCabinetGroup;
import ru.auto.tests.publicapi.model.AutoCabinetGroupsList;
import ru.auto.tests.publicapi.model.AutoCabinetResource;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.*;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.model.AutoCabinetResource.AccessEnum.ONLY;
import static ru.auto.tests.publicapi.model.AutoCabinetResource.AliasEnum.DASHBOARD;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount;

@DisplayName("DELETE /dealer/users/group/{group_id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DeleteUsersGroupTest {

    private static final int INVALID_GROUP_ID = 0;
    private static final AutoCabinetGroup GROUP = new AutoCabinetGroup().name(getRandomString()).editable(true)
            .addGrantsItem(new AutoCabinetResource().access(ONLY).alias(DASHBOARD));

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
        api.dealer().deleteUsersGroup().groupIdPath(INVALID_GROUP_ID).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee401WithoutSession() {
        api.dealer().deleteUsersGroup().reqSpec(defaultSpec())
                .groupIdPath(INVALID_GROUP_ID)
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee403WithUserSession() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiErrorResponse response = api.dealer().deleteUsersGroup().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .groupIdPath(INVALID_GROUP_ID)
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

        AutoApiErrorResponse response = api.dealer().deleteUsersGroup().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .groupIdPath(INVALID_GROUP_ID)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR).hasError(NOT_FOUND)
                .hasDetailedError(format("Group %s not found", INVALID_GROUP_ID));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldNotSeeUsersGroupInListAfterDelete() {
        String sessionId = adaptor.login(getDemoAccount()).getSession().getId();
        AutoCabinetGroup group = adaptor.createDealerUsersGroup(sessionId, GROUP);

        api.dealer().deleteUsersGroup().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .groupIdPath(group.getId())
                .execute(ResponseSpecBuilders.validatedWith(shouldBe200OkJSON()));

        AutoCabinetGroupsList dealerUsersGroupsList = api.dealer().getUsersGroupsList()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBe200OkJSON()));

        assertThat(dealerUsersGroupsList).doesNotHaveGroups(group);
    }

}
