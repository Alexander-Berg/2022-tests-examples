package ru.yandex.infra.auth;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.infra.auth.staff.StaffGroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.infra.auth.GroupsAndUsersCacheImpl.YP_ABC_SERVICE_GROUP_PREFIX;
import static ru.yandex.infra.auth.GroupsAndUsersCacheImpl.YP_DEPARTMENT_GROUP_PREFIX;

public class StaffToYpGroupsMapTests {

    private GroupsAndUsersCacheImpl.Groups getEmptyObjects() {
        var result = new GroupsAndUsersCacheImpl.Groups();
        result.staffDepartmentGroups = Collections.emptyList();
        result.staffAbcGroups = Collections.emptyList();
        result.staffAbcRoleGroups = Collections.emptyList();
        result.ypAllGroupIds = Collections.emptySet();
        result.ypAbcScopeGroups = Collections.emptyMap();
        return result;
    }

    @Test
    void mapDepartmentGroupTest() {
        var groups = getEmptyObjects();
        groups.staffDepartmentGroups = List.of(
                new StaffGroup(1L, null, "department_1_url", null, new StaffGroup.ObjectWithId("123"), null),
                new StaffGroup(10L, null, "department_10_url", null, new StaffGroup.ObjectWithId("256"), null));
        groups.ypAllGroupIds = Set.of(YP_DEPARTMENT_GROUP_PREFIX + 123, YP_DEPARTMENT_GROUP_PREFIX + 124);

        assertEquals(Map.of(
                    "1", YP_DEPARTMENT_GROUP_PREFIX + 123,
                    "department_1_url", YP_DEPARTMENT_GROUP_PREFIX + 123
                ),
                GroupsAndUsersCacheImpl.mapStaffToYpGroups(groups));
    }

    @Test
    void abcServiceGroupTest() {
        var groups = getEmptyObjects();
        groups.staffAbcGroups = List.of(
                new StaffGroup(1L, null, "some_url", new StaffGroup.ObjectWithId("123"), null, null),
                new StaffGroup(10L, null, "some_url_10", new StaffGroup.ObjectWithId("256"), null, null));
        groups.ypAllGroupIds = Set.of(YP_ABC_SERVICE_GROUP_PREFIX + 123, YP_ABC_SERVICE_GROUP_PREFIX + 124);

        assertEquals(Map.of("1", YP_ABC_SERVICE_GROUP_PREFIX + 123), GroupsAndUsersCacheImpl.mapStaffToYpGroups(groups));
    }

    @Test
    void abcRoleGroupTest() {

        var abcService = new StaffGroup(333L, null, "drug", new StaffGroup.ObjectWithId("3494"), null, null);
        var abcService2 = new StaffGroup(111L, null, "yp_service_url", new StaffGroup.ObjectWithId("1979"), null, null);

        var groups = getEmptyObjects();

        groups.staffAbcRoleGroups = List.of(
            new StaffGroup(64443L, "admins", "some_url", null, null, abcService),
            new StaffGroup(64444L, "duty", "some_url2", null, null, abcService),
            new StaffGroup(158394L, "devops", "some_url_10", null, null, abcService2)
        );

        groups.ypAbcScopeGroups = Map.of(
            GroupsAndUsersCacheImpl.getAbcRoleGroupKey("3494", "admins"), "abc:service-scope:3494:13",
            GroupsAndUsersCacheImpl.getAbcRoleGroupKey("3494", "users"), "abc:service-scope:3494:14",
            GroupsAndUsersCacheImpl.getAbcRoleGroupKey("1979", "admins"), "abc:service-scope:1979:1",
            GroupsAndUsersCacheImpl.getAbcRoleGroupKey("1979", "devops"), "abc:service-scope:1979:2"
        );

        assertEquals(
                Map.of(
                    "64443", "abc:service-scope:3494:13",
                    "158394", "abc:service-scope:1979:2"
                ),
                GroupsAndUsersCacheImpl.mapStaffToYpGroups(groups));
    }
}
