package ru.yandex.qe.dispenser.ws.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.qe.bus.Robot;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.abc.AbcService;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceGradient;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceReference;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceState;
import ru.yandex.qe.dispenser.domain.abc.Page;
import ru.yandex.qe.dispenser.domain.abc.TranslatedString;
import ru.yandex.qe.dispenser.standalone.MockAbcApi;
import ru.yandex.qe.dispenser.ws.TvmDestination;
import ru.yandex.qe.dispenser.ws.abc.AbcApiHelper;
import ru.yandex.qe.dispenser.ws.abc.ProjectTreeSync;
import ru.yandex.qe.dispenser.ws.abc.UpdateProjectMembers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProjectAbcSyncTest extends BusinessLogicTestBase {

    @Autowired
    private MockAbcApi mockAbcApi;
    @Autowired
    @Qualifier("abc-tvm")
    private TvmDestination abcTvmDestination;

    @Autowired
    private ProjectTreeSync projectTreeSync;

    @Autowired
    private AbcApiHelper abcApiHelper;

    @AfterAll
    public void resetAbcTree() {
        mockAbcApi.reset();
    }

    @BeforeEach
    public void clearAbcTree() {
        mockAbcApi.clear();
    }

    @Test
    public void syncMustRemoveAllNonYandexProjectOnEmptyAbcRoot() {
        projectTreeSync.perform(new UpdateProjectMembers.RoleChangesHolder());
        final Set<Project> projects = getNotTrashProjects();
        assertEquals(1, projects.size());
        assertEquals(YANDEX, projects.iterator().next().getPublicKey());
    }

    @Test
    public void syncedProjectMustHaveValidFields() {
        mockAbcApi.addService(new AbcService(100, Collections.emptyList(), null, "test-slug", new TranslatedString("test-name", ""), new TranslatedString("test-description", ""), AbcServiceState.NEED_INFO));

        projectTreeSync.perform(new UpdateProjectMembers.RoleChangesHolder());

        final Set<Project> projects = getNotTrashProjects();

        assertEquals(2, projects.size());

        final Project syncedProject = projectDao.read("test-slug");
        assertEquals(Integer.valueOf(100), syncedProject.getAbcServiceId());
        assertEquals("test-name", syncedProject.getName());
        assertEquals("test-description", syncedProject.getDescription());
    }

    @NotNull
    private AbcService addService(final Integer abcId, final Integer parentAbcId) {
        final String slug = "abc-" + abcId;
        return mockAbcApi.addService(new AbcService(abcId, Collections.emptyList(), parentAbcId == null ? null :
                new AbcServiceReference(parentAbcId), slug, new TranslatedString(slug, slug), MockAbcApi.EMPTY_TRANSLATE_STRING, AbcServiceState.DEVELOP));
    }

    @Test
    public void syncMustMoveExistingProjects() {

        addService(TEST_ABC_SERVICE_ID, null);
        addService(2, TEST_ABC_SERVICE_ID);

        final Project top = projectDao.create(Project.withKey("abc-2")
                .name("Top")
                .abcServiceId(2)
                .parent(projectDao.read(YANDEX))
                .build()
        );

        final Project bot = projectDao.create(Project.withKey("abc-" + TEST_ABC_SERVICE_ID)
                .name("Bot")
                .abcServiceId(TEST_ABC_SERVICE_ID)
                .parent(top)
                .build()
        );

        final long rootMax = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .forResource(YT_GPU)
                .ofProject(YANDEX)
                .perform().getFirst().getMax(DiUnit.PERMILLE);

        updateHierarchy();

        dispenser()
                .service(NIRVANA)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(YT_GPU)
                        .forProject(YANDEX)
                        .withMax(DiAmount.of(rootMax + 44, DiUnit.PERMILLE))
                        .build())
                .changeQuota(DiQuotaState.forResource(YT_GPU)
                        .forProject(top.getPublicKey())
                        .withMax(DiAmount.of(44, DiUnit.PERMILLE))
                        .build())
                .changeQuota(DiQuotaState.forResource(YT_GPU)
                        .forProject(bot.getPublicKey())
                        .withMax(DiAmount.of(33, DiUnit.PERMILLE))
                        .build())
                .performBy(AMOSOV_F);

        projectTreeSync.perform(new UpdateProjectMembers.RoleChangesHolder());

        final Set<Project> projects = getNotTrashProjects();
        assertEquals(3, projects.size());

        final Map<Long, Project> projectsById = projects.stream()
                .collect(Collectors.toMap(Project::getId, Function.identity()));

        final Project modifiedTop = projectsById.get(top.getId());

        assertNotNull(modifiedTop);
        assertNotNull(projectsById.get(bot.getId()));

        assertEquals(modifiedTop.getParent().getId(), bot.getId());

        updateHierarchy();

        final Map<String, Long> quotaMaxByProjectKey = dispenser()
                .quotas()
                .get()
                .inService(NIRVANA)
                .forResource(YT_GPU)
                .perform().stream()
                .collect(Collectors.toMap(q -> q.getProject().getKey(), q -> q.getMax(DiUnit.PERMILLE)));


        assertEquals(Long.valueOf(44), quotaMaxByProjectKey.get(bot.getKey().getPublicKey()));
        assertEquals(Long.valueOf(11), quotaMaxByProjectKey.get(top.getKey().getPublicKey()));
    }


    @Test
    public void syncMustMatchBySlugIfNoAbcServiceIdExists() {
        mockAbcApi.addService(new AbcService(100, Collections.emptyList(), null, INFRA, new TranslatedString("test-name", ""), new TranslatedString("test-description", ""), AbcServiceState.DEVELOP));

        projectTreeSync.perform(new UpdateProjectMembers.RoleChangesHolder());

        final Set<Project> projects = getNotTrashProjects();
        assertEquals(2, projects.size());
    }

    @Test
    public void syncMustSlugIdMissmatching() {
        mockAbcApi.addService(new AbcService(100, Collections.emptyList(), null, INFRA, new TranslatedString("test-name", ""), new TranslatedString("test-description", ""), AbcServiceState.DEVELOP));
        mockAbcApi.addService(new AbcService(200, Collections.emptyList(), null, DEFAULT, new TranslatedString("test-name-2", ""), new TranslatedString("test-description", ""), AbcServiceState.DEVELOP));

        projectDao.update(
                Project.copyOf(projectDao.read(INFRA))
                        .abcServiceId(200)
                        .build()
        );

        projectTreeSync.perform(new UpdateProjectMembers.RoleChangesHolder());

        final Set<Project> projects = getNotTrashProjects();
        assertEquals(3, projects.size());
    }

    @Test
    public void onlyServicesWithValidStatesMustBeSynced() {
        mockAbcApi.addService(new AbcService(100, Collections.emptyList(), null, INFRA, new TranslatedString("test-name", ""), new TranslatedString("test-description", ""), AbcServiceState.DEVELOP));
        mockAbcApi.addService(new AbcService(200, Collections.emptyList(), null, DEFAULT, new TranslatedString("test-name-2", ""), new TranslatedString("test-description", ""), AbcServiceState.NEED_INFO));
        mockAbcApi.addService(new AbcService(300, Collections.emptyList(), null, VERTICALI, new TranslatedString("test-name-3", ""), new TranslatedString("test-description", ""), AbcServiceState.SUPPORTED));
        mockAbcApi.addService(new AbcService(400, Collections.emptyList(), null, SEARCH, new TranslatedString("test-name-4", ""), new TranslatedString("test-description", ""), AbcServiceState.DELETED));
        mockAbcApi.addService(new AbcService(500, Collections.emptyList(), null, INFRA_SPECIAL, new TranslatedString("test-name-5", ""), new TranslatedString("test-description", ""), AbcServiceState.CLOSED));

        projectTreeSync.perform(new UpdateProjectMembers.RoleChangesHolder());

        final Set<Integer> abcIds = getNotTrashProjects().stream()
                .filter(p -> !p.getPublicKey().equals(YANDEX))
                .map(Project::getAbcServiceId)
                .collect(Collectors.toSet());

        assertEquals(Sets.newHashSet(100, 200, 300), abcIds);
    }

    private Set<Project> getNotTrashProjects() {
        return projectDao.getAll().stream()
                .filter(p -> p.getPathToRoot().stream().noneMatch(ancestor -> ancestor.getPublicKey().equals(Project.TRASH_PROJECT_KEY)))
                .collect(Collectors.toSet());
    }

    @Test
    public void projectCanBeMovedInOutTrashProject() {

        final int projectsBefore = projectDao.getAll().size();

        mockAbcApi.addService(new AbcService(100, Collections.emptyList(), null,
                "no-" + INFRA, new TranslatedString("test-name", ""), new TranslatedString("test-description", ""), AbcServiceState.DEVELOP));

        projectTreeSync.perform(new UpdateProjectMembers.RoleChangesHolder());

        assertEquals(2, getNotTrashProjects().size()); //new synced + yandex

        final int projectsSizeAfter = projectDao.getAll().size();
        assertEquals(projectsSizeAfter, projectsBefore + 2); //new synced + trash for other

        mockAbcApi.addService(new AbcService(200, Collections.emptyList(), null, DEFAULT, new TranslatedString("test-name-2", ""), new TranslatedString("test-description", ""), AbcServiceState.NEED_INFO));

        updateHierarchy();
        projectTreeSync.perform(new UpdateProjectMembers.RoleChangesHolder());

        assertEquals(3, getNotTrashProjects().size()); //new synced, old restored + yandex

        assertEquals(projectsSizeAfter, projectsBefore + 2);
    }

    @Test
    public void isCursorPaginationSendServiceIdToAbcApiForFiltering() {
        final List<Integer> gotByApiFromParamServiceIds = new ArrayList<>();
        final MockAbcApi localMockAbcApi = new MockAbcApi() {
            @Override
            public Page<AbcService> getServices(@Nullable final String ids,
                                                @Nullable final Integer smallestAcceptableServiceId,
                                                @Nullable final String cursor,
                                                @Nullable final String fields,
                                                @Nullable final Integer page,
                                                @Nullable final Integer pageSize,
                                                @Nullable final Integer parentIdWithDescendants,
                                                @Nullable final String statesString) {
                gotByApiFromParamServiceIds.add(smallestAcceptableServiceId);
                return super.getServices(ids, smallestAcceptableServiceId, cursor, fields, page, pageSize, parentIdWithDescendants, statesString);
            }
        };

        final AbcApiHelper localAbcApiHelper = new AbcApiHelper(localMockAbcApi, new Robot("abc-robot"), abcTvmDestination, false);
        localMockAbcApi.clear();
        final int servicesNumber = 17;
        final List<Integer> serviceIdsList = new ArrayList<>();
        for (int serviceId = 1; serviceId <= servicesNumber; serviceId++) {
            localMockAbcApi.addService(new AbcService(serviceId, Collections.emptyList(), null, null, null, null, null));
            serviceIdsList.add(serviceId);
        }

        final List<Integer> usingCursorPaginationAbcServices = localAbcApiHelper.createServiceRequestBuilder()
                .fields("id")
                .pageSize(5)
                .stream()
                .map(AbcServiceReference::getId)
                .collect(Collectors.toList());

        assertEquals(Arrays.asList(null, 5, 10, 15), gotByApiFromParamServiceIds);
        assertEquals(usingCursorPaginationAbcServices, serviceIdsList);
    }

    @Test
    public void cursorPaginationResponseIsCorrect() {
        final int servicesNumber = 3000;
        for (int ServiceId = servicesNumber; ServiceId >= 1; ServiceId--) {
            mockAbcApi.addService(new AbcService(ServiceId, Collections.emptyList(), null, null, null, null, null));
        }

        final Set<AbcService> abcServicesUsingCursorPagination = abcApiHelper.createServiceRequestBuilder()
                .fields("id")
                .stream()
                .collect(Collectors.toSet());

        final Set<AbcService> abcServicesUsingPagePagination = abcApiHelper.createServiceRequestBuilder()
                .fields("id")
                .stream()
                .collect(Collectors.toSet());

        assertEquals(abcServicesUsingCursorPagination, abcServicesUsingPagePagination);
    }

    @Test
    public void cursorPaginationReturnSortedByIdServices() {
        final List<Integer> serviceIds = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            serviceIds.add(i);
        }

        // перемешиваем Id-шники сервисов в коллекции
        final Random random = new Random();
        for (int i = 1; i <= 100; i++) {
            final int firstServiceId = random.nextInt(serviceIds.size());
            final int secondServiceId = random.nextInt(serviceIds.size());
            final Integer buff = serviceIds.get(firstServiceId);
            serviceIds.set(firstServiceId, serviceIds.get(secondServiceId));
            serviceIds.set(secondServiceId, buff);
        }

        for (final int serviceId : serviceIds) {
            mockAbcApi.addService(new AbcService(serviceId, Collections.emptyList(), null, null, null, null, AbcServiceState.DEVELOP));
        }

        final List<AbcService> sortedAbcServices = abcApiHelper.createServiceRequestBuilder()
                .fields("id")
                .state(AbcServiceState.DEVELOP)
                .stream()
                .collect(Collectors.toList());

        for (int i = 1; i < sortedAbcServices.size(); i++) {
            final AbcService firstService = sortedAbcServices.get(i - 1);
            final AbcService secondService = sortedAbcServices.get(i);

            assertTrue(firstService.getId().longValue() < secondService.getId().longValue());
        }
    }

    @Test
    public void gradientFieldsShouldBeSynced() {
        addService(205, null);
        addService(210, 205);
        addService(215, 210);
        addService(220, 215);
        mockAbcApi.setGradients(ImmutableList.of(
                new AbcServiceGradient(205L, "umb", null, null),
                new AbcServiceGradient(210L, "vs", null, null),
                new AbcServiceGradient(215L, "umb", 210L, null),
                new AbcServiceGradient(220L, null, 210L, 215L)
        ));

        projectTreeSync.perform(new UpdateProjectMembers.RoleChangesHolder());

        assertNull(projectDao.read("abc-205").getValueStreamAbcServiceId());
        assertEquals(210L, projectDao.read("abc-210").getValueStreamAbcServiceId());
        assertEquals(210L, projectDao.read("abc-215").getValueStreamAbcServiceId());
        assertEquals(210L, projectDao.read("abc-220").getValueStreamAbcServiceId());

    }
}
