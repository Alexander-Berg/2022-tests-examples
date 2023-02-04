package ru.yandex.qe.dispenser.ws.logic;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiPersonGroup;
import ru.yandex.qe.dispenser.api.v1.DiProject;
import ru.yandex.qe.dispenser.api.v1.DiProjectServiceMeta;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.DiYandexGroupType;
import ru.yandex.qe.dispenser.api.v1.field.DiField;
import ru.yandex.qe.dispenser.api.v1.field.DiProjectFields;
import ru.yandex.qe.dispenser.api.v1.project.DiExtendedProject;
import ru.yandex.qe.dispenser.api.v1.project.DiProjectFieldsHolder;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.request.DiEntityUsage;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.abc.AbcPerson;
import ru.yandex.qe.dispenser.domain.abc.AbcRole;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceMember;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceReference;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceResponsible;
import ru.yandex.qe.dispenser.domain.bot.BigOrder;
import ru.yandex.qe.dispenser.domain.dao.campaign.CampaignDao;
import ru.yandex.qe.dispenser.domain.dao.group.GroupDao;
import ru.yandex.qe.dispenser.domain.dao.person.StaffCache;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.domain.util.StreamUtils;
import ru.yandex.qe.dispenser.standalone.MockAbcApi;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;
import ru.yandex.qe.dispenser.ws.bot.BigOrderManager;
import ru.yandex.qe.dispenser.ws.intercept.TransactionWrapper;
import ru.yandex.qe.dispenser.ws.reqbody.ProjectBody;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.ws.logic.QuotaChangeRequestValidationTest.defaultCampaignBuilder;

public class ProjectValidationTest extends BusinessLogicTestBase {
    @Autowired
    private StaffCache staffCache;

    @Autowired
    private MockAbcApi mockAbcApi;

    @Autowired
    private CampaignDao campaignDao;

    @Autowired
    private BigOrderManager botBigOrderManager;

    @Autowired
    private GroupDao groupDao;

    @Test
    public void calculatingPermissionsForPerformerShouldWorkCorrectly() {
        botBigOrderManager.clear();
        BigOrder bigOrder = botBigOrderManager.create(BigOrder.builder(LocalDate.now()));
        campaignDao.create(defaultCampaignBuilder(bigOrder).build());
        final String projectKey = "Test";
        dispenser().projects()
                .create(DiProject.withKey(projectKey)
                        .withName(projectKey)
                        .withDescription(projectKey)
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .withParentProject(YANDEX)
                        .withResponsibles(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .withMembers(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final Set<DiExtendedProject.Permission> permissionsForMember = getPermissionsFor(projectKey, BINARY_CAT);

        assertTrue(permissionsForMember.contains(DiExtendedProject.Permission.CAN_CREATE_QUOTA_REQUEST));

        final Set<DiExtendedProject.Permission> permissionsForNotMember = getPermissionsFor(projectKey, LOTREK);
        assertTrue(permissionsForNotMember.isEmpty());
    }

    @Test
    public void testCanViewBotPreorderCostsPermission() {
        botBigOrderManager.clear();
        BigOrder bigOrder = botBigOrderManager.create(BigOrder.builder(LocalDate.now()));
        campaignDao.create(defaultCampaignBuilder(bigOrder).build());
        final String projectKey = "Test";
        dispenser().projects()
                .create(DiProject.withKey(projectKey)
                        .withName(projectKey)
                        .withDescription(projectKey)
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .withParentProject(YANDEX)
                        .withResponsibles(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .withMembers(DiPersonGroup.builder().addPersons(BINARY_CAT.getLogin()).build())
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final Set<DiExtendedProject.Permission> permissionsForResponsible = getPermissionsFor(projectKey, BINARY_CAT);

        assertFalse(permissionsForResponsible.contains(DiExtendedProject.Permission.CAN_VIEW_BOT_PREORDER_COSTS));

        final Set<DiExtendedProject.Permission> permissionsForNotResponsible = getPermissionsFor(projectKey, LOTREK);
        assertFalse(permissionsForNotResponsible.contains(DiExtendedProject.Permission.CAN_VIEW_BOT_PREORDER_COSTS));

        final Set<DiExtendedProject.Permission> permissionsForProcessResponsible = getPermissionsFor(projectKey, KEYD);

        assertTrue(permissionsForProcessResponsible.contains(DiExtendedProject.Permission.CAN_VIEW_BOT_PREORDER_COSTS));

        final Set<DiExtendedProject.Permission> permissionsForProviderAdmins = getPermissionsFor(projectKey, SANCHO);

        assertFalse(permissionsForProviderAdmins.contains(DiExtendedProject.Permission.CAN_VIEW_BOT_PREORDER_COSTS));

    }

    private Set<DiExtendedProject.Permission> getPermissionsFor(final String projectKey, final DiPerson person) {
        return createAuthorizedLocalClient(person)
                .path("/v1/projects/" + projectKey)
                .query("field", "permissions")
                .get(DiExtendedProject.class)
                .getPermissions();
    }

    /**
     * DISPENSER-398: Обновление родительского проекта в методе UPDATE для проекта
     */
    @Test
    public void projectWithQuotaCantBeParent() {
        Assumptions.assumeFalse(isStubMode(), "Real projects don't have created personal subprojects");
        assertThrows(BadRequestException.class, () -> {
            dispenser().quotas()
                    .changeInService(NIRVANA)
                    .createEntity(UNIT_ENTITY, LYADZHIN.chooses(VERTICALI))
                    .perform();

            updateHierarchy();

            dispenser().project(INFRA).update()
                    .withName("Infrastruktura")
                    .withDescription("descr")
                    .withParentProject(VERTICALI)
                    .performBy(AMOSOV_F);
        });
    }

    /**
     * DISPENSER-398: Обновление родительского проекта в методе UPDATE для проекта
     */
    @Test
    public void projectCantBeSelfParent() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().project(INFRA).update()
                    .withName("Infrastruktura")
                    .withDescription("descr")
                    .withParentProject(INFRA)
                    .performBy(AMOSOV_F);
        });
    }

    /**
     * DISPENSER-398: Обновление родительского проекта в методе UPDATE для проекта
     */
    @Test
    public void rootCantChangeParent() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().project(YANDEX).update()
                    .withName("Yandex")
                    .withDescription("Корневой проект Dispenser")
                    .withParentProject(INFRA)
                    .performBy(AMOSOV_F);
        });
    }

    /**
     * DISPENSER-398: Обновление родительского проекта в методе UPDATE для проекта
     */
    @Test
    public void parentChangePerformerMustBeResponsibleOfNewParent() {
        assertThrows(ForbiddenException.class, () -> {
            dispenser().project(INFRA).update()
                    .withName("Infrastruktura")
                    .withDescription("descr")
                    .withResponsibles(DiPersonGroup.builder().addPersons(LYADZHIN.getLogin()).build())
                    .performBy(WHISTLER);

            dispenser().project(INFRA).update()
                    .withName("Infrastruktura")
                    .withDescription("descr")
                    .withParentProject(VERTICALI)
                    .performBy(LYADZHIN);
        });
    }

    /**
     * DISPENSER-409: Пустые пользователи в json-е проекта
     * <p>
     * {@link DiPersonGroup.Builder}
     */
    @Test
    public void failIfNewMemberHasEmptyLogin() {
        final Map<String, Object> body = ImmutableMap.of(
                "key", "sdch",
                "name", "SDCH",
                "description", "descr",
                "members", ImmutableMap.of("persons", new String[]{""}, "yandexGroups", Collections.emptyMap())
        );
        final int status = createLocalClient()
                .path("/v1/projects/yandex/create-subproject")
                .header("Content-Type", "application/json")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + WHISTLER)
                .post(body)
                .getStatus();
        assertTrue(status != HttpStatus.SC_OK);
    }

    /**
     * DISPENSER-963: При редактировании проекта разрешить оставлять уволившихся
     * <p>
     * {@link ProjectBody#requireExistingPersons}
     */
    @Test
    public void doNotfailIfPersonIsDismissed() {
        dispenser().project(INFRA).update()
                .withName("Infrastruktura")
                .withDescription("descr")
                .withResponsibles(DiPersonGroup.builder().addPersons("welvet").build())
                .performBy(AMOSOV_F);
        updateHierarchy();
        assertTrue(dispenser().projects().get().withKey(INFRA).perform().getResponsibles().getPersons().contains("welvet"));
    }

    /**
     * DISPENSER-520: Синхронизировать валидации на frontend и backend
     * <p>
     * {@link ProjectBody#requireExistingPersons}
     */
    @Test
    public void failIfPersonNotExistsAtStaff() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().project(INFRA).update()
                    .withName("Infrastruktura")
                    .withDescription("descr")
                    .withResponsibles(DiPersonGroup.builder().addPersons("not-existing-person").build())
                    .performBy(AMOSOV_F);
        });
    }

    /**
     * DISPENSER-520: Синхронизировать валидации на frontend и backend
     * <p>
     * {@link ProjectBody#requireExistingPersons}
     */
    @Test
    public void failIfYandexGroupNotExistsAtStaff() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().project(INFRA).update()
                    .withName("Infrastruktura")
                    .withDescription("descr")
                    .withMembers(DiPersonGroup.builder().addYaGroups(DiYandexGroupType.DEPARTMENT, "my_super_group").build())
                    .performBy(AMOSOV_F);
        });
    }

    /**
     * DISPENSER-520: Синхронизировать валидации на frontend и backend
     * <p>
     * {@link ProjectBody#requireExistingPersons}
     */
    @Test
    public void failIfInvalidDescription() {
        assertThrows(BadRequestException.class, () -> {
            dispenser().project(INFRA).update()
                    .withName("Infrastruktura")
                    .withDescription(StringUtils.repeat("pizza", 600))
                    .performBy(AMOSOV_F);
        });
    }

    /**
     * DISPENSER-551: Невалидный проект сохраняется
     * <p>
     * {@link TransactionWrapper#execute}
     */
    @Test
    public void invalidProjectShouldNotBeSaved() {
        ignoreInStub();
        try {
            dispenser().project(DEFAULT).update()
                    .withName("Default")
                    .withDescription(StringUtils.repeat("pizza", 600))
                    .performBy(AMOSOV_F);
            Assertions.fail();
        } catch (BadRequestException ignored) {
        }
        updateHierarchy();
        Assertions.assertNotEquals("Des", dispenser().projects().get().withKey(DEFAULT).perform().getDescription());
    }

    private void ignoreInStub() {
        Assumptions.assumeFalse(isStubMode(), "Not working in stub mode");
    }

    /**
     * DISPENSER-551: Невалидный проект сохраняется
     * <p>
     * {@link ProjectBody#requireNoResponsibleGroups}
     */
    @Test
    public void yandexGroupCantBeResponsibleOfProject() throws BadRequestException {
        assertThrows(BadRequestException.class, () -> {
            final DiPersonGroup responsibles = DiPersonGroup.builder()
                    .addYaGroups(DiYandexGroupType.DEPARTMENT, "yandex_search_tech_searchinfradev_serp_user_logsng")
                    .build();
            dispenser().project(INFRA).update()
                    .withName("Infrastruktura")
                    .withDescription("Des")
                    .withResponsibles(responsibles)
                    .performBy(AMOSOV_F);
        });
    }

    @Test
    public void removedProjectCantBeFetched() {
        dispenser().project(INFRA).delete().performBy(AMOSOV_F);

        updateHierarchy();

        assertThrowsWithMessage(() -> dispenser().project(INFRA).get().perform(), HttpStatus.SC_NOT_FOUND, "No project with key = [infra]!");
    }

    @Test()
    public void parentProjectNotContainsRemovedProject() {
        dispenser().project(INFRA).delete().performBy(AMOSOV_F);

        updateHierarchy();

        final DiProject parentProject = dispenser().projects().get().withKey(YANDEX).perform();
        assertFalse(parentProject.getSubprojectKeys().contains(INFRA));
    }

    @Test
    public void removedProjectHasNoMeta() {
        final DiListResponse<DiProjectServiceMeta> oldMetas = dispenser().projects().getMeta()
                .perform();

        assertTrue(oldMetas.stream()
                .anyMatch(meta -> meta.getProject().getKey().equals(INFRA)));

        dispenser().project(INFRA).delete().performBy(AMOSOV_F);

        updateHierarchy();

        final DiListResponse<DiProjectServiceMeta> newMetas = dispenser().projects().getMeta()
                .perform();

        assertFalse(newMetas.stream()
                .anyMatch(meta -> meta.getProject().getKey().equals(INFRA)));
    }

    @Test
    public void removedProjectHasQuotas() {

        final DiQuotaGetResponse beforeQuotas = dispenser().quotas().get()
                .perform();

        assertTrue(beforeQuotas.stream()
                .anyMatch(quota -> quota.getProject().getKey().equals(INFRA)));

        dispenser().project(INFRA).delete().performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaGetResponse afterQuotas = dispenser().quotas().get()
                .perform();

        assertTrue(afterQuotas.stream()
                .anyMatch(quota -> quota.getProject().getKey().equals(INFRA)));
    }

    @Test
    public void projectCantBeRemovedWithNonZeroQuotaUsagesOfServiceWithCorrespondingSetting() {
        final HashSet<String> segments = Sets.newHashSet(DC_SEGMENT_2, SEGMENT_SEGMENT_1);

        dispenser().service(YP).syncState().quotas()
                .changeQuota(DiQuotaState
                        .forResource(SEGMENT_STORAGE)
                        .forProject(INFRA_SPECIAL)
                        .withSegments(segments)
                        .withActual(DiAmount.of(512, DiUnit.BYTE))
                        .build())
                .perform();

        final DiEntity file = DiEntity.withKey("file")
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.of(100, DiUnit.GIBIBYTE))
                .build();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(file, LYADZHIN.chooses(INFRA_SPECIAL))
                .perform();

        updateHierarchy();

        final BadRequestException exception = assertThrows(BadRequestException.class, () -> dispenser().project(SEARCH).delete().performBy(WHISTLER));
        assertTrue(exception.getMessage().contains("Project can't be deleted or can't become a parent due to non-zero quota usage"));

        dispenser().service(YP).syncState().quotas()
                .changeQuota(DiQuotaState
                        .forResource(SEGMENT_STORAGE)
                        .forProject(INFRA_SPECIAL)
                        .withSegments(segments)
                        .withActual(DiAmount.of(0, DiUnit.BYTE))
                        .build())
                .perform();

        updateHierarchy();

        dispenser().project(SEARCH).delete().performBy(WHISTLER);
    }

    @Test
    public void noCacheQueryParamAffectProjectReading() {

        final String newProject = "new-project";

        dispenser().projects()
                .create(DiProject.withKey(newProject)
                        .withName("New project")
                        .withDescription("New project")
                        .withAbcServiceId(AcceptanceTestBase.TEST_ABC_SERVICE_ID)
                        .withParentProject(INFRA)
                        .build())
                .performBy(AMOSOV_F);

        final DiListResponse<DiProject> projects = createLocalClient()
                .path("/v1/projects")
                .header("Content-Type", "application/json")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + WHISTLER)
                .query("nocache", "true")
                .get()
                .readEntity(new GenericType<DiListResponse<DiProject>>() {
                });


        assertTrue(projects.stream()
                .anyMatch(project -> project.getKey().equals(newProject)));
    }

    private DiExtendedProject requestProjectFields(@NotNull final String projectKey,
                                                   @NotNull final DiField<?>... fields) {
        return dispenser()
                .projects()
                .getWithFields(fields)
                .withKey(projectKey)
                .perform();
    }


    @Test
    public void projectFieldsCanBeSpecifiedByFieldQueryParam() {

        final DiProject originProject = dispenser().projects()
                .get()
                .withKey(INFRA)
                .perform();

        final DiExtendedProject projectFields = requestProjectFields(INFRA, DiField.NAME);

        assertEquals(1, projectFields.getValues().size());
        assertEquals(originProject.getName(), projectFields.getName());

        final DiExtendedProject moreFields = requestProjectFields(INFRA, DiField.KEY, DiField.NAME, DiField.DESCRIPTION);

        assertEquals(3, moreFields.getValues().size());
        assertEquals(originProject.getName(), moreFields.getName());
        assertEquals(originProject.getDescription(), moreFields.getDescription());
        assertEquals(originProject.getKey(), moreFields.getKey());
    }

    @Test
    public void projectAncestorsCanBeFetched() {

        final DiExtendedProject projectFields = requestProjectFields(INFRA, DiProjectFields.ANCESTORS);

        final List<DiExtendedProject> ancestors = projectFields.getAncestors();
        assertEquals(2, ancestors.size());
        final DiExtendedProject firstAncestor = ancestors.get(0);
        assertEquals(YANDEX, firstAncestor.getKey());
    }


    @Test
    public void projectPersonsCanBeRequestedThroughFields() {
        final String projectKey = "tools-java";
        final DiProject project = DiProject.withKey(projectKey)
                .withName("Tools: Java services")
                .withDescription("Development of Java services in Tools")
                .withResponsibles(DiPersonGroup.builder().addPersons(LOTREK.getLogin()).build())
                .withMembers(DiPersonGroup.builder()
                        .addPersons(AMOSOV_F.getLogin())
                        .addYaGroups(DiYandexGroupType.DEPARTMENT, "yandex_infra_tech_tools_st_dev")
                        .build())
                .withParentProject(YANDEX)
                .withAbcServiceId(TEST_ABC_SERVICE_ID)
                .build();
        dispenser().projects().create(project).performBy(WHISTLER);

        updateHierarchy();

        final DiProject originProject = dispenser().projects()
                .get()
                .withKey(projectKey)
                .perform();

        final DiExtendedProject projectFields = requestProjectFields(projectKey, DiProjectFields.MEMBERS, DiProjectFields.RESPONSIBLES, DiProjectFields.ALL_MEMBERS);

        assertEquals(originProject.getResponsibles(), projectFields.getResponsibles());
        assertEquals(originProject.getMembers(), projectFields.getMembers());

        final Set<String> allMembers = StreamUtils.concat(
                originProject.getResponsibles().getPersons().stream(),
                originProject.getMembers().getPersons().stream(),
                originProject.getMembers()
                        .getYandexGroups(DiYandexGroupType.DEPARTMENT)
                        .stream()
                        .flatMap(group -> staffCache
                                .getPersonsInGroups(groupDao.tryReadYaGroupsByUrls(Collections.singleton(group)).stream()
                                        .filter(g -> !g.isDeleted()).collect(Collectors.toSet())).stream().map(Person::getLogin)),
                Stream.of(LYADZHIN.getLogin(), WHISTLER.getLogin())
        ).collect(Collectors.toSet());

        assertEquals(allMembers, projectFields.getAllMembers());
    }

    @Test
    public void projectFieldsCanContainNullValues() {
        final DiExtendedProject projectFields = requestProjectFields(YANDEX, DiProjectFields.ABC_SERVICE);
        assertTrue(projectFields.getValues().containsKey(DiProjectFields.ABC_SERVICE));
        assertNull(projectFields.getAbcServiceId());
    }

    @Test
    public void removeProjectWithPersonalSubProjectShouldWork() {
        final DiEntity entity = DiEntity.withKey("pool")
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.of(60, DiUnit.BYTE))
                .build();

        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(entity, LYADZHIN.chooses(INFRA))
                .shareEntity(DiEntityUsage.singleOf(entity), LYADZHIN.chooses(VERTICALI))
                .perform();

        updateHierarchy();

        dispenser().project(INFRA).delete().performBy(AMOSOV_F);

        updateHierarchy();

        assertThrows(Throwable.class, () -> {
            dispenser().projects()
                    .get()
                    .withKey(INFRA)
                    .perform();
        });
    }

    @Test
    public void removedProjectChildrenMustExistsInMemDao() {
        ignoreInStub();

        final Set<Project> previousProjects = Hierarchy.get().getProjectReader().getAll();

        dispenser().project(YANDEX).delete().performBy(AMOSOV_F);

        updateHierarchy();

        final Set<Project> currentProjects = Hierarchy.get().getProjectReader().getAll();
        assertEquals(currentProjects, previousProjects);
    }

    @Test
    public void projectSubProjectKeysCanBeFetchedByFields() {

        final DiExtendedProject projectFields = requestProjectFields(YANDEX, DiProjectFields.SUB_PROJECT_KEYS);

        final Collection<String> keys = requireNonNull(projectFields.getSubprojectKeys());

        assertTrue(keys.containsAll(Arrays.asList("default", "search", "infra", "verticals")));
    }

    @Test
    public void fieldsCanBeFilteredWhenRequestingMultipleProjects() {
        final DiListResponse<DiExtendedProject> projects = dispenser()
                .projects()
                .getWithFields(DiField.KEY, DiField.NAME, DiProjectFields.ALL_MEMBERS)
                .perform();

        assertTrue(projects.stream().allMatch(
                project -> project.getKey() != null && project.getName() != null && project.getAllMembers() != null
        ));
    }

    @Test
    public void allFieldsCanBeRequestedForProject() {
        final ImmutableSet<DiField<?>> allFieldsSet = ImmutableSet.copyOf(DiProjectFields.ALL_FIELDS);

        dispenser().projects()
                .create(DiProject.withKey("with-abc-id")
                        .withName("Project with abc id")
                        .withDescription("Project with abc id")
                        .withAbcServiceId(TEST_ABC_SERVICE_ID)
                        .withParentProject(YANDEX)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        projectDao.update(Project.copyOf(projectDao.read("with-abc-id"))
                .mailList("ml@yandex-team.ru")
                .build());

        updateHierarchy();

        final ImmutableMap<DiField<?>, Function<DiExtendedProject, ?>> fieldToGetter = ImmutableMap.<DiField<?>, Function<DiExtendedProject, ?>>builder()
                .put(DiField.KEY, DiProjectFieldsHolder::getKey)
                .put(DiField.NAME, DiProjectFieldsHolder::getName)
                .put(DiField.DESCRIPTION, DiProjectFieldsHolder::getDescription)
                .put(DiProjectFields.ANCESTORS, DiExtendedProject::getAncestors)
                .put(DiProjectFields.ALL_MEMBERS, DiExtendedProject::getAllMembers)
                .put(DiProjectFields.ABC_SERVICE, DiProjectFieldsHolder::getAbcServiceId)
                .put(DiProjectFields.RESPONSIBLES, DiProjectFieldsHolder::getResponsibles)
                .put(DiProjectFields.MEMBERS, DiProjectFieldsHolder::getMembers)
                .put(DiProjectFields.PARENT_PROJECT_KEY, DiProjectFieldsHolder::getParentProjectKey)
                .put(DiProjectFields.SUB_PROJECT_KEYS, DiProjectFieldsHolder::getSubprojectKeys)
                .put(DiProjectFields.PARENT_PROJECT, DiExtendedProject::getParentProject)
                .put(DiProjectFields.PERMISSIONS, DiExtendedProject::getPermissions)
                .put(DiProjectFields.MAIL_LIST, DiExtendedProject::getMailList)
                .build();


        final Sets.SetView<DiField<?>> fieldsWithoutGetter = Sets.difference(allFieldsSet, fieldToGetter.keySet());
        assertEquals(Collections.emptySet(), fieldsWithoutGetter, "All fields must be tested");

        final DiField<?>[] fieldsArray = allFieldsSet.toArray(new DiField<?>[allFieldsSet.size()]);

        final DiListResponse<DiExtendedProject> projects = dispenser()
                .projects()
                .getWithFields(fieldsArray)
                .perform();

        final Set<DiField<?>> fieldWithValues = new HashSet<>();


        for (final DiExtendedProject project : projects) {
            final Map<DiField<?>, ?> values = project.getValues();

            for (final DiField<?> field : fieldToGetter.keySet()) {
                assertEquals(values.get(field), fieldToGetter.get(field).apply(project), "field getter invalid " + field);

                if (values.get(field) != null) {
                    fieldWithValues.add(field);
                }
            }
        }


        final Sets.SetView<DiField<?>> fieldWithoutValues = Sets.difference(allFieldsSet, fieldWithValues);
        assertEquals(Collections.emptySet(), fieldWithoutValues, "All fields must be tested with value");
    }

    @Test
    public void projectAbcMembersFlagCanBeUsed() {
        assertFalse(projectDao.read(YANDEX).isSyncedWithAbc());

        final String syncedAbcProjectKey = "abc-synced";

        final Project project = projectDao.create(Project.withKey(syncedAbcProjectKey)
                .name(syncedAbcProjectKey)
                .description(syncedAbcProjectKey)
                .syncedWithAbc(true)
                .build());

        assertTrue(projectDao.read(syncedAbcProjectKey).isSyncedWithAbc());

        projectDao.update(Project.copyOf(project)
                .syncedWithAbc(false)
                .build());

        assertFalse(projectDao.read(syncedAbcProjectKey).isSyncedWithAbc());
    }

    @Test
    public void projectWithAbcSyncShouldNotAcceptGroupsOrUsers() {
        final String syncedAbcProjectKey = "abc-synced";
        projectDao.create(Project.withKey(syncedAbcProjectKey)
                .name(syncedAbcProjectKey)
                .description(syncedAbcProjectKey)
                .syncedWithAbc(true)
                .abcServiceId(TEST_ABC_SERVICE_ID)
                .parent(projectDao.read(YANDEX))
                .build());


        updateHierarchy();

        final Collection<DiPersonGroup> roles = ImmutableList.of(
                DiPersonGroup.builder().addPersons(AMOSOV_F.getLogin()).build(),
                DiPersonGroup.builder().addPersons(AMOSOV_F.getLogin()).addYaGroups(DiYandexGroupType.DEPARTMENT, "yandex_infra_tech_tools_st_dev").build(),
                DiPersonGroup.builder().addYaGroups(DiYandexGroupType.DEPARTMENT, "yandex_infra_tech_tools_st_dev").build()
        );

        final DiProject diProject = dispenser()
                .projects()
                .get()
                .withKey(syncedAbcProjectKey)
                .perform();

        final DiProject diYandex = dispenser()
                .projects()
                .get()
                .withKey(YANDEX)
                .perform();

        final DiProject.Builder projectBuilder = DiProject.copyOf(diProject);
        final DiProject.Builder yandexBuilder = DiProject.copyOf(diYandex).withDescription(YANDEX);

        for (final DiPersonGroup group : roles) {
            dispenser().projects().update(yandexBuilder.withMembers(group).build()).performBy(AMOSOV_F);
            assertThrows(Throwable.class, () -> {
                dispenser().projects().update(projectBuilder.withMembers(group).build()).performBy(AMOSOV_F);
            });
        }
    }

    @Test
    public void responsiblesAndMembersFromAbcShouldBeUsedForSyncedWithAbcProjects() {
        mockAbcApi.reset();

        final AbcServiceReference ref = new AbcServiceReference(TEST_ABC_SERVICE_ID);
        mockAbcApi.addRole(new AbcRole(123, ref));
        mockAbcApi.addRole(new AbcRole(456, ref));
        mockAbcApi.addResponsible(new AbcServiceResponsible(new AbcPerson("lotrek"), ref));
        mockAbcApi.addResponsible(new AbcServiceResponsible(new AbcPerson("dm-tim"), ref));
        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("binarycat"), ref, new AbcRole(123, ref), 1));
        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("starlight"), ref, new AbcRole(123, ref), 2));
        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("veged"), ref, new AbcRole(456, ref), 3));
        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("illyusion"), ref, new AbcRole(456, ref), 4));

        final String syncedAbcProjectKey = "abc-synced";
        projectDao.create(Project.withKey(syncedAbcProjectKey)
                .name(syncedAbcProjectKey)
                .description(syncedAbcProjectKey)
                .syncedWithAbc(true)
                .abcServiceId(TEST_ABC_SERVICE_ID)
                .parent(projectDao.read(YANDEX))
                .build());

        updateHierarchy();

        final DiProject.Builder projectBuilder = DiProject.copyOf(dispenser()
                .projects()
                .get()
                .withKey(syncedAbcProjectKey)
                .perform());

        dispenser().projects().update(projectBuilder.withMembers(DiPersonGroup.builder()
                .build()).build()).performBy(AMOSOV_F);

        updateHierarchy();

        DiProject updatedProject = dispenser().projects()
                .get()
                .withKey(syncedAbcProjectKey)
                .perform();

        assertEquals(ImmutableSet.of("binarycat", "starlight", "illyusion", "veged"), updatedProject.getMembers().getPersons());
        assertEquals(ImmutableSet.of("lotrek", "dm-tim"), updatedProject.getResponsibles().getPersons());
    }

    @Test
    public void projectKeyValidationMustFailOnInvalidKeys() {
        assertThrows(IllegalArgumentException.class, () -> {
            projectDao.create(Project.withKey("1231 23._")
                    .name("1")
                    .description("1")
                    .abcServiceId(TEST_ABC_SERVICE_ID)
                    .build());
        });
    }

    @Test
    public void mailListShouldBeValidated() {
        final HashMap<String, Object> body = new HashMap<>();
        body.put("key", "Test002");
        body.put("name", "Test002");
        body.put("description", "Test002");
        body.put("abcServiceId", TEST_ABC_SERVICE_ID);
        body.put("mailList", "test@non-yateam.ru");

        assertThrowsWithMessage(() -> createAuthorizedLocalClient(AMOSOV_F)
                        .path("/v1/projects/yandex/create-subproject")
                        .post(body, DiProject.class)
                , "'test@non-yateam.ru' does not match regexp ");

        body.put("mailList", "custommail");

        assertThrowsWithMessage(() -> createAuthorizedLocalClient(AMOSOV_F)
                        .path("/v1/projects/yandex/create-subproject")
                        .post(body, DiProject.class)
                , "'custommail' does not match regexp ");

        body.put("mailList", "valid@yandex-team.ru");

        createAuthorizedLocalClient(AMOSOV_F)
                .path("/v1/projects/yandex/create-subproject")
                .post(body, DiProject.class);

        updateHierarchy();

        final DiExtendedProject createdProject = dispenser().projects()
                .getWithFields(DiProjectFields.MAIL_LIST)
                .withKey("Test002")
                .perform();

        assertEquals("valid@yandex-team.ru", createdProject.getMailList());

        body.put("mailList", "nonvalid@yandex.ru");

        assertThrowsWithMessage(() -> createAuthorizedLocalClient(AMOSOV_F)
                        .path("/v1/projects/" + body.get("key"))
                        .post(body, DiProject.class)
                , "'nonvalid@yandex.ru' does not match regexp ");
    }
}
