package ru.yandex.infra.auth.idm.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.auth.Role;
import ru.yandex.infra.auth.RoleSubject;
import ru.yandex.infra.auth.RolesInfo;
import ru.yandex.infra.auth.nanny.DummyNannyService;
import ru.yandex.infra.auth.nanny.NannyRole;
import ru.yandex.infra.auth.yp.DummyYpServiceImpl;
import ru.yandex.infra.controller.metrics.GaugeRegistry;
import ru.yandex.infra.controller.util.ResourceUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class IdmServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private DummyYpServiceImpl ypService;
    private DummyNannyService nannyService;
    private RolesInfo rolesInfo;
    private IdmService idmService;

    @BeforeEach
    void before() {
        ypService = new DummyYpServiceImpl();
        Config rootConfig = ConfigFactory.load("roles_to_permissions.conf");
        rolesInfo = RolesInfo.fromConfig(rootConfig);
        nannyService = new DummyNannyService();
        idmService = new IdmService(ypService, rolesInfo, nannyService, GaugeRegistry.EMPTY);
    }

    private static String removeTabsAndSpaces(String str) {
        return str.replace("\n", "").replace("\r", "").replace(" ", "").replace("\t", "");
    }

    @Test
    void getInfoTest() {
        ypService.roles = ImmutableSet.of(
                new Role("", ""),
                new Role("", "SUPER_USER"),
                new Role("project", ""),
                new Role("project", "OWNER"),
                new Role("project", "MAINTAINER"),
                new Role("project", "DEVELOPER"),
                new Role("project", "ROOT_DEVELOPER"),
                new Role("project", "SYSTEM_DEVELOPER"),
                new Role("project.stage", ""),
                new Role("project.stage", "MAINTAINER"),
                new Role("project.stage", "DEVELOPER"),
                new Role("project.stage", "ROOT_DEVELOPER"),
                new Role("project.stage", "SYSTEM_DEVELOPER"),
                new Role("project.stage.box", ""),
                new Role("project.stage.box", "DEVELOPER"),
                new Role("project.stage.box", "ROOT_DEVELOPER"),
                new Role("project.stage.system-box", ""),
                new Role("project.stage.system-box", "SYSTEM_DEVELOPER"),
                new Role("project.stage2", "", "", "c65f9708-f8a2c60d-10112dbb-1c31717d"),
                new Role("project.stage2", "MAINTAINER", "", "MAINTAINER.c65f9708-f8a2c60d-10112dbb-1c31717d")
        );

        IdmInfoResponse infoResponse = idmService.getInfo();
        String expectedResponse = ResourceUtils.readResource("idm_get_info_response.json");

        assertDoesNotThrow(() -> {
            String jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(infoResponse);
            Assertions.assertEquals(removeTabsAndSpaces(expectedResponse), removeTabsAndSpaces(jsonResponse));
        });
    }

    @Test
    void getInfoNoRolesTest() {
        ypService.roles = Set.of();

        IdmInfoResponse infoResponse = idmService.getInfo();

        assertDoesNotThrow(() -> {
            String jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(infoResponse);
            Assertions.assertEquals(removeTabsAndSpaces("{\"code\":1,\"warning\":\"There is no role nodes in the system yet\"," +
                    "\"roles\":{\"slug\":\"ROOT\",\"name\":\"ROOT\",\"values\":{}}}"), removeTabsAndSpaces(jsonResponse));
        });
    }

    @Test
    void getInfoNannyTest() {
        ypService.roles = ImmutableSet.of(
            new Role("", ""),
            new Role("project", ""),
            new Role("project."+Role.NANNY_ROLES_PARENT_NODE, "", "", ""),
            new Role("project."+Role.NANNY_ROLES_PARENT_NODE+".service1", "", "", ""),
            new NannyRole("project."+Role.NANNY_ROLES_PARENT_NODE+".service1", "owners", "uniqueId", Set.of(), Set.of()),
            new NannyRole("project."+Role.NANNY_ROLES_PARENT_NODE+".service1", "developers", "", Set.of(), Set.of())
        );

        IdmInfoResponse infoResponse = idmService.getInfo();
        String expectedResponse = ResourceUtils.readResource("idm_get_info_nanny_response.json");

        assertDoesNotThrow(() -> {
            String jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(infoResponse);
            Assertions.assertEquals(removeTabsAndSpaces(expectedResponse), removeTabsAndSpaces(jsonResponse));
        });
    }

    @Test
    void getRolesNoRolesTest() {
        ypService.roles = Set.of();

        var response = idmService.getRoles();
        assertDoesNotThrow(() -> {
            String jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
            Assertions.assertEquals(removeTabsAndSpaces("{\"code\":1,\"warning\":\"Cannot find any roles in database\"}"), removeTabsAndSpaces(jsonResponse));
        });
    }

    @Test
    void getRolesNoRoleSubjectsTest() {
        ypService.roles = Set.of(
                new Role("project", ""),
                new Role("project", "OWNER"),
                new Role("project.nanny.service1", "", "", ""),
                new NannyRole("project.nanny.service1", "owners", "uniqueId", Set.of(), Set.of())
        );

        var response = idmService.getRoles();
        assertDoesNotThrow(() -> {
            String jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
            Assertions.assertEquals(removeTabsAndSpaces("{\"code\":1,\"warning\":\"Cannot find any roles in database\"}"), removeTabsAndSpaces(jsonResponse));
        });
    }

    @Test
    void getRolesTest() {
        Role projectOwnerRole = new Role("project", "OWNER");
        Role nannyOwnerRole = new Role("project.nanny.service1", "owners", "uniqueId", "");
        ypService.roles = Set.of(
                new Role("project", ""),
                projectOwnerRole,
                new Role("project.nanny.service1", "", "", ""),
                nannyOwnerRole
        );

        ypService.roleSubjects = new HashSet<>(Set.of(
                new RoleSubject("user1", 0L, projectOwnerRole),
                new RoleSubject("user2", 0L, nannyOwnerRole)

        ));

        var response = idmService.getRoles();
        assertDoesNotThrow(() -> {
            String jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
            Assertions.assertEquals(removeTabsAndSpaces("{\"code\":0,\"users\":[{\"login\":\"user1\"," +
                    "\"roles\":[{\"ROOT\":\"project\",\"project\":\"OWNER\"}]},{\"login\":\"user2\"," +
                    "\"roles\":[{\"project.nanny\":\"service1\",\"ROOT\":\"project\",\"project.nanny" +
                    ".service1\":\"owners\",\"project\":\"nanny\"}]}]}"), removeTabsAndSpaces(jsonResponse));
        });
    }

    @Test
    void getMembersTest() {
        IdmServiceResponse response;
        response = idmService.processMemberships("[{\"login\":\"eng\", \"group\":28011}]", IdmService.RequestType.ADD_BATCH_MEMBERSHIPS);
        Assertions.assertEquals(0, response.getCode());
        response = idmService.processMemberships("[{\"login\":\"dev\", \"group\":28011}]", IdmService.RequestType.ADD_BATCH_MEMBERSHIPS);
        Assertions.assertEquals(0, response.getCode());
        response = idmService.processMemberships("[{\"login\":\"mngr\", \"group\":28011}]", IdmService.RequestType.ADD_BATCH_MEMBERSHIPS);
        Assertions.assertEquals(0, response.getCode());

        IdmMembershipsResponse membershipResponse = idmService.getAllMemberships();
        assertDoesNotThrow(() -> {
                    String jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(membershipResponse);
                    Assertions.assertEquals("{\n"
                            + "  \"code\" : 0,\n"
                            + "  \"memberships\" : [ {\n"
                            + "    \"login\" : \"dev\",\n"
                            + "    \"group\" : 28011\n"
                            + "  }, {\n"
                            + "    \"login\" : \"eng\",\n"
                            + "    \"group\" : 28011\n"
                            + "  }, {\n"
                            + "    \"login\" : \"mngr\",\n"
                            + "    \"group\" : 28011\n"
                            + "  } ]\n"
                            + "}", jsonResponse.replace("\r", ""));
                }
        );
    }

    @Test
    void addStageRoleTest() {
        String jsonEncodedRole = "{\"ROOT\": \"proj\", \"proj\": \"stage0\", \"proj.stage0\": \"MAINTAINER\"}";
        IdmServiceResponse response = idmService.addOrRemoveRole(Optional.of("user1"), Optional.empty(), jsonEncodedRole,
                "MAINTAINER.236f36e2-9000eaa-1746e53a-264a28c1", IdmService.RequestType.ADD_ROLE);
        Assertions.assertEquals(IdmServiceResponse.ResponseCode.RESPONSE_CODE_SUCCESS.getCode(), response.getCode());
        Assertions.assertEquals(
                Set.of(
                        new RoleSubject("user1", 0L, new Role("proj.stage0", "MAINTAINER", "", "MAINTAINER.236f36e2-9000eaa-1746e53a-264a28c1"))
                ),
                ypService.roleSubjects);
    }

    @Test
    void removeNannyServiceRoleTest() {
        String jsonEncodedRole = "{\"ROOT\": \"proj\"," +
                "\"proj\": \"nanny\"," +
                "\"proj.nanny\": \"service1\"," +
                "\"proj.nanny.service1\": \"evicters\"}";
        IdmServiceResponse response = idmService.addOrRemoveRole(Optional.of("user1"), Optional.empty(), jsonEncodedRole,
                "evicters.236f36e2-9000eaa-1746e53a-264a28c1", IdmService.RequestType.ADD_ROLE);
        Assertions.assertEquals(IdmServiceResponse.ResponseCode.RESPONSE_CODE_SUCCESS.getCode(), response.getCode());
        Assertions.assertEquals(
                Set.of(
                        new RoleSubject("user1", 0L, new Role("proj.nanny.service1", "evicters", "", "evicters.236f36e2-9000eaa-1746e53a-264a28c1"))
                ),
                ypService.roleSubjects);
    }
}
