package ru.auto.tests.cabinet.acl;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.model.UpdateResult;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("DELETE /acl/client/{client_id}/group/{group_id}")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class DeleteAclClientGroupTest {

    private static final String MANAGER_ID = "19565983";
    private static final String USER_ID = "34885555";

    @Inject
    private CabinetApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldDeleteAclGroup() {
        String dealerId = "27554";
        Long aclGroupId = adaptor.addAclIdToDealer(dealerId, USER_ID).getId();
        UpdateResult response = api.acl().deleteClientAccessGroup().groupIdPath(aclGroupId).clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(USER_ID).reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getId()).isEqualTo(aclGroupId);
    }

    @Test
    public void shouldGetStatusOkForManager() {
        String dealerId = "27268";
        Long aclGroupId = adaptor.addAclIdToDealer(dealerId, MANAGER_ID).getId();
        api.acl().deleteClientAccessGroup().groupIdPath(aclGroupId).clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(MANAGER_ID).reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetStatusForbidden() {
        String dealerId = "27134";
        Long aclGroupId = adaptor.addAclIdToDealer(dealerId, MANAGER_ID).getId();
        api.acl().deleteClientAccessGroup().groupIdPath(aclGroupId).clientIdPath(dealerId)
                .xAutoruOperatorUidHeader("1956598").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldGetStatusForbiddenForFistGroup() {
        String dealerId = "27134";
        api.acl().deleteClientAccessGroup().groupIdPath("1").clientIdPath(dealerId)
                .xAutoruOperatorUidHeader("1956598").reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}