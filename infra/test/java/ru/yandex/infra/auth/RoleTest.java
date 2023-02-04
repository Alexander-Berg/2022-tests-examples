package ru.yandex.infra.auth;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.infra.auth.nanny.NannyRole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleTest {
    private static final String MAINTAINER_ROLE = "MAINTAINER";
    private static final String DEVELOPER_ROLE = "DEVELOPER";

    RoleTest() {
    }

    @ParameterizedTest
    @CsvSource(value = {
            "'','',true,'','',,", //rootRole
            "'',MAINTAINER,false,MAINTAINER,MAINTAINER,,",//root Role with leaf
            "project,'',false,project,'',project,",//project node
            "project,OWNER,false,project.OWNER,OWNER,project,",//project role
            "projectA.stageB,'',false,projectA.stageB,'',projectA,stageB",//stage node
            "projectA.stageB,DEVELOPER,false,projectA.stageB.DEVELOPER,DEVELOPER,projectA,stageB",//stage role
            "projectA.stageB.box,DEVELOPER,false,projectA.stageB.box.DEVELOPER,DEVELOPER,projectA,stageB",//box role
    })
    void roleAttributes(String rolePath,
                        String leaf,
                        boolean expectedIsRoot,
                        String expectedLevelsJoinedWithDelimiter,
                        String expectedLeaf,
                        String expectedProjectId,
                        String expectedStageId) {

        Role role = new Role(rolePath, leaf, "", "");

        assertEquals(expectedIsRoot, role.isRoot());
        assertEquals(expectedLevelsJoinedWithDelimiter, role.getLevelsJoinedWithDelimiter());
        assertEquals(expectedLeaf, role.getLeaf());
        assertEquals(expectedProjectId, role.getProjectId().orElse(null));
        assertEquals(expectedStageId, role.getStageId().orElse(null));
        assertEquals(role.getLevelsJoinedWithDelimiter(), role.toString());
    }

    @Test
    void sortedSetOfRoles() {
        SortedSet<Role> roles = new TreeSet<>(ImmutableSet.of(
                new Role("project.stage", ""),
                new Role("project.stage", DEVELOPER_ROLE),
                new Role("project", DEVELOPER_ROLE),
                new Role("project", ""),
                new Role("project", MAINTAINER_ROLE),
                new Role("project-test.stage", MAINTAINER_ROLE),
                new Role("project-test.stage", DEVELOPER_ROLE),
                new Role("project-test", DEVELOPER_ROLE),
                new Role("project-test.stage", ""),
                new Role("project.stage", MAINTAINER_ROLE),
                new Role("project-test", MAINTAINER_ROLE),
                new Role("project-test", "")));

        List<String> sortedRoles = roles.stream()
                .map(Role::getLevelsJoinedWithDelimiter)
                .collect(Collectors.toList());
        assertEquals(sortedRoles, ImmutableList.of(
                "project",
                "project.DEVELOPER",
                "project.MAINTAINER",
                "project.stage",
                "project.stage.DEVELOPER",
                "project.stage.MAINTAINER",
                "project-test",
                "project-test.DEVELOPER",
                "project-test.MAINTAINER",
                "project-test.stage",
                "project-test.stage.DEVELOPER",
                "project-test.stage.MAINTAINER"));
    }

    @Test
    void getLeafUniqueIdTest() {
        assertEquals("", Role.getLeafUniqueId(null, "DEVELOPER"));
        assertEquals("", Role.getLeafUniqueId("", "DEVELOPER"));
        assertEquals("DEVELOPER.1d457f2b-84fb4d5e-c7c2624-9d12e8b9", Role.getLeafUniqueId("1d457f2b-84fb4d5e-c7c2624-9d12e8b9", "DEVELOPER"));
    }

    @Test
    void getExtendedDescription() {
        assertEquals("project.stage.MAINTAINER", new Role("project.stage", "MAINTAINER", "", "").getExtendedDescription());
        assertEquals("project.stage.MAINTAINER, uniqueId = 1d457f2b-84fb4d5e-c7c2624-9d12e8b9", new Role("project.stage", "MAINTAINER", "", "1d457f2b-84fb4d5e-c7c2624-9d12e8b9").getExtendedDescription());
    }

    @Test
    void superUserRole() {
        Role su = Role.superUser();

        assertEquals("SUPER_USER", su.getLeaf());
        assertEquals("", su.getUniqueId());
        assertEquals(new Role("", "SUPER_USER", "", ""), su);
    }

    @Test
    void emptyRole() {
        assertEquals(new Role("", "", "", ""), Role.empty());
    }

    @Test
    void equalsTest() {
        final Role role = new Role("a", "b", "c", "d");

        assertEquals(role, new Role("a", "b", "creatorShouldBeIgnoredDuringComparison", "d"));

        assertNotEquals(role, new Role("aa", "b", "c", "d"));
        assertNotEquals(role, new Role("a", "bb", "c", "d"));
        assertNotEquals(role, new Role("a", "b", "c", "dd"));

        assertNotEquals(role, new Role("", "b", "c", "d"));
        assertNotEquals(role, new Role("a", "", "c", "d"));
        assertNotEquals(role, new Role("a", "b", "c", ""));
    }

    @Test
    void isNannyRoleTest() {
        assertFalse(Role.empty().isNannyRole());
        assertFalse(Role.superUser().isNannyRole());
        assertFalse(new Role("Project", "OWNER").isNannyRole());
        assertFalse(new Role("Project", "").isNannyRole());
        assertFalse(new Role("Project.Stage", "DEPLOYER").isNannyRole());
        assertTrue(new Role(Role.NANNY_ROLES_PARENT_NODE, "OWNERS").isNannyRole());
        assertTrue(new Role(Role.NANNY_ROLES_PARENT_NODE, "").isNannyRole());
        assertTrue(new Role(Role.NANNY_ROLES_PARENT_NODE + ".service1", "").isNannyRole());
        assertTrue(new Role(Role.NANNY_ROLES_PARENT_NODE + ".service1", "owners").isNannyRole());

        assertTrue(new Role("Project." + Role.NANNY_ROLES_PARENT_NODE, "").isNannyRole());
        assertTrue(new Role("Project." + Role.NANNY_ROLES_PARENT_NODE + ".service1", "").isNannyRole());
        assertTrue(new Role("Project." + Role.NANNY_ROLES_PARENT_NODE + ".service1", "owners").isNannyRole());
        assertTrue(new Role("Project." + Role.NANNY_ROLES_PARENT_NODE + ".service1", "owners", "", "uniqueId").isNannyRole());
        assertTrue(new NannyRole("Project." + Role.NANNY_ROLES_PARENT_NODE + ".service2", "owners", "", Collections.emptySet(), Collections.emptySet()).isNannyRole());
    }

    @Test
    void getNannyServiceIdTest() {
        assertEquals(Optional.empty(), new Role("Project." + Role.NANNY_ROLES_PARENT_NODE, "").getNannyServiceId());
        assertEquals(Optional.of("service1"), new Role("Project." + Role.NANNY_ROLES_PARENT_NODE + ".service1", "").getNannyServiceId());
        assertEquals(Optional.of("service1"), new Role("Project." + Role.NANNY_ROLES_PARENT_NODE + ".service1", "owners").getNannyServiceId());
        assertEquals(Optional.of("service2"), new NannyRole("Project." + Role.NANNY_ROLES_PARENT_NODE + ".service2", "owners", "", Collections.emptySet(), Collections.emptySet()).getNannyServiceId());
    }

    @Test
    void getLastLevelTest() {
        assertEquals("", Role.empty().getLastLevelName());
        assertEquals("SUPER_USER", Role.superUser().getLastLevelName());
        assertEquals("OWNER", new Role("Project", "OWNER").getLastLevelName());
        assertEquals("Project", new Role("Project", "").getLastLevelName());
        assertEquals("DEPLOYER", new Role("Project.Stage", "DEPLOYER").getLastLevelName());
        assertEquals(Role.NANNY_ROLES_PARENT_NODE, new Role("Project." + Role.NANNY_ROLES_PARENT_NODE, "").getLastLevelName());
        assertEquals("service1", new Role("Project." + Role.NANNY_ROLES_PARENT_NODE + ".service1", "").getLastLevelName());
        assertEquals("owners", new Role("Project." + Role.NANNY_ROLES_PARENT_NODE + ".service1", "owners").getLastLevelName());
    }

    @Test
    void getIDMNodeNameTest() {
        assertEquals("Project role or Stage name", new Role("Project", "").getIDMKeyNodeName());
        assertEquals("Stage role or box name", new Role("Project.Stage", "").getIDMKeyNodeName());
        assertEquals("Service name", new Role("Project." + Role.NANNY_ROLES_PARENT_NODE, "").getIDMKeyNodeName());
        assertEquals("role", new Role("Project." + Role.NANNY_ROLES_PARENT_NODE + ".service1", "").getIDMKeyNodeName());
    }

    @Test
    void getLeafLevelTest() {
        assertNull(Role.empty().getLeafLevel());
        assertEquals(RolesInfo.LevelName.SYSTEM, Role.superUser().getLeafLevel());
        assertEquals(RolesInfo.LevelName.PROJECT, new Role("Project", "OWNER").getLeafLevel());
        assertNull(new Role("Project", "").getLeafLevel());
        assertEquals(RolesInfo.LevelName.STAGE, new Role("Project.Stage", "DEPLOYER").getLeafLevel());
        assertNull(new Role("Project." + Role.NANNY_ROLES_PARENT_NODE, "").getLeafLevel());
        assertNull(new Role("Project." + Role.NANNY_ROLES_PARENT_NODE + ".service1", "").getLeafLevel());
        assertEquals(RolesInfo.LevelName.NANNY_SERVICE, new Role("Project." + Role.NANNY_ROLES_PARENT_NODE + ".service1", "owners").getLeafLevel());
    }

    @Test
    void getLeafLevelIgnoringLeafTest() {
        assertEquals(0, Role.empty().getLevelIgnoringLeaf());
        assertEquals(1, Role.superUser().getLevelIgnoringLeaf());

        assertEquals(1, new Role("Project", "").getLevelIgnoringLeaf());
        assertEquals(1, new Role("Project", "OWNER").getLevelIgnoringLeaf());

        assertEquals(2, new Role("Project.Stage", "").getLevelIgnoringLeaf());
        assertEquals(2, new Role("Project.Stage", "DEPLOYER").getLevelIgnoringLeaf());

        assertEquals(3, new Role("Project.Stage.Box", "").getLevelIgnoringLeaf());
        assertEquals(3, new Role("Project.Stage.Box", "DEVELOPER").getLevelIgnoringLeaf());

        assertEquals(1, new Role(Role.NANNY_ROLES_PARENT_NODE, "").getLevelIgnoringLeaf());
        assertEquals(2, new Role(Role.NANNY_ROLES_PARENT_NODE + ".service1", "").getLevelIgnoringLeaf());
        assertEquals(2, new Role(Role.NANNY_ROLES_PARENT_NODE + ".service1", "owners").getLevelIgnoringLeaf());

        assertEquals(2, new Role("Project." + Role.NANNY_ROLES_PARENT_NODE, "").getLevelIgnoringLeaf());
        assertEquals(3, new Role("Project." + Role.NANNY_ROLES_PARENT_NODE + ".service1", "").getLevelIgnoringLeaf());
        assertEquals(3, new Role("Project." + Role.NANNY_ROLES_PARENT_NODE + ".service1", "owners").getLevelIgnoringLeaf());
    }

}
