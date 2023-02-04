package ru.yandex.intranet.d.web.idm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.web.MockUser;

/**
 * IDM API test.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 * @since 14.10.2020
 */
@IntegrationTest
public class IdmApiTest {

    private static final String SLUG = "group";
    private static final String ROLE = "admin";

    @Autowired
    private WebTestClient webClient;
    @Value("${idm.tvmSourceId}")
    private long idmTvmSourceId;


    @Test
    public void getInfoTest() {
        String responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .get()
                .uri("/idm/info")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":0,\"roles\":{\"slug\":\"group\",\"name\":\"группа\"," +
                "\"values\":{\"admin\":\"администратор\"}}}", responseBody);
    }

    @Test
    public void getAllRolesTest() {
        String responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .get()
                .uri("/idm/get-all-roles")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":0,\"users\":[" +
                        "{\"uid\":\"1120000000000001\",\"roles\":[{\"group\":\"admin\"}]}," +
                        "{\"uid\":\"1120000000000015\",\"roles\":[{\"group\":\"admin\"}]}" +
                        "]}",
                responseBody);
    }

    @Test
    public void postRemoveRoleTest() {
        String responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .get()
                .uri("/idm/get-all-roles")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":0,\"users\":[" +
                "{\"uid\":\"1120000000000001\",\"roles\":[{\"group\":\"admin\"}]}," +
                        "{\"uid\":\"1120000000000015\",\"roles\":[{\"group\":\"admin\"}]}]}",
                responseBody);

        responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .post()
                .uri("/idm/remove-role"
                )
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("role", roleAsJson(SLUG, ROLE))
                        .with("path", roleAsPath(SLUG, ROLE))
                        .with("uid", "1120000000000001")
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":0}", responseBody);

        responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .get()
                .uri("/idm/get-all-roles")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(
                "{\"code\":0,\"users\":[{\"uid\":\"1120000000000015\",\"roles\":[{\"group\":\"admin\"}]}]}",
                responseBody
        );
    }

    @Test
    public void postRemoveRoleOnUserWithoutRoleMustAnswerCodeOkTest() {
        String responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .post()
                .uri("/idm/remove-role")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("role", roleAsJson(SLUG, ROLE))
                        .with("path", roleAsPath(SLUG, ROLE))
                        .with("uid", "1120000000000001")
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":0}", responseBody);

        responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .post()
                .uri("/idm/remove-role")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("role", roleAsJson(SLUG, ROLE))
                        .with("path", roleAsPath(SLUG, ROLE))
                        .with("uid", "1120000000000001")
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":0,\"warning\":\"User with uid = [1120000000000001] " +
                "already haven't role.\"}", responseBody);
    }

    @Test
    public void postRemoveRoleWrongTest() {
        String responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .post()
                .uri("/idm/remove-role")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("role", roleAsJson(SLUG, ROLE + "_wrong"))
                        .with("path", roleAsPath(SLUG, ROLE + "_wrong"))
                        .with("uid", "1120000000000001")
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":1,\"fatal\":\"Role not existing.\"}", responseBody);

        responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .post()
                .uri("/idm/remove-role")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("role", roleAsJson(SLUG + "_wrong", ROLE))
                        .with("path", roleAsPath(SLUG + "_wrong", ROLE))
                        .with("uid", "1120000000000001")
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":1,\"fatal\":\"Role not existing.\"}", responseBody);

        responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .post()
                .uri("/idm/remove-role?uid")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("role", roleAsJson(SLUG, ROLE))
                        .with("path", roleAsPath(SLUG, ROLE))
                        .with("uid", "1120000000099991")
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":1,\"fatal\":\"User with uid = [1120000000099991] not found.\"}",
                responseBody);

    }

    @Test
    public void postAddRoleTest() {
        String responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .get()
                .uri("/idm/get-all-roles")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":0,\"users\":[" +
                        "{\"uid\":\"1120000000000001\",\"roles\":[{\"group\":\"admin\"}]}," +
                        "{\"uid\":\"1120000000000015\",\"roles\":[{\"group\":\"admin\"}]}]}",
                responseBody);

        responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .post()
                .uri("/idm/add-role")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("role", roleAsJson(SLUG, ROLE))
                        .with("path", roleAsPath(SLUG, ROLE))
                        .with("uid", "1120000000000002")
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":0}", responseBody);

        responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .get()
                .uri("/idm/get-all-roles")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":0,\"users\":[" +
                        "{\"uid\":\"1120000000000001\",\"roles\":[{\"group\":\"admin\"}]}," +
                        "{\"uid\":\"1120000000000002\",\"roles\":[{\"group\":\"admin\"}]}," +
                        "{\"uid\":\"1120000000000015\",\"roles\":[{\"group\":\"admin\"}]}]}",
                responseBody);
    }

    @Test
    public void postAddDuplicateRoleMustAnswerCodeOkTest() {
        String responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .post()
                .uri("/idm/add-role")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("role", roleAsJson(SLUG, ROLE))
                        .with("path", roleAsPath(SLUG, ROLE))
                        .with("uid", "1120000000000001")
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":0,\"warning\":\"User with uid = [1120000000000001] " +
                "already have role.\"}", responseBody);
    }

    @Test
    public void postAddRoleWrongDataTest() {
        String responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .post()
                .uri("/idm/add-role")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("role", roleAsJson(SLUG, ROLE + "_wrong"))
                        .with("path", roleAsPath(SLUG, ROLE + "_wrong"))
                        .with("uid", "1120000000000001")
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":1,\"fatal\":\"Role not existing.\"}", responseBody);

        responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .post()
                .uri("/idm/add-role")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("role", roleAsJson(SLUG + "_wrong", ROLE))
                        .with("path", roleAsPath(SLUG + "_wrong", ROLE))
                        .with("uid", "1120000000000001")
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":1,\"fatal\":\"Role not existing.\"}", responseBody);

        responseBody = webClient
                .mutateWith(MockUser.tvm(idmTvmSourceId))
                .post()
                .uri("/idm/add-role")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("role", roleAsJson(SLUG, ROLE))
                        .with("path", roleAsPath(SLUG, ROLE))
                        .with("uid", "1120000000099991")
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals("{\"code\":1,\"fatal\":\"User with uid = [1120000000099991] not found.\"}",
                responseBody);
    }

    private String roleAsJson(String slug, String role) {
        return String.format("{\"%s\":\"%s\"}", slug, role);
    }

    private String roleAsPath(String slug, String role) {
        return String.format("/%s/%s", slug, role);
    }
}
