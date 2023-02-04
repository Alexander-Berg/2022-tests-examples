package ru.yandex.qe.dispenser.ws.logic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.field.DiProjectFields;
import ru.yandex.qe.dispenser.api.v1.project.DiExtendedProject;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.ProjectRole;
import ru.yandex.qe.dispenser.domain.abc.AbcPerson;
import ru.yandex.qe.dispenser.domain.abc.AbcRole;
import ru.yandex.qe.dispenser.domain.abc.AbcService;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceMember;
import ru.yandex.qe.dispenser.domain.abc.AbcServicePersonHolder;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceResponsible;
import ru.yandex.qe.dispenser.domain.dao.project.ProjectDao;
import ru.yandex.qe.dispenser.domain.dao.project.role.ProjectRoleDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Role;
import ru.yandex.qe.dispenser.standalone.MockAbcApi;
import ru.yandex.qe.dispenser.ws.abc.UpdateProjectMembers;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProjectAbcServiceMemberSyncTest extends BusinessLogicTestBase {
    @Autowired
    private MockAbcApi mockAbcApi;

    @Autowired
    private UpdateProjectMembers updateProjectMembers;

    @Autowired
    private ProjectRoleDao projectRoleDao;

    @Autowired
    private ProjectDao projectDao;

    @BeforeAll
    public void initAbcTree() {
        final AbcService abcService1 = mockAbcApi.addService(1, Collections.emptyList());
        final AbcService abcService2 = mockAbcApi.addService(2, Collections.emptyList());
        final AbcService abcService3 = mockAbcApi.addService(3, Collections.emptyList());

        final AbcRole role10 = mockAbcApi.addRole(new AbcRole(10, abcService1));
        final AbcRole role20 = mockAbcApi.addRole(new AbcRole(20, abcService2));
        final AbcRole role21 = mockAbcApi.addRole(new AbcRole(21, abcService2));
        final AbcRole role30 = mockAbcApi.addRole(new AbcRole(30, abcService3));
        final AbcRole role31 = mockAbcApi.addRole(new AbcRole(31, abcService3));
        final AbcRole role32 = mockAbcApi.addRole(new AbcRole(32, abcService3));

        mockAbcApi.addResponsible(new AbcServiceResponsible(new AbcPerson("amosov-f"), abcService1));
        mockAbcApi.addResponsible(new AbcServiceResponsible(new AbcPerson("whistler"), abcService2));
        mockAbcApi.addResponsible(new AbcServiceResponsible(new AbcPerson("sancho"), abcService2));
        mockAbcApi.addResponsible(new AbcServiceResponsible(new AbcPerson("terry"), abcService3));
        mockAbcApi.addResponsible(new AbcServiceResponsible(new AbcPerson("lyadzhin"), abcService3));
        mockAbcApi.addResponsible(new AbcServiceResponsible(new AbcPerson("qdeee"), abcService3));

        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("sterligovak"), abcService1, role10, 1));
        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("slonnn"), abcService1, role10, 2));
        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("binarycat"), abcService2, role20, 3));
        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("lotrek"), abcService2, role20, 4));
        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("illyusion"), abcService2, role21, 5));
        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("amosov-f"), abcService2, role21, 6));
        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("whistler"), abcService3, role30, 7));
        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("sancho"), abcService3, role31, 8));
        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("terry"), abcService3, role31, 9));
        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("lyadzhin"), abcService3, role32, 10));
        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("qdeee"), abcService3, role32, 11));
        mockAbcApi.addMember(new AbcServiceMember(new AbcPerson("bendyna"), abcService3, role32, 12));
    }

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
    }

    public static Object[][] projects() {
        return new Object[][]{
                {
                        1,
                        ImmutableSet.of("sterligovak", "slonnn")
                },
                {
                        2,
                        ImmutableSet.of("binarycat", "lotrek", "illyusion", "amosov-f")
                },
                {
                        3,
                        ImmutableSet.of("whistler", "sancho", "terry", "lyadzhin", "qdeee", "bendyna")
                }
        };
    }

    @MethodSource("projects")
    @ParameterizedTest
    public void abcServiceMembersShouldBeSynced(final Integer abcServiceId,
                                                final Set<String> expectedMembers) {
        projectRoleDao.clear();
        projectRoleDao.create(new ProjectRole(Role.MEMBER.getKey(), ProjectRole.AbcRoleSyncType.MEMBER, null));
        projectRoleDao.create(new ProjectRole(Role.RESPONSIBLE.getKey(), ProjectRole.AbcRoleSyncType.NONE, null));

        final Project project = projectDao.create(Project.withKey("abc-sync-1")
                .parent(projectDao.read("yandex"))
                .syncedWithAbc(true)
                .name("abc-sync-1")
                .description("abc-sync-1")
                .abcServiceId(abcServiceId)
                .build());

        updateHierarchy();

        updateProjectMembers.update();
        updateHierarchy();

        final DiExtendedProject allMemberHolder = dispenser().projects()
                .getWithFields(DiProjectFields.ALL_MEMBERS)
                .withKey(project.getPublicKey())
                .perform();

        final Set<String> allMembers = allMemberHolder.getAllMembers();

        final Set<String> expectedAllMembers = Stream.of(project.getPathToRoot().stream()
                        .skip(1)
                        .flatMap(ancestor -> projectDao.getAllMembers(ancestor).stream()),
                expectedMembers.stream()
        )
                .flatMap(Function.identity())
                .collect(Collectors.toSet());

        assertEquals(allMembers, expectedAllMembers);

        assertEquals(projectDao.getLinkedMembers(project).stream().map(Person::getLogin).collect(Collectors.toSet()), expectedMembers);
    }

    public static Object[][] customRoles() {
        return new Object[][]{
                {
                        1,
                        ImmutableMap.of(
                                Pair.of(ProjectRole.AbcRoleSyncType.MEMBER, null),
                                ImmutableSet.of("sterligovak", "slonnn"),
                                Pair.of(ProjectRole.AbcRoleSyncType.RESPONSIBLE, null),
                                ImmutableSet.of("amosov-f")
                        )
                },
                {
                        2,
                        ImmutableMap.of(
                                Pair.of(ProjectRole.AbcRoleSyncType.MEMBER, null),
                                ImmutableSet.of("binarycat", "lotrek", "illyusion", "amosov-f"),
                                Pair.of(ProjectRole.AbcRoleSyncType.RESPONSIBLE, null),
                                ImmutableSet.of("whistler", "sancho"),
                                Pair.of(ProjectRole.AbcRoleSyncType.ROLE, 21L),
                                ImmutableSet.of("illyusion", "amosov-f")
                        )
                },
                {
                        3,
                        ImmutableMap.of(
                                Pair.of(ProjectRole.AbcRoleSyncType.MEMBER, null),
                                ImmutableSet.of("whistler", "sancho", "terry", "lyadzhin", "qdeee", "bendyna"),
                                Pair.of(ProjectRole.AbcRoleSyncType.RESPONSIBLE, null),
                                ImmutableSet.of("terry", "lyadzhin", "qdeee"),
                                Pair.of(ProjectRole.AbcRoleSyncType.ROLE, 30L),
                                ImmutableSet.of("whistler"),
                                Pair.of(ProjectRole.AbcRoleSyncType.ROLE, 32L),
                                ImmutableSet.of("lyadzhin", "qdeee", "bendyna")
                        )
                }
        };
    }

    @MethodSource("customRoles")
    @ParameterizedTest
    public void severalRolesCanBeSynced(final Integer abcServiceId, final Map<Pair<ProjectRole.AbcRoleSyncType, Long>, Set<String>> expectedMembers) {
        projectRoleDao.clear();
        projectRoleDao.create(new ProjectRole(Role.MEMBER.getKey(), ProjectRole.AbcRoleSyncType.MEMBER, null));
        projectRoleDao.create(new ProjectRole(Role.RESPONSIBLE.getKey(), ProjectRole.AbcRoleSyncType.RESPONSIBLE, null));
        projectRoleDao.create(new ProjectRole("role21", ProjectRole.AbcRoleSyncType.ROLE, 21L));
        projectRoleDao.create(new ProjectRole("role30", ProjectRole.AbcRoleSyncType.ROLE, 30L));
        projectRoleDao.create(new ProjectRole("role32", ProjectRole.AbcRoleSyncType.ROLE, 32L));

        final Project project = projectDao.create(Project.withKey("abc-sync-1")
                .parent(projectDao.read("yandex"))
                .syncedWithAbc(true)
                .name("abc-sync-1")
                .description("abc-sync-1")
                .abcServiceId(abcServiceId)
                .build());

        updateHierarchy();

        updateProjectMembers.update();
        updateHierarchy();

        final Map<Pair<ProjectRole.AbcRoleSyncType, Long>, ProjectRole> projectRolesByAbcSyncState = projectRoleDao.getAll().stream()
                .collect(Collectors.toMap(pr -> Pair.of(pr.getAbcRoleSyncType(), pr.getAbcRoleId()), Function.identity()));

        for (final Pair<ProjectRole.AbcRoleSyncType, Long> abcSyncState : expectedMembers.keySet()) {
            final Set<String> persons = projectDao.getLinkedPersons(project, projectRolesByAbcSyncState.get(abcSyncState).getKey())
                    .stream()
                    .map(Person::getLogin)
                    .collect(Collectors.toSet());

            assertEquals(persons, expectedMembers.get(abcSyncState));
        }

    }

    @Test
    public void rolesMustBeRemoveIfAbcMembersRemoved() {
        projectRoleDao.clear();
        projectRoleDao.create(new ProjectRole(Role.MEMBER.getKey(), ProjectRole.AbcRoleSyncType.MEMBER, null));
        projectRoleDao.create(new ProjectRole(Role.RESPONSIBLE.getKey(), ProjectRole.AbcRoleSyncType.RESPONSIBLE, null));
        projectRoleDao.create(new ProjectRole("role40", ProjectRole.AbcRoleSyncType.ROLE, 40L));
        projectRoleDao.create(new ProjectRole("role41", ProjectRole.AbcRoleSyncType.ROLE, 41L));

        final AbcService abcService4 = mockAbcApi.addService(4, Collections.emptyList());
        final AbcRole role40 = mockAbcApi.addRole(new AbcRole(40, abcService4));
        final AbcRole role41 = mockAbcApi.addRole(new AbcRole(41, abcService4));

        final AbcServiceResponsible lyadzhin = new AbcServiceResponsible(new AbcPerson("lyadzhin"), abcService4);
        mockAbcApi.addResponsible(lyadzhin);
        final AbcServiceResponsible qdeee = new AbcServiceResponsible(new AbcPerson("qdeee"), abcService4);
        mockAbcApi.addResponsible(qdeee);

        final AbcServiceMember sterligovak = new AbcServiceMember(new AbcPerson("sterligovak"), abcService4, role40, 1);
        mockAbcApi.addMember(sterligovak);
        final AbcServiceMember slonnn = new AbcServiceMember(new AbcPerson("slonnn"), abcService4, role40, 2);
        mockAbcApi.addMember(slonnn);
        final AbcServiceMember binarycat = new AbcServiceMember(new AbcPerson("binarycat"), abcService4, role41, 3);
        mockAbcApi.addMember(binarycat);

        final Project project = projectDao.create(Project.withKey("abc-sync-1")
                .parent(projectDao.read("yandex"))
                .syncedWithAbc(true)
                .name("abc-sync-1")
                .description("abc-sync-1")
                .abcServiceId(4)
                .build());

        updateHierarchy();

        updateProjectMembers.update();
        updateHierarchy();


        final ImmutableMap<Pair<ProjectRole.AbcRoleSyncType, Long>, Set<String>> expected = ImmutableMap.of(
                Pair.of(ProjectRole.AbcRoleSyncType.MEMBER, null),
                Sets.newHashSet(ImmutableSet.of("sterligovak", "slonnn", "binarycat")),
                Pair.of(ProjectRole.AbcRoleSyncType.RESPONSIBLE, null),
                Sets.newHashSet(ImmutableSet.of("lyadzhin", "qdeee")),
                Pair.of(ProjectRole.AbcRoleSyncType.ROLE, 40L),
                Sets.newHashSet(ImmutableSet.of("sterligovak", "slonnn")),
                Pair.of(ProjectRole.AbcRoleSyncType.ROLE, 41L),
                Sets.newHashSet(ImmutableSet.of("binarycat"))
        );

        final Map<Pair<ProjectRole.AbcRoleSyncType, Long>, ProjectRole> projectRolesByAbcSyncState = projectRoleDao.getAll().stream()
                .collect(Collectors.toMap(pr -> Pair.of(pr.getAbcRoleSyncType(), pr.getAbcRoleId()), Function.identity()));

        for (final Pair<ProjectRole.AbcRoleSyncType, Long> abcSyncState : expected.keySet()) {
            final Set<String> persons = projectDao.getLinkedPersons(project, projectRolesByAbcSyncState.get(abcSyncState).getKey())
                    .stream()
                    .map(Person::getLogin)
                    .collect(Collectors.toSet());

            assertEquals(persons, expected.get(abcSyncState));
        }

        final List<Pair<AbcServicePersonHolder, ImmutableSet<Pair<ProjectRole.AbcRoleSyncType, Long>>>> expectedAfterRemove = Arrays.asList(
                Pair.of(sterligovak, ImmutableSet.of(Pair.of(ProjectRole.AbcRoleSyncType.MEMBER, null), Pair.of(ProjectRole.AbcRoleSyncType.ROLE, 40L))),
                Pair.of(slonnn, ImmutableSet.of(Pair.of(ProjectRole.AbcRoleSyncType.MEMBER, null), Pair.of(ProjectRole.AbcRoleSyncType.ROLE, 40L))),
                Pair.of(binarycat, ImmutableSet.of(Pair.of(ProjectRole.AbcRoleSyncType.MEMBER, null), Pair.of(ProjectRole.AbcRoleSyncType.ROLE, 41L))),
                Pair.of(lyadzhin, ImmutableSet.of(Pair.of(ProjectRole.AbcRoleSyncType.RESPONSIBLE, null))),
                Pair.of(qdeee, ImmutableSet.of(Pair.of(ProjectRole.AbcRoleSyncType.RESPONSIBLE, null)))
        );

        for (final Pair<AbcServicePersonHolder, ImmutableSet<Pair<ProjectRole.AbcRoleSyncType, Long>>> expectedOnPersonRemoved : expectedAfterRemove) {

            final AbcServicePersonHolder person = expectedOnPersonRemoved.getLeft();
            final ImmutableSet<Pair<ProjectRole.AbcRoleSyncType, Long>> expectedRoles = expectedOnPersonRemoved.getRight();

            if (person instanceof AbcServiceMember) {
                abcApi.removeMember((AbcServiceMember) person);
            }
            if (person instanceof AbcServiceResponsible) {
                abcApi.removeResponsible((AbcServiceResponsible) person);
            }

            for (final Pair<ProjectRole.AbcRoleSyncType, Long> expectedRole : expectedRoles) {
                expected.get(expectedRole).remove(person.getPerson().getLogin());
            }

            updateProjectMembers.update();
            updateHierarchy();

            for (final Pair<ProjectRole.AbcRoleSyncType, Long> abcSyncState : expected.keySet()) {
                final Set<String> persons = projectDao.getLinkedPersons(project, projectRolesByAbcSyncState.get(abcSyncState).getKey())
                        .stream()
                        .map(Person::getLogin)
                        .collect(Collectors.toSet());

                assertEquals(persons, expected.get(abcSyncState));
            }


        }


    }

}
