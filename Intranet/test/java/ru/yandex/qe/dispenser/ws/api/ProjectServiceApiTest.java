package ru.yandex.qe.dispenser.ws.api;

import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.util.SerializationUtils;
import ru.yandex.qe.dispenser.api.v1.DiMetaField;
import ru.yandex.qe.dispenser.api.v1.DiMetaValueSet;
import ru.yandex.qe.dispenser.api.v1.DiPersonGroup;
import ru.yandex.qe.dispenser.api.v1.DiProject;
import ru.yandex.qe.dispenser.api.v1.DiProjectServiceMeta;
import ru.yandex.qe.dispenser.api.v1.DiYandexGroupType;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.ws.ProjectMetaService;
import ru.yandex.qe.dispenser.ws.ProjectService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ProjectServiceApiTest extends ApiTestBase {
    @NotNull
    private DiProject createTestProject() {
        return dispenser().project("nirvana-debug-qdeee").create()
                .withParentProject(YANDEX)
                .withName("Otladochka")
                .withDescription("Opisanie")
                .withAbcServiceId(TEST_ABC_SERVICE_ID)
                .withResponsibles(DiPersonGroup.builder().addPersons(QDEEE.getLogin()).build())
                .withMembers(DiPersonGroup.builder().addPersons(QDEEE.getLogin()).addYaGroups(DiYandexGroupType.DEPARTMENT, YANDEX).build())
                .performBy(WHISTLER);
    }

    @Test
    public void createProjectRegressionTest() {
        disableHierarchy();

        final DiProject project = createTestProject();

        assertLastMethodEquals(HttpMethod.POST);
        assertLastPathEquals("/api/v1/projects/yandex/create-subproject");
        assertLastRequestBodyEquals("/body/project/create/new-project.json");
        assertLastResponseEquals("/body/project/create/new-project.json");
        assertJsonEquals("/body/project/create/new-project.json", project);
    }

    @Test
    public void membersShouldBeReturndUnderFlag() {
        disableHierarchy();

        createTestProject();

        {
            final DiListResponse<DiProject> projects = dispenser()
                    .projects()
                    .get()
                    .avaliableFor("qdeee")
                    .showPersons(true)
                    .perform();

            assertTrue(projects.size() == 2);
            final DiProject project = projects.stream().filter(p -> "nirvana-debug-qdeee".equals(p.getKey())).findAny().get();
            assertTrue(project.getMembers().getPersons().size() == 1);
            assertTrue(project.getMembers().getPersons().iterator().next().equals("qdeee"));
            assertTrue(project.getMembers().getYandexGroups(DiYandexGroupType.DEPARTMENT).size() == 1);
            assertTrue(project.getMembers().getYandexGroups(DiYandexGroupType.DEPARTMENT).iterator().next().equals("yandex"));
        }

        {
            final DiListResponse<DiProject> projects = dispenser()
                    .projects()
                    .get()
                    .avaliableFor("qdeee")
                    .showPersons(false)
                    .perform();
            assertTrue(projects.size() == 2);
            final DiProject project = projects.stream().filter(p -> "nirvana-debug-qdeee".equals(p.getKey())).findAny().get();
            assertTrue(project.getMembers().getPersons().isEmpty());
            assertTrue(project.getMembers().getYandexGroups(DiYandexGroupType.DEPARTMENT).isEmpty());
        }

    }

    @Test
    public void getProjectRegressionTest() {
        final DiProject project = dispenser().projects().get().withKey(YANDEX).perform();

        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathQueryEquals("/api/v1/projects/yandex");
        assertLastResponseHeaderEquals(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8");
        assertLastResponseEquals("/body/project/read/project.json");
        assertJsonEquals("/body/project/read/project.json", project);
    }

    /**
     * {@link ProjectService#filterProjects}
     */
    @Test
    public void getMemberProjectsRegressionTest() {
        final DiListResponse<DiProject> projects = dispenser().projects().get().avaliableFor(LYADZHIN.getLogin()).perform();

        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathQueryEquals("/api/v1/projects?leaf=true&member=lyadzhin");
        assertLastResponseEquals("/body/project/member-projects/lyadzhin.json");
        assertJsonEquals("/body/project/member-projects/lyadzhin.json", projects);
    }

    @Test
    public void updateProjectRegressionTest() {
        disableHierarchy();

        final DiProject project = dispenser().project(YANDEX)
                .update()
                .withName("New Yandex")
                .withDescription("New Yandex description")
                .withResponsibles(DiPersonGroup.builder().addPersons(WHISTLER.getLogin()).build())
                .withMembers(DiPersonGroup.builder().addPersons(LYADZHIN.getLogin()).addYaGroups(DiYandexGroupType.DEPARTMENT, YANDEX).build())
                .performBy(WHISTLER);

        assertLastMethodEquals(HttpMethod.POST);
        assertLastPathEquals("/api/v1/projects/yandex");
        assertLastRequestBodyEquals("/body/project/update/project/req.json");
        assertLastResponseEquals("/body/project/update/project/resp.json");
        assertJsonEquals("/body/project/update/project/resp.json", project);
    }

    @Test
    public void attachAndDetachMembersRegressionTest() {
        disableHierarchy();

        final DiProject infraWithQdeee = dispenser().project(INFRA)
                .members()
                .attach(QDEEE.getLogin())
                .performBy(WHISTLER);

        assertLastMethodEquals(HttpMethod.POST);
        assertLastPathEquals("/api/v1/projects/infra/attach-members");
        assertLastRequestBodyEquals("/body/project/update/members/members.json");
        assertLastResponseEquals("/body/project/update/members/attach-members.json");
        assertJsonEquals("/body/project/update/members/attach-members.json", infraWithQdeee);

        final DiProject infraWithoutQdeee = dispenser().project(INFRA)
                .members()
                .detach(QDEEE.getLogin())
                .performBy(WHISTLER);

        assertLastMethodEquals(HttpMethod.POST);
        assertLastPathEquals("/api/v1/projects/infra/detach-members");
        assertLastRequestBodyEquals("/body/project/update/members/members.json");
        assertLastResponseEquals("/body/project/update/members/detach-members.json");
        assertJsonEquals("/body/project/update/members/detach-members.json", infraWithoutQdeee);
    }

    @Test
    public void attachAndDetachResponsiblesRegressionTest() {
        disableHierarchy();

        final DiProject infraWithLyadzhin = dispenser().project(INFRA)
                .responsibles()
                .attach(LYADZHIN.getLogin())
                .performBy(WHISTLER);

        assertLastMethodEquals(HttpMethod.POST);
        assertLastPathEquals("/api/v1/projects/infra/attach-responsibles");
        assertLastRequestBodyEquals("/body/project/update/responsibles/responsibles.json");
        assertLastResponseEquals("/body/project/update/responsibles/attach-responsibles.json");
        assertJsonEquals("/body/project/update/responsibles/attach-responsibles.json", infraWithLyadzhin);

        final DiProject infraWithoutLyadzhin = dispenser().project(INFRA)
                .responsibles()
                .detach(LYADZHIN.getLogin())
                .performBy(WHISTLER);

        assertLastMethodEquals(HttpMethod.POST);
        assertLastPathEquals("/api/v1/projects/infra/detach-responsibles");
        assertLastRequestBodyEquals("/body/project/update/responsibles/responsibles.json");
        assertLastResponseEquals("/body/project/update/responsibles/detach-responsibles.json");
        assertJsonEquals("/body/project/update/responsibles/detach-responsibles.json", infraWithoutLyadzhin);
    }

    /**
     * DISPENSER-597: Мета-информация к проектам
     * <p>
     * {@link ProjectMetaService#getProjectMeta}
     * {@link ProjectMetaService#putProjectMeta}
     */
    @Test
    public void putAndGetProjectMetaRegressionTest() {
        final DiMetaField<Double> ratio = DiMetaField.of("ratio", DiMetaField.Type.ZERO_ONE_DOUBLE);
        final DiMetaField<Double> weight = DiMetaField.of("weight", DiMetaField.Type.POSITIVE_DOUBLE);
        final DiMetaField<Long> resource = DiMetaField.of("resource", DiMetaField.Type.POSITIVE_LONG);

        final DiMetaValueSet expectedMeta = DiMetaValueSet.builder()
                .set(ratio, 0.9)
                .set(weight, 2.0)
                .set(resource, 100L)
                .build();
        createLocalClient()
                .path("/v1/project-metas/" + CLUSTER_API + "/" + YANDEX)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(SerializationUtils.convertValue(expectedMeta, Map.class));

        assertLastMethodEquals(HttpMethod.POST);
        assertLastPathEquals("/api/v1/project-metas/cluster-api/yandex");
        assertLastRequestBodyEquals("/body/project/meta/put-req.json");
        assertLastResponseEquals("/body/project/meta/put-resp.json");
        assertJsonEquals("/body/project/meta/put-resp.json", expectedMeta);

        final DiMetaValueSet actualMeta = dispenser().projects().getMeta()
                .inService(CLUSTER_API)
                .ofProject(YANDEX)
                .perform();

        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathEquals("/api/v1/project-metas/cluster-api/yandex");
        assertLastResponseEquals("/body/project/meta/get-resp.json");
        assertJsonEquals("/body/project/meta/get-resp.json", actualMeta);

        assertEquals(2.0, expectedMeta.getValue(weight));
        assertEquals(0.9, expectedMeta.getValue(ratio));
        assertEquals(Long.valueOf(100), expectedMeta.getValue(resource));
    }

    /**
     * DISPENSER-670: Вызов для получения метаинформации по всем проектам диспенсера
     * <p>
     * {@link ProjectMetaService#getProjectMetas}
     */
    @Test
    public void getAllProjectMetasRegressionTest() {
        final DiMetaField<Double> ratio = DiMetaField.of("ratio", DiMetaField.Type.ZERO_ONE_DOUBLE);

        createLocalClient()
                .path("/v1/project-metas/" + CLUSTER_API + "/" + YANDEX)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(SerializationUtils.convertValue(DiMetaValueSet.builder().set(ratio, 1.0).build(), Map.class));

        final DiListResponse<DiProjectServiceMeta> actualMetas = dispenser().projects()
                .getMeta()
                .inService(CLUSTER_API)
                .perform();

        assertLastMethodEquals(HttpMethod.GET);
        assertLastPathQueryEquals("/api/v1/project-metas?service=cluster-api");
        assertLastResponseEquals("/body/project/meta/get-all-resp.json");
        assertJsonEquals("/body/project/meta/get-all-resp.json", actualMetas);
    }

    @Test
    public void stringAbcIdRegressionTest() {
        disableHierarchy();
        createProjectWithBodyFromClasspath("/body/project/create/abc-service-id-string.json");
        assertLastResponseEquals("/body/project/create/new-project.json");
    }

    @Test
    public void emptyAbcIdRegressionTest() {
        createProjectWithBodyFromClasspath("/body/project/create/abc-service-id-empty.json");
        assertLastResponseStatusEquals(HttpStatus.SC_BAD_REQUEST);
    }

    private void createProjectWithBodyFromClasspath(@NotNull final String path) {
        createLocalClient()
                .path("/v1/projects/" + YANDEX + "/create-subproject")
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + WHISTLER)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(fromClasspath(path));
    }
}
