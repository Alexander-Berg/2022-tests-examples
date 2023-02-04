package ru.auto.tests.publicapi.dealer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.restassured.ResponseSpecBuilders;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoCabinetGroup;
import ru.auto.tests.publicapi.model.AutoCabinetGroupsList;
import ru.auto.tests.publicapi.model.AutoCabinetResource;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoCabinetResource.AccessEnum.ONLY;
import static ru.auto.tests.publicapi.model.AutoCabinetResource.AliasEnum.DASHBOARD;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount;

@DisplayName("POST /dealer/users/group")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class CreateUsersGroupTest {

    private AutoCabinetGroup validUsersGroup = new AutoCabinetGroup().name(getRandomString()).editable(true)
            .addGrantsItem(new AutoCabinetResource().access(ONLY).alias(DASHBOARD).name("Дашборд"));

    private String sessionId;
    private Long groupId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    @Owner(TIMONDL)
    public void shouldSeeUsersGroupInListAfterCreate() {
        sessionId = adaptor.login(getDemoAccount()).getSession().getId();

        AutoCabinetGroup response = api.dealer().createUsersGroup().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .body(validUsersGroup)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        groupId = response.getId();

        assertThat(response).hasName(validUsersGroup.getName())
                .hasEditable(validUsersGroup.getEditable())
                .hasGrants(validUsersGroup.getGrants());

        AutoCabinetGroupsList dealerUsersGroupsList = api.dealer().getUsersGroupsList()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBe200OkJSON()));

        assertThat(dealerUsersGroupsList).hasGroups(response);
    }

    @After
    public void deleteUserGroup() {
        adaptor.deleteDealerUsersGroup(sessionId, groupId);
    }

}
