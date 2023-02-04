package ru.auto.tests.cabinet.acl;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.model.Group;
import ru.auto.tests.cabinet.module.CabinetApiModule;

import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;

@DisplayName("PUT /acl/client/{client_id}/group")
@GuiceModules(CabinetApiModule.class)
@RunWith(GuiceTestRunner.class)
public class PutAclClientGroupTest {

    private String userId;
    private String dealerId;
    private Long groupAclId;

    private static final String MANAGER = "19565983";

    @Inject
    private CabinetApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldGetNewAclGroup() {
        userId = "1804036";
        dealerId = "27626";
        String body = getResourceAsString("acl_read_write.json");

        Group response = api.acl().putClientAccessGroup().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec())
                .reqSpec(req -> req.setBody(body))
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
        groupAclId = response.getId();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isNotNull();
        assertThat(response.getGrants()).isNotNull();
    }


    @Test
    public void shouldGetEditAclGroup() {
        userId = "28085878";
        dealerId = "31492";
        groupAclId = adaptor.addAclIdToDealer(dealerId, userId).getId();
        String body = format(getResourceAsString("acl_read_only.json"), groupAclId);
        Group response = api.acl().putClientAccessGroup().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec())
                .reqSpec(req -> req.setBody(body))
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getGrants().get(0).getAccess().toString()).isEqualTo("READ_ONLY");
    }

    @Test
    public void shouldGetStatusOkForManager() {
        userId = "28459880";
        dealerId = "31554";
        groupAclId = adaptor.addAclIdToDealer(dealerId, userId).getId();
        String body = format(getResourceAsString("acl_read_only.json"), groupAclId);
        api.acl().putClientAccessGroup().clientIdPath(dealerId).xAutoruOperatorUidHeader(MANAGER)
                .reqSpec(defaultSpec())
                .reqSpec(req -> req.setBody(body))
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    public void shouldGetStatusForbidden() {
        userId = "2111169";
        dealerId = "31768";
        groupAclId = adaptor.addAclIdToDealer(dealerId, MANAGER).getId();
        String body = format(getResourceAsString("acl_read_only.json"), groupAclId);
        api.acl().putClientAccessGroup().clientIdPath(dealerId).xAutoruOperatorUidHeader(userId)
                .reqSpec(defaultSpec())
                .reqSpec(req -> req.setBody(body))
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @After
    public void after() {
        if (dealerId != null && groupAclId != null) {
            adaptor.deleteAclIdFromDealer(dealerId, MANAGER, groupAclId);
        }
    }
}