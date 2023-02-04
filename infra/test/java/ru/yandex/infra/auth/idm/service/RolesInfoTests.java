package ru.yandex.infra.auth.idm.service;

import java.util.Set;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.auth.RolesInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RolesInfoTests {
    private RolesInfo rolesInfo;

    @BeforeEach
    void before() {
        Config rootConfig = ConfigFactory.load("roles_to_permissions.conf");
        rolesInfo = RolesInfo.fromConfig(rootConfig);
    }

    @Test
    void getRolesPerLevelTest() {
        assertEquals(Set.of("SUPER_USER"), rolesInfo.getRolesPerLevel(RolesInfo.LevelName.SYSTEM));
        assertEquals(Set.of(
            "OWNER", "DEPLOYER", "DEVELOPER", "RESPONSIBLE", "SYSTEM_DEVELOPER", "VIEWER", "MAINTAINER",
            "ROOT_DEVELOPER", "MANDATORY_APPROVER", "APPROVER"
        ), rolesInfo.getRolesPerLevel(RolesInfo.LevelName.PROJECT));
        assertEquals(Set.of(
            "DEVELOPER", "RESPONSIBLE", "SYSTEM_DEVELOPER", "VIEWER", "MAINTAINER",
            "ROOT_DEVELOPER", "DEPLOYER", "MANDATORY_APPROVER", "APPROVER"
        ), rolesInfo.getRolesPerLevel(RolesInfo.LevelName.STAGE));
        assertEquals(Set.of(
            "DEVELOPER", "ROOT_DEVELOPER"
        ), rolesInfo.getRolesPerLevel(RolesInfo.LevelName.BOX));
        assertEquals(Set.of(
            "evicters", "ops_managers", "developers", "conf_managers", "owners", "observers"
        ), rolesInfo.getRolesPerLevel(RolesInfo.LevelName.NANNY_SERVICE));

    }
}
