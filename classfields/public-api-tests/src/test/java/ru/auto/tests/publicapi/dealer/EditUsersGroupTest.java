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
import static ru.auto.tests.publicapi.model.AutoCabinetResource.AccessEnum.WRITE;
import static ru.auto.tests.publicapi.model.AutoCabinetResource.AliasEnum.DASHBOARD;
import static ru.auto.tests.publicapi.model.AutoCabinetResource.AliasEnum.OFFERS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount;

@DisplayName("PUT /dealer/users/group/{group_id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class EditUsersGroupTest {

    private AutoCabinetGroup usersGroup = new AutoCabinetGroup().name(getRandomString()).editable(true)
            .addGrantsItem(new AutoCabinetResource().access(ONLY).alias(DASHBOARD).name("Дашборд"));

    private AutoCabinetGroup anotherUsersGroup = new AutoCabinetGroup().name(getRandomString()).editable(true)
            .addGrantsItem(new AutoCabinetResource().access(ONLY).alias(DASHBOARD).name("Дашборд"))
            .addGrantsItem(new AutoCabinetResource().access(WRITE).alias(OFFERS).name("Объявления"));

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
    public void shouldUpdateUsersGroup() {
        sessionId = adaptor.login(getDemoAccount()).getSession().getId();

        AutoCabinetGroup cabinetGroup = adaptor.createDealerUsersGroup(sessionId, usersGroup);

        AutoCabinetGroup updatedCabinetGroup = api.dealer().editUsersGroup().reqSpec(defaultSpec())
                .groupIdPath(cabinetGroup.getId())
                .xSessionIdHeader(sessionId)
                .body(anotherUsersGroup)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        groupId = updatedCabinetGroup.getId();

        assertThat(updatedCabinetGroup).hasName(anotherUsersGroup.getName())
                .hasGrants(anotherUsersGroup.getGrants());

        AutoCabinetGroupsList dealerUsersGroupsList = api.dealer().getUsersGroupsList()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBe200OkJSON()));

        assertThat(dealerUsersGroupsList).hasGroups(updatedCabinetGroup);
    }

    @After
    public void deleteUserGroup() {
        adaptor.deleteDealerUsersGroup(sessionId, groupId);
    }

}
