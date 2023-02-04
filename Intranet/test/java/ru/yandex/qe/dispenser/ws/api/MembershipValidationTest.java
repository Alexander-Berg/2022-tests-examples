package ru.yandex.qe.dispenser.ws.api;


import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.ProjectRole;
import ru.yandex.qe.dispenser.domain.abc.AbcPerson;
import ru.yandex.qe.dispenser.domain.abc.AbcRole;
import ru.yandex.qe.dispenser.domain.abc.AbcService;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceMember;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceResponsible;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceState;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.project.role.ProjectRoleCache;
import ru.yandex.qe.dispenser.domain.dao.project.role.ProjectRoleDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Role;
import ru.yandex.qe.dispenser.standalone.MockAbcApi;
import ru.yandex.qe.dispenser.ws.abc.AbcApiHelper;
import ru.yandex.qe.dispenser.ws.abc.validator.ProjectMembersValidationResult;
import ru.yandex.qe.dispenser.ws.abc.validator.ProjectMembersValidatorImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MembershipValidationTest extends ApiTestBase {

    private static final String PATH = "/v1/staff/_validateRoles";

    @Autowired
    private MockAbcApi abcApi;

    @Autowired
    private ProjectRoleDao projectRoleDao;

    @Autowired
    private AbcApiHelper abcApiHelper;

    @Autowired
    private ProjectRoleCache projectRoleCache;

    @Autowired
    private PersonDao personDao;

    private ProjectMembersValidatorImpl validator;

    @BeforeAll
    public void beforeClass() {
        validator = new ProjectMembersValidatorImpl(personDao, abcApiHelper, projectRoleCache);
    }

    @Test
    public void validate() {
        final ProjectMembersValidationResult result = createAuthorizedLocalClient(AMOSOV_F)
                .path(PATH)
                .get(ProjectMembersValidationResult.class);
        Assertions.assertTrue(result.getMissingRoles().get(Role.MEMBER.getKey()).isEmpty());
        Assertions.assertTrue(result.getMissingRoles().get(Role.RESPONSIBLE.getKey()).isEmpty());
        Assertions.assertTrue(result.getExcessRoles().get(Role.MEMBER.getKey()).isEmpty());
        Assertions.assertTrue(result.getExcessRoles().get(Role.RESPONSIBLE.getKey()).isEmpty());
    }

    @Test
    public void validateShouldCheckExcess() {
        projectRoleDao.clear();
        final ProjectRole memberRole = projectRoleDao.create(new ProjectRole(Role.MEMBER.getKey(), ProjectRole.AbcRoleSyncType.MEMBER, null));
        final ProjectRole otherRole = projectRoleDao.create(new ProjectRole("role10", ProjectRole.AbcRoleSyncType.ROLE, 10L));

        abcApi.reset();
        final AbcService abcService1 = abcApi.addService(new AbcService(1, Collections.emptyList(), null, null, null, null, AbcServiceState.DEVELOP));

        final AbcRole role10 = abcApi.addRole(new AbcRole(10, abcService1));
        final AbcRole role11 = abcApi.addRole(new AbcRole(11, abcService1));

        abcApi.addMember(new AbcServiceMember(new AbcPerson("slonnn"), abcService1, role10, 1));
        abcApi.addMember(new AbcServiceMember(new AbcPerson("binarycat"), abcService1, role11, 2));

        final Project project = projectDao.create(Project.withKey("abc-sync-1")
                .parent(projectDao.read("yandex"))
                .syncedWithAbc(true)
                .name("abc-sync-1")
                .description("abc-sync-1")
                .abcServiceId(1)
                .build());

        final Person amosovf = personDao.read(AMOSOV_F.getLogin());
        final Person binaryCat = personDao.read(BINARY_CAT.getLogin());
        final Person slonnn = personDao.read(SLONNN.getLogin());

        projectDao.attachAll(ImmutableSet.of(binaryCat, amosovf, slonnn), Collections.emptySet(), project, memberRole.getId());
        projectDao.attachAll(ImmutableSet.of(binaryCat, amosovf, slonnn), Collections.emptySet(), project, otherRole.getId());

        updateHierarchy();

        final ProjectMembersValidationResult result = validator.validate();

        assertEquals(Collections.emptyMap(), result.getMissingRoles().get(Role.MEMBER.getKey()));
        assertEquals(Collections.emptyMap(), result.getMissingRoles().get("role10"));

        assertEquals(result.getExcessRoles().get(Role.MEMBER.getKey()), ImmutableMap.of(
                amosovf.getLogin(), ImmutableSet.of(1L)
        ));

        assertEquals(result.getExcessRoles().get("role10"), ImmutableMap.of(
                amosovf.getLogin(), ImmutableSet.of(1L),
                binaryCat.getLogin(), ImmutableSet.of(1L)
        ));
    }

    @Test
    public void validateShouldCheckMiss() {
        projectRoleDao.clear();
        final ProjectRole memberRole = projectRoleDao.create(new ProjectRole(Role.MEMBER.getKey(), ProjectRole.AbcRoleSyncType.MEMBER, null));
        projectRoleDao.create(new ProjectRole("role10", ProjectRole.AbcRoleSyncType.ROLE, 10L));

        abcApi.reset();
        final AbcService abcService1 = abcApi.addService(new AbcService(1, Collections.emptyList(), null, null, null, null, AbcServiceState.DEVELOP));

        final AbcRole role10 = abcApi.addRole(new AbcRole(10, abcService1));
        final AbcRole role11 = abcApi.addRole(new AbcRole(11, abcService1));

        abcApi.addResponsible(new AbcServiceResponsible(new AbcPerson("amosov-f"), abcService1));

        abcApi.addMember(new AbcServiceMember(new AbcPerson("sterligovak"), abcService1, role10, 1));
        abcApi.addMember(new AbcServiceMember(new AbcPerson("slonnn"), abcService1, role11, 2));

        final Project project = projectDao.create(Project.withKey("abc-sync-1")
                .parent(projectDao.read("yandex"))
                .syncedWithAbc(true)
                .name("abc-sync-1")
                .description("abc-sync-1")
                .abcServiceId(1)
                .build());

        updateHierarchy();

        final ProjectMembersValidationResult result = validator.validate();

        assertEquals(ImmutableMap.of(
                "sterligovak", ImmutableSet.of(1L),
                "slonnn", ImmutableSet.of(1L)
        ), result.getMissingRoles().get(Role.MEMBER.getKey()));

        assertEquals(ImmutableMap.of(
                "sterligovak", ImmutableSet.of(1L)
        ), result.getMissingRoles().get("role10"));

        assertEquals(Collections.emptyMap(), result.getExcessRoles().get(Role.MEMBER.getKey()));
        assertEquals(Collections.emptyMap(), result.getExcessRoles().get("role10"));
    }
}
