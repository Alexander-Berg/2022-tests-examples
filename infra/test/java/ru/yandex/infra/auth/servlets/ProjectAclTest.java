package ru.yandex.infra.auth.servlets;

import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.auth.Role;
import ru.yandex.infra.auth.RoleSubject;
import ru.yandex.infra.auth.RolesInfo;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.infra.auth.yp.YpGroupsHelper.IDM_GROUP_PREFIX;


class ProjectAclTest {
    private static final String EMPTY_RESPONSE = "{\n"
            + "  \"project_acl\" : { },\n"
            + "  \"stage_acl\" : { },\n"
            + "  \"nanny_service_acl\" : { },\n"
            + "  \"box_type_acl\" : { }\n"
            + "}";
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private ProjectAcl.Builder builder;

    @BeforeEach
    void before() {
        Config config = ConfigFactory.load("roles_to_permissions.conf");
        RolesInfo rolesInfo = RolesInfo.fromConfig(config);
        builder = new ProjectAcl.Builder(rolesInfo);
    }

    @Test
    void noSubjectsTest() throws IOException {
        String result = createProjectAclResponse(emptySet());
        assertEquals(EMPTY_RESPONSE, result.replace("\r", ""));
    }

    @Test
    void noSubjectsButNonEmptyProjectTest() throws IOException {
        builder.addDefaultProjectRoles("project1");
        String result = createProjectAclResponse(emptySet());
        assertEquals("{\n"
                + "  \"project_acl\" : {\n"
                + "    \"project1\" : {\n"
                + "      \"owner\" : [ ],\n"
                + "      \"maintainer\" : [ ],\n"
                + "      \"developer\" : [ ],\n"
                + "      \"root_developer\" : [ ],\n"
                + "      \"approver\" : [ ],\n"
                + "      \"mandatory_approver\" : [ ],\n"
                + "      \"deployer\" : [ ],\n"
                + "      \"responsible\" : [ ],\n"
                + "      \"viewer\" : [ ]\n"
                + "    }\n"
                + "  },\n"
                + "  \"stage_acl\" : { },\n"
                + "  \"nanny_service_acl\" : { },\n"
                + "  \"box_type_acl\" : { }\n"
                + "}", result.replace("\r", ""));
    }

    @Test
    void nannyServiceRolesTest() throws IOException {
        Set<RoleSubject> subjects = ImmutableSet.of(
                new RoleSubject("user1", 0L, new Role("project1."+Role.NANNY_ROLES_PARENT_NODE+".service1", "developers")),
                new RoleSubject("", 5L, new Role("project1."+Role.NANNY_ROLES_PARENT_NODE+".service1", "developers"))
        );

        String result = createProjectAclResponse(subjects);
        assertEquals("{\n"
                + "  \"project_acl\" : { },\n"
                + "  \"stage_acl\" : { },\n"
                + "  \"nanny_service_acl\" : {\n"
                + "    \"service1\" : {\n"
                + "      \"evicters\" : [ ],\n"
                + "      \"ops_managers\" : [ ],\n"
                + "      \"developers\" : [ \"user1\", \"group:5\" ],\n"
                + "      \"conf_managers\" : [ ],\n"
                + "      \"owners\" : [ ],\n"
                + "      \"observers\" : [ ]\n"
                + "    }\n"
                + "  },\n"
                + "  \"box_type_acl\" : { }\n"
                + "}", result.replace("\r", ""));
    }

    @Test
    void notVisibleRolesTest() throws IOException {
        Set<RoleSubject> subjects = ImmutableSet.of(
                new RoleSubject("fifth", 0L, new Role("", "SUPER_USER")),
                new RoleSubject("first", 0L, new Role("project", "SYSTEM_DEVELOPER")),
                new RoleSubject("second", 0L, new Role("project.stage", "SYSTEM_DEVELOPER")),
                new RoleSubject("second", 0L, new Role("project.stage2", "SYSTEM_DEVELOPER"))
        );

        String result = createProjectAclResponse(subjects);
        assertEquals("{\n"
                + "  \"project_acl\" : {\n"
                + "    \"project\" : {\n"
                + "      \"owner\" : [ ],\n"
                + "      \"maintainer\" : [ ],\n"
                + "      \"developer\" : [ ],\n"
                + "      \"root_developer\" : [ ],\n"
                + "      \"approver\" : [ ],\n"
                + "      \"mandatory_approver\" : [ ],\n"
                + "      \"deployer\" : [ ],\n"
                + "      \"responsible\" : [ ],\n"
                + "      \"viewer\" : [ ]\n"
                + "    }\n"
                + "  },\n"
                + "  \"stage_acl\" : {\n"
                + "    \"stage\" : {\n"
                + "      \"maintainer\" : [ ],\n"
                + "      \"developer\" : [ ],\n"
                + "      \"root_developer\" : [ ],\n"
                + "      \"approver\" : [ ],\n"
                + "      \"mandatory_approver\" : [ ],\n"
                + "      \"deployer\" : [ ],\n"
                + "      \"responsible\" : [ ],\n"
                + "      \"viewer\" : [ ]\n"
                + "    },\n"
                + "    \"stage2\" : {\n"
                + "      \"maintainer\" : [ ],\n"
                + "      \"developer\" : [ ],\n"
                + "      \"root_developer\" : [ ],\n"
                + "      \"approver\" : [ ],\n"
                + "      \"mandatory_approver\" : [ ],\n"
                + "      \"deployer\" : [ ],\n"
                + "      \"responsible\" : [ ],\n"
                + "      \"viewer\" : [ ]\n"
                + "    }\n"
                + "  },\n"
                + "  \"nanny_service_acl\" : { },\n"
                + "  \"box_type_acl\" : { }\n"
                + "}", result.replace("\r", ""));
    }

    @Test
    void unknownsAndUnexpectedRolesTest() throws IOException {
        Set<RoleSubject> subjects = ImmutableSet.of(
                new RoleSubject("first", 0L, new Role("project", "SYSTEM_USER")),
                new RoleSubject("second", 0L, new Role("project.stage", "GOOD_GUY")),
                new RoleSubject("second", 0L, new Role("project.stage", "OWNER")),
                new RoleSubject("second", 0L, new Role("project.stage2", "INFORMER")),
                new RoleSubject("second", 0L, new Role("project.stage2", "SUPER_USER")),
                new RoleSubject("second", 0L, new Role("project.stage2.sox-box", "OWNER")),
                new RoleSubject("third", 0L, new Role("project.stage2.sox-box", "MAINTAINER")),
                new RoleSubject("third", 0L, new Role("project.stage2.sox-box", "SUPER_USER"))
        );

        String result = createProjectAclResponse(subjects);
        assertEquals("{\n"
                + "  \"project_acl\" : {\n"
                + "    \"project\" : {\n"
                + "      \"owner\" : [ ],\n"
                + "      \"maintainer\" : [ ],\n"
                + "      \"developer\" : [ ],\n"
                + "      \"root_developer\" : [ ],\n"
                + "      \"approver\" : [ ],\n"
                + "      \"mandatory_approver\" : [ ],\n"
                + "      \"deployer\" : [ ],\n"
                + "      \"responsible\" : [ ],\n"
                + "      \"viewer\" : [ ]\n"
                + "    }\n"
                + "  },\n"
                + "  \"stage_acl\" : {\n"
                + "    \"stage\" : {\n"
                + "      \"maintainer\" : [ ],\n"
                + "      \"developer\" : [ ],\n"
                + "      \"root_developer\" : [ ],\n"
                + "      \"approver\" : [ ],\n"
                + "      \"mandatory_approver\" : [ ],\n"
                + "      \"deployer\" : [ ],\n"
                + "      \"responsible\" : [ ],\n"
                + "      \"viewer\" : [ ]\n"
                + "    },\n"
                + "    \"stage2\" : {\n"
                + "      \"maintainer\" : [ ],\n"
                + "      \"developer\" : [ ],\n"
                + "      \"root_developer\" : [ ],\n"
                + "      \"approver\" : [ ],\n"
                + "      \"mandatory_approver\" : [ ],\n"
                + "      \"deployer\" : [ ],\n"
                + "      \"responsible\" : [ ],\n"
                + "      \"viewer\" : [ ]\n"
                + "    }\n"
                + "  },\n"
                + "  \"nanny_service_acl\" : { },\n"
                + "  \"box_type_acl\" : {\n"
                + "    \"stage2\" : {\n"
                + "      \"sox-box\" : {\n"
                + "        \"developer\" : [ ],\n"
                + "        \"root_developer\" : [ ]\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}", result.replace("\r", ""));
    }

    @Test
    void tooMuchLevelsTest() throws IOException {
        Set<RoleSubject> subjects = ImmutableSet.of(
                new RoleSubject("second", 0L, new Role("project.stage.box.something-else", "MAINTAINER")),
                new RoleSubject("first", 0L, new Role("project.stage.box.something-else.more.more", "MAINTAINER"))
        );

        String result = createProjectAclResponse(subjects);
        assertEquals(EMPTY_RESPONSE, result.replace("\r", ""));
    }

    @Test
    void onlyProjectsTest() throws IOException {
        Set<RoleSubject> subjects = ImmutableSet.of(
                new RoleSubject("first", 0L, new Role("project", "OWNER")),
                new RoleSubject("second", 0L, new Role("project", "OWNER"))
        );

        String result = createProjectAclResponse(subjects);
        assertEquals("{\n"
                + "  \"project_acl\" : {\n"
                + "    \"project\" : {\n"
                + "      \"owner\" : [ \"first\", \"second\" ],\n"
                + "      \"maintainer\" : [ ],\n"
                + "      \"developer\" : [ ],\n"
                + "      \"root_developer\" : [ ],\n"
                + "      \"approver\" : [ ],\n"
                + "      \"mandatory_approver\" : [ ],\n"
                + "      \"deployer\" : [ ],\n"
                + "      \"responsible\" : [ ],\n"
                + "      \"viewer\" : [ ]\n"
                + "    }\n"
                + "  },\n"
                + "  \"stage_acl\" : { },\n"
                + "  \"nanny_service_acl\" : { },\n"
                + "  \"box_type_acl\" : { }\n"
                + "}", result.replace("\r", ""));
    }

    @Test
    void projectsAndStagesTest() throws IOException {
        Set<RoleSubject> subjects = ImmutableSet.of(
                new RoleSubject("first", 0L, new Role("project", "OWNER")),
                new RoleSubject("", 1000L, new Role("project", "OWNER")),
                new RoleSubject("second", 0L, new Role("project.stage", "MAINTAINER")),
                new RoleSubject("", 2000L, new Role("project.stage2", "MAINTAINER"))
        );

        String result = createProjectAclResponse(subjects);
        assertEquals("{\n"
                + "  \"project_acl\" : {\n"
                + "    \"project\" : {\n"
                + "      \"owner\" : [ \"first\", \"group:1000\" ],\n"
                + "      \"maintainer\" : [ ],\n"
                + "      \"developer\" : [ ],\n"
                + "      \"root_developer\" : [ ],\n"
                + "      \"approver\" : [ ],\n"
                + "      \"mandatory_approver\" : [ ],\n"
                + "      \"deployer\" : [ ],\n"
                + "      \"responsible\" : [ ],\n"
                + "      \"viewer\" : [ ]\n"
                + "    }\n"
                + "  },\n"
                + "  \"stage_acl\" : {\n"
                + "    \"stage\" : {\n"
                + "      \"maintainer\" : [ \"second\" ],\n"
                + "      \"developer\" : [ ],\n"
                + "      \"root_developer\" : [ ],\n"
                + "      \"approver\" : [ ],\n"
                + "      \"mandatory_approver\" : [ ],\n"
                + "      \"deployer\" : [ ],\n"
                + "      \"responsible\" : [ ],\n"
                + "      \"viewer\" : [ ]\n"
                + "    },\n"
                + "    \"stage2\" : {\n"
                + "      \"maintainer\" : [ \"group:2000\" ],\n"
                + "      \"developer\" : [ ],\n"
                + "      \"root_developer\" : [ ],\n"
                + "      \"approver\" : [ ],\n"
                + "      \"mandatory_approver\" : [ ],\n"
                + "      \"deployer\" : [ ],\n"
                + "      \"responsible\" : [ ],\n"
                + "      \"viewer\" : [ ]\n"
                + "    }\n"
                + "  },\n"
                + "  \"nanny_service_acl\" : { },\n"
                + "  \"box_type_acl\" : { }\n"
                + "}", result.replace("\r", ""));
    }


    @Test
    void allSubjectsTest() throws IOException {
        Set<RoleSubject> subjects = ImmutableSet.of(
                new RoleSubject("fifth", 0L, new Role("", "SUPER_USER")),
                new RoleSubject("first", 0L, new Role("project", "OWNER")),
                new RoleSubject("second", 0L, new Role("project", "OWNER")),
                new RoleSubject("third", 0L, new Role("project", "APPROVER")),
                new RoleSubject("fourth", 0L, new Role("project", "MANDATORY_APPROVER")),
                new RoleSubject("fifth", 0L, new Role("project", "DEPLOYER")),
                new RoleSubject("second", 0L, new Role("project.stage", "MAINTAINER")),
                new RoleSubject("second", 0L, new Role("project.stage2", "MAINTAINER")),
                new RoleSubject("fourth", 0L, new Role("project.stage", "SYSTEM_DEVELOPER")),
                new RoleSubject("seventh", 0L, new Role("project.stage", "APPROVER")),
                new RoleSubject("fourth", 0L, new Role("project.stage2", "MANDATORY_APPROVER")),
                new RoleSubject("fourth", 0L, new Role("project.stage2", "RESPONSIBLE")),
                new RoleSubject("third", 0L, new Role("project.stage.sox_box", "DEVELOPER")),
                new RoleSubject("sixth", 0L, new Role("project.stage.secret_box", "ROOT_DEVELOPER")),
                new RoleSubject("third", 0L, new Role("project.stage2.sox_box", "DEVELOPER"))
        );

        String result = createProjectAclResponse(subjects);
        assertEquals("{\n"
                + "  \"project_acl\" : {\n"
                + "    \"project\" : {\n"
                + "      \"owner\" : [ \"first\", \"second\" ],\n"
                + "      \"maintainer\" : [ ],\n"
                + "      \"developer\" : [ ],\n"
                + "      \"root_developer\" : [ ],\n"
                + "      \"approver\" : [ \"third\" ],\n"
                + "      \"mandatory_approver\" : [ \"fourth\" ],\n"
                + "      \"deployer\" : [ \"fifth\" ],\n"
                + "      \"responsible\" : [ ],\n"
                + "      \"viewer\" : [ ]\n"
                + "    }\n"
                + "  },\n"
                + "  \"stage_acl\" : {\n"
                + "    \"stage\" : {\n"
                + "      \"maintainer\" : [ \"second\" ],\n"
                + "      \"developer\" : [ ],\n"
                + "      \"root_developer\" : [ ],\n"
                + "      \"approver\" : [ \"seventh\" ],\n"
                + "      \"mandatory_approver\" : [ ],\n"
                + "      \"deployer\" : [ ],\n"
                + "      \"responsible\" : [ ],\n"
                + "      \"viewer\" : [ ]\n"
                + "    },\n"
                + "    \"stage2\" : {\n"
                + "      \"maintainer\" : [ \"second\" ],\n"
                + "      \"developer\" : [ ],\n"
                + "      \"root_developer\" : [ ],\n"
                + "      \"approver\" : [ ],\n"
                + "      \"mandatory_approver\" : [ \"fourth\" ],\n"
                + "      \"deployer\" : [ ],\n"
                + "      \"responsible\" : [ \"fourth\" ],\n"
                + "      \"viewer\" : [ ]\n"
                + "    }\n"
                + "  },\n"
                + "  \"nanny_service_acl\" : { },\n"
                + "  \"box_type_acl\" : {\n"
                + "    \"stage\" : {\n"
                + "      \"sox_box\" : {\n"
                + "        \"developer\" : [ \"third\" ],\n"
                + "        \"root_developer\" : [ ]\n"
                + "      },\n"
                + "      \"secret_box\" : {\n"
                + "        \"developer\" : [ ],\n"
                + "        \"root_developer\" : [ \"sixth\" ]\n"
                + "      }\n"
                + "    },\n"
                + "    \"stage2\" : {\n"
                + "      \"sox_box\" : {\n"
                + "        \"developer\" : [ \"third\" ],\n"
                + "        \"root_developer\" : [ ]\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}", result.replace("\r", ""));
    }

    private String createProjectAclResponse(Set<RoleSubject> subjects) throws IOException {
        subjects.forEach(subject -> builder.addAcl(subject.getRole(),
                subject.isPersonal() ? subject.getLogin() : IDM_GROUP_PREFIX + subject.getGroupId()));
        ProjectAcl projectAcl = builder.build();
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(projectAcl);
    }
}
