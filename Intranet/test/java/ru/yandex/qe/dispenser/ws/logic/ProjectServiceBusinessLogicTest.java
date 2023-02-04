package ru.yandex.qe.dispenser.ws.logic;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.KeyBase;
import ru.yandex.qe.dispenser.api.util.SerializationUtils;
import ru.yandex.qe.dispenser.api.v1.DiMetaField;
import ru.yandex.qe.dispenser.api.v1.DiMetaValueSet;
import ru.yandex.qe.dispenser.api.v1.DiProject;
import ru.yandex.qe.dispenser.api.v1.DiProjectServiceMeta;
import ru.yandex.qe.dispenser.api.v1.DiQuota;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.client.v1.impl.ProjectFilter;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.ws.ProjectMetaService;
import ru.yandex.qe.dispenser.ws.ProjectService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ProjectServiceBusinessLogicTest extends BusinessLogicTestBase {

    private static boolean containsProjectWithKey(@NotNull final DiListResponse<DiProject> projects, @NotNull final String key) {
        return projects.stream().anyMatch(p -> p.getKey().equals(key));
    }

    /**
     * {@link ProjectService#filterProjects}
     */
    @Test
    public void getMemberProjectsMethodShouldReturnAllLeafMemberProjects() {
        final DiListResponse<DiProject> projects = dispenser().projects().get().avaliableFor(LYADZHIN.getLogin()).perform();
        assertTrue(containsProjectWithKey(projects, INFRA));
        assertTrue(containsProjectWithKey(projects, VERTICALI));
        assertTrue(containsProjectWithKey(projects, DEFAULT));
        assertFalse(containsProjectWithKey(projects, YANDEX));
    }

    @Test
    public void getMemberProjectsShouldNotDependOnPersonalProjectsCreating() {
        dispenser().quotas().changeInService(NIRVANA).createEntity(UNIT_ENTITY, LYADZHIN.chooses(DEFAULT)).perform();
        getMemberProjectsMethodShouldReturnAllLeafMemberProjects();
    }

    @Test
    public void existingPersonShouldBeMemberOfDefaultProject() {
        final DiListResponse<DiProject> projects = dispenser().projects().get().avaliableFor("bendyna").perform();
        assertEquals(1, projects.size());
        assertTrue(containsProjectWithKey(projects, DEFAULT));
    }

    @Test
    public void dismissedPersonShouldBeMemberOfDefaultProject() {
        final DiListResponse<DiProject> projects = dispenser().projects().get().avaliableFor("welvet").perform();
        assertEquals(1, projects.size());
        assertTrue(containsProjectWithKey(projects, DEFAULT));
    }

    @Test
    public void notExistingPersonShouldNotBeMemberOfDefaultProject() {
        assertThrows(NotFoundException.class, () -> {
            assertTrue(dispenser().projects().get().avaliableFor("not-existing-person").perform().isEmpty());
        });
    }

    @Test
    public void fakeProjectsShouldNotBeShown() {
        dispenser().quotas().changeInService(NIRVANA).createEntity(UNIT_ENTITY, LYADZHIN.chooses(INFRA)).perform();
        final List<String> projectKeys = dispenser().projects().get().perform().stream().map(KeyBase::getKey).collect(Collectors.toList());
        final Set<String> uniqueProjectKeys = new HashSet<>(projectKeys);
        assertEquals(projectKeys.size(), uniqueProjectKeys.size());
    }

    /**
     * DISPENSER-337: Учитывать ответственных за проект как участников
     */
    @Test
    public void getAvaliableProjectsShouldReturnResponsibleProjectsToo() {
        final Set<DiProject> whistlerProjects = dispenser().projects().get()
                .avaliableFor(WHISTLER.getLogin())
                .perform()
                .stream()
                .collect(Collectors.toSet());
        final Set<DiProject> allLeafProjects = dispenser().projects().get()
                .filterBy(ProjectFilter.onlyLeafs())
                .perform()
                .stream()
                .collect(Collectors.toSet());

        assertEquals(whistlerProjects, allLeafProjects);
    }

    /**
     * DISPENSER-347: Админ Диспенсера не может добавить проект
     */
    @Test
    public void dispenserAdminCanCreateSubproject() {
        dispenser().project("web-ranking").create()
                .withParentProject(YANDEX)
                .withName("Web Ранжирование")
                .withDescription("Корневой проект для проектов web ранжирования.")
                .withAbcServiceId(TEST_ABC_SERVICE_ID)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiListResponse<DiProject> allProjects = dispenser().projects().get().perform();
        assertTrue(allProjects.stream().map(DiProject::getKey).anyMatch("web-ranking"::equals));
    }

    /**
     * DISPENSER-516: Учитывать админов Dispenser в ответственных проекта
     * <p>
     * {@link Hierarchy#getResponsibleProjects}
     */
    @Test
    public void dispenserAdminMustBeResponsibleOfAllProjects() {
        final DiListResponse<DiProject> adminProjects = dispenser().projects().get()
                .filterBy(ProjectFilter.responsible(AMOSOV_F.getLogin()))
                .perform();

        final DiListResponse<DiProject> allProjects = dispenser().projects().get().perform();
        assertEquals(adminProjects.size(), allProjects.size());
    }

    /**
     * DISPENSER-398: Обновление родительского проекта в методе UPDATE для проекта
     */
    @Test
    public void canChangeParentProject() {
        dispenser().project(DEFAULT).update()
                .withName("Default")
                .withDescription("Проект с дефолтной квотой")
                .withParentProject(INFRA)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiProject infra = dispenser().projects().get().withKey(DEFAULT).perform();
        assertEquals(INFRA, infra.getParentProjectKey());
    }

    /**
     * DISPENSER-597: Мета-информация к проектам
     * <p>
     * {@link ProjectMetaService#getProjectMeta}
     */
    @Test
    public void returnEmptyProjectMetaIfNotExists() {
        final DiMetaValueSet actualMeta = dispenser().projects().getMeta()
                .inService(CLUSTER_API)
                .ofProject(YANDEX)
                .perform();
        assertTrue(actualMeta.getKeys().isEmpty());
    }

    /**
     * DISPENSER-597: Мета-информация к проектам
     * <p>
     * {@link ProjectMetaService#putProjectMeta}
     */
    @Test
    public void putMustOverrideProjectMetaIfExists() {
        final DiMetaField<Double> weight = DiMetaField.of("weight", DiMetaField.Type.POSITIVE_DOUBLE);

        createLocalClient()
                .path("/v1/project-metas/" + CLUSTER_API + "/" + YANDEX)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(SerializationUtils.convertValue(DiMetaValueSet.builder().set(weight, 2.0).build(), Map.class));

        createLocalClient()
                .path("/v1/project-metas/" + CLUSTER_API + "/" + YANDEX)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(SerializationUtils.convertValue(DiMetaValueSet.builder().set(weight, 3.0).build(), Map.class));

        final DiMetaValueSet actualMeta = dispenser().projects().getMeta()
                .inService(CLUSTER_API)
                .ofProject(YANDEX)
                .perform();
        assertEquals(3.0, actualMeta.getValue(weight));
    }

    /**
     * DISPENSER-670: Вызов для получения метаинформации по всем проектам диспенсера
     * <p>
     * {@link ProjectMetaService#getProjectMetas}
     */
    @Test
    public void allProjectsMustBeInGetAllMetasResult() {
        final DiListResponse<DiProjectServiceMeta> allMetas = dispenser().projects()
                .getMeta()
                .inService(CLUSTER_API)
                .perform();

        final List<String> metaProjectKeys = allMetas.stream()
                .map(DiProjectServiceMeta::getProject)
                .map(KeyBase::getKey)
                .sorted()
                .collect(Collectors.toList());
        final List<String> allProjectKeys = dispenser().projects().get()
                .perform()
                .stream()
                .map(KeyBase::getKey)
                .sorted()
                .collect(Collectors.toList());

        assertEquals(metaProjectKeys, allProjectKeys);
    }

    /**
     * DISPENSER-670: Вызов для получения метаинформации по всем проектам диспенсера
     * <p>
     * {@link ProjectMetaService#getProjectMetas}
     */
    @Test
    public void onlyRealProjectsMustBeInGetAllMetasResult() {
        dispenser().quotas().changeInService(NIRVANA).createEntity(randomUnitEntity(), LYADZHIN.chooses(INFRA)).perform();

        final DiListResponse<DiProjectServiceMeta> allMetas = dispenser().projects()
                .getMeta()
                .inService(CLUSTER_API)
                .perform();

        final List<String> metaProjectKeys = allMetas.stream()
                .map(DiProjectServiceMeta::getProject)
                .map(KeyBase::getKey)
                .sorted()
                .collect(Collectors.toList());
        final List<String> allProjectKeys = dispenser().projects().get()
                .perform()
                .stream()
                .map(KeyBase::getKey)
                .sorted()
                .collect(Collectors.toList());

        assertEquals(metaProjectKeys, allProjectKeys);
    }

    private static DiProject.Builder createSubprojectOf(@NotNull final String parentKey) {
        return DiProject.withKey("new-project")
                .withParentProject(parentKey)
                .withName("New Project")
                .withDescription("Some description")
                .withAbcServiceId(TEST_ABC_SERVICE_ID);
    }

    @Test
    public void insertedSubprojectShouldContainOnlyExistingDescendants() {
        assertThrows(BadRequestException.class, () -> {
            final DiProject project = createSubprojectOf(YANDEX).withSubprojects(INFRA, "not-existing-project").build();
            dispenser().projects().create(project).performBy(WHISTLER);
        });
    }

    @Test
    public void insertedSubprojectShouldContainOnlyDescendantsOfParent() {
        assertThrows(BadRequestException.class, () -> {
            final DiProject project = createSubprojectOf(YANDEX).withSubprojects(VERTICALI, INFRA_SPECIAL).build();
            dispenser().projects().create(project).performBy(WHISTLER);
        });
    }

    @Test
    public void onlyParentResponsibleCanInsertedSubproject() {
        assertThrows(ForbiddenException.class, () -> {
            final DiProject project = createSubprojectOf(YANDEX).withSubprojects(VERTICALI, INFRA).build();
            dispenser().projects().create(project).performBy(LYADZHIN);
        });
    }

    @Test
    public void insertedSubprojectShouldBeCreatedCorrectly() {
        final DiProject projectToInsert = createSubprojectOf(YANDEX).withSubprojects(SEARCH, INFRA, VERTICALI).build();
        dispenser().projects().create(projectToInsert).performBy(WHISTLER);

        updateHierarchy();

        // Check subprojects parent
        assertEquals(dispenser().projects().get().withKey(SEARCH).perform().getParentProjectKey(), projectToInsert.getKey());
        assertEquals(dispenser().projects().get().withKey(INFRA).perform().getParentProjectKey(), projectToInsert.getKey());
        assertEquals(dispenser().projects().get().withKey(VERTICALI).perform().getParentProjectKey(), projectToInsert.getKey());

        // Check quota maxes
        final DiQuotaGetResponse quotas = dispenser().quotas().get().perform();
        final ImmutableSet<String> childProjects = ImmutableSet.of(SEARCH, INFRA, VERTICALI);

        quotas.stream()
                .map(DiQuota::getSpecification)
                .distinct()
                .forEach(specification -> {

                    final Map<Set<String>, Long> subprojectMaxSums = quotas.stream()
                            .filter(q -> childProjects.contains(q.getProject().getKey()) && q.getSpecification().equals(specification))
                            .collect(Collectors.groupingBy(DiQuota::getSegmentKeys, Collectors.summingLong(q -> q.getMax().getValue())));

                    final Map<Set<String>, Long> insertedProjectMaxes = quotas.stream()
                            .filter(q -> projectToInsert.getKey().equals(q.getProject().getKey()) && q.getSpecification().equals(specification))
                            .collect(Collectors.toMap(DiQuota::getSegmentKeys, q -> q.getMax().getValue()));

                    assertEquals(insertedProjectMaxes, subprojectMaxSums);

                });
    }

    @Test
    public void onlyDispenserAdminsCanChangeProjectParent() {
        assertThrows(BadRequestException.class, () -> {

            dispenser().project(DEFAULT).update()
                    .withName("default")
                    .withDescription("default project")
                    .withParentProject(INFRA)
                    .performBy(WHISTLER);

            updateHierarchy();
        });
    }

    @Test
    public void projectCanBeMovedToProjectWithoutQuotas() {

        dispenser().projects()
                .create(DiProject.withKey("no-quotas")
                        .withParentProject(YANDEX)
                        .withName("no-quotas")
                        .withDescription("no-desc")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser().project(DEFAULT).update()
                .withName("default")
                .withDescription("default project")
                .withParentProject("no-quotas")
                .performBy(AMOSOV_F);
    }

    @Test
    public void newParentCantBeInOriginsSubtree() {

        assertThrows(BadRequestException.class, () -> {
            dispenser().project(SEARCH).update()
                    .withName("search")
                    .withDescription("search project")
                    .withParentProject(INFRA_SPECIAL)
                    .performBy(AMOSOV_F);
        });

    }

    @Test
    public void checkUpdatedQuotas() {

        dispenser().project(DEFAULT).update()
                .withName("default")
                .withDescription("default project")
                .withParentProject(INFRA)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiProject defaultProject = dispenser().projects()
                .get()
                .withKey(DEFAULT)
                .perform();

        assertEquals(INFRA, defaultProject.getParentProjectKey());

        final DiQuotaGetResponse quotas = dispenser().quotas()
                .get()
                .ofProject(INFRA)
                .perform();

        long max = 0;
        long actual = 0;

        for (final DiQuota q : quotas) {
            if (q.getSpecification().getKey().equals("computer")) {
                max += q.getMax().getValue();
                actual += q.getActual().getValue();
            }
        }

        assertEquals(50, max);
        assertEquals(0, actual);
    }

    @Test
    public void checkUpdatedQuotasInOldAndNewParents() {
        dispenser().project(INFRA_SPECIAL).update()
                .withName("infra-special")
                .withDescription("infra-special project")
                .withParentProject(VERTICALI)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaGetResponse quotasVerticali = dispenser().quotas()
                .get()
                .ofProject(VERTICALI)
                .perform();

        final DiQuotaGetResponse quotasSearch = dispenser().quotas()
                .get()
                .ofProject(SEARCH)
                .perform();

        long maxValueInVerticali = 0;

        for (final DiQuota q : quotasVerticali) {
            if (q.getSpecification().getKey().equals("yt-cpu")) {
                maxValueInVerticali += q.getMax().getValue();
            }
        }
        assertEquals(25000L, maxValueInVerticali);

        long maxValueInSearch = 0;

        for (final DiQuota q : quotasSearch) {
            if (q.getSpecification().getKey().equals("yt-cpu")) {
                maxValueInSearch += q.getMax().getValue();
            }
        }
        assertEquals(35000L, maxValueInSearch);
    }

    @Test
    public void cantCreateProjectWithoutAbcServiceId() {
        assertThrows(BadRequestException.class, () -> {
            final DiProject project = createSubprojectOf(YANDEX).withAbcServiceId(null).build();
            dispenser().projects().create(project).performBy(WHISTLER);
        });
    }

    @Test
    public void cantCreateProjectWithZeroAbcServiceId() {
        assertThrows(BadRequestException.class, () -> {
            final DiProject project = createSubprojectOf(YANDEX).withAbcServiceId(0).build();
            dispenser().projects().create(project).performBy(WHISTLER);
        });
    }

    @Test
    public void cantCreateProjectWithNegativeAbcServiceId() {
        assertThrows(BadRequestException.class, () -> {
            final DiProject project = createSubprojectOf(YANDEX).withAbcServiceId(-1).build();
            dispenser().projects().create(project).performBy(WHISTLER);
        });
    }

    @Test
    public void canGetAbcServiceIdAfterProjectCreation() {
        final DiProject project = createSubprojectOf(YANDEX).build();
        final DiProject createdProject = dispenser().projects().create(project).performBy(WHISTLER);
        assertEquals(TEST_ABC_SERVICE_ID, createdProject.getAbcServiceId());

        updateHierarchy();

        final DiProject requestedProject = dispenser().projects().get().withKey(project.getKey()).perform();
        assertEquals(TEST_ABC_SERVICE_ID, requestedProject.getAbcServiceId());
    }

    @Test
    public void canUpdateAbcServiceId() {
        final DiProject project = createSubprojectOf(YANDEX).build();
        dispenser().projects().create(project).performBy(WHISTLER);

        updateHierarchy();

        final Integer newAbcServiceId = 1234;
        final DiProject updatedProject = dispenser().projects()
                .update(DiProject.copyOf(project).withAbcServiceId(newAbcServiceId).build())
                .performBy(WHISTLER);
        assertEquals(newAbcServiceId, updatedProject.getAbcServiceId());

        updateHierarchy();

        final DiProject requestedProject = dispenser().projects().get().withKey(project.getKey()).perform();
        assertEquals(newAbcServiceId, requestedProject.getAbcServiceId());
    }

    @Test
    public void cantRemoveAbcServiceId() {
        assertThrows(BadRequestException.class, () -> {
            final DiProject project = createSubprojectOf(YANDEX).build();
            dispenser().projects().create(project).performBy(WHISTLER);
    
            updateHierarchy();
    
            final DiProject projectWithoutId = DiProject.copyOf(project).withAbcServiceId(null).build();
            dispenser().projects().update(projectWithoutId).performBy(WHISTLER);
        });
    }

    @Test
    public void canAddAbcServiceIdForExistingProject() {
        final DiProject project = dispenser().projects().get().withKey(DEFAULT).perform();
        final DiProject projectWithId = DiProject.copyOf(project).withAbcServiceId(TEST_ABC_SERVICE_ID).build();
        final DiProject updatedProject = dispenser().projects().update(projectWithId).performBy(WHISTLER);
        assertEquals(TEST_ABC_SERVICE_ID, updatedProject.getAbcServiceId());

        updateHierarchy();

        final DiProject requestedProject = dispenser().projects().get().withKey(project.getKey()).perform();
        assertEquals(TEST_ABC_SERVICE_ID, requestedProject.getAbcServiceId());
    }
}
