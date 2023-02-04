package ru.yandex.infra.auth.idm.service;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.auth.Role;

import static java.util.Collections.emptyMap;
import static junit.framework.Assert.assertEquals;

class IdmRoleTest {
    IdmRoleTest() {
    }

    @Test
    void notLeafRootRole() {
        IdmRole role = IdmRole.createFromRole(new Role("", ""));
        assertEquals("", role.getPath());
        assertEquals("ROOT", role.getName());
        assertEquals("/", role.getValuePath());
        assertEquals("/", role.getIdmPath());
        assertEquals("/ROOT/", role.getFullIdmPath());
        assertEquals(emptyMap(), role.getRoles());
    }

    @Test
    void leafRootRole() {
        IdmRole role = IdmRole.createFromRole(new Role("", "SUPER_USER"));
        assertEquals("", role.getPath());
        assertEquals("SUPER_USER", role.getName());
        assertEquals("/SUPER_USER/", role.getValuePath());
        assertEquals("/ROOT/", role.getIdmPath());
        assertEquals("/ROOT/SUPER_USER/", role.getFullIdmPath());
        assertEquals(ImmutableMap.of("ROOT", "SUPER_USER"), role.getRoles());
    }


    @Test
    void notLeafRole() {
        IdmRole role = IdmRole.createFromRole(new Role("project.stage", ""));
        assertEquals("project", role.getPath());
        assertEquals("stage", role.getName());
        assertEquals("/project/stage/", role.getValuePath());
        assertEquals("/ROOT/project/project/", role.getIdmPath());
        assertEquals("/ROOT/project/project/stage/", role.getFullIdmPath());
        assertEquals(ImmutableMap.of(
                "ROOT", "project",
                "project", "stage"), role.getRoles());

    }

    @Test
    void leafRole() {
        IdmRole role = IdmRole.createFromRole(new Role("project.stage", "DEVELOPER"));

        assertEquals("project.stage", role.getPath());
        assertEquals("DEVELOPER", role.getName());
        assertEquals("/project/stage/DEVELOPER/", role.getValuePath());
        assertEquals("/ROOT/project/project/stage/project.stage/", role.getIdmPath());
        assertEquals("/ROOT/project/project/stage/project.stage/DEVELOPER/", role.getFullIdmPath());
        assertEquals(ImmutableMap.of(
                "ROOT", "project",
                "project", "stage",
                "project.stage", "DEVELOPER"), role.getRoles());
    }

    @Test
    void leafRoleShorterThanROOT() {
        IdmRole role = IdmRole.createFromRole(new Role("p.s", "DEVELOPER"));

        assertEquals("p.s", role.getPath());
        assertEquals("DEVELOPER", role.getName());
        assertEquals("/p/s/DEVELOPER/", role.getValuePath());
        assertEquals("/ROOT/p/p/s/p.s/", role.getIdmPath());
        assertEquals("/ROOT/p/p/s/p.s/DEVELOPER/", role.getFullIdmPath());
        assertEquals(ImmutableMap.of(
                "ROOT", "p",
                "p", "s",
                "p.s", "DEVELOPER"), role.getRoles());
    }
}
