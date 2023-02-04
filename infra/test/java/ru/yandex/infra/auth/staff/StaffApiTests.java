package ru.yandex.infra.auth.staff;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.metrics.GaugeRegistry;
import ru.yandex.infra.controller.testutil.DummyServlet;
import ru.yandex.infra.controller.testutil.FutureUtils;
import ru.yandex.infra.controller.testutil.LocalHttpServerBasedTest;
import ru.yandex.infra.controller.util.ResourceUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

class StaffApiTests extends LocalHttpServerBasedTest {
    private DummyServlet servlet;

    @Override
    protected Map<String, HttpServlet> getServlets() {
        servlet = new DummyServlet(ResourceUtils.readResource("staff_groups_response.json"));
        return ImmutableMap.of("/v3/groups", servlet);
    }

    @Test
    void groupsParsingTest() {
        StaffApiImpl api = new StaffApiImpl(getClient(), getUrl(), "token", 100, GaugeRegistry.EMPTY);

        List<StaffGroup> groups = FutureUtils.get5s(api.getAllAbcServiceGroups());

        assertThat(groups, hasSize(3));

        StaffGroup department = groups.get(0);
        assertThat(department.getId(), equalTo(5L));
        assertThat(department.getUrl(), equalTo("ext"));
        assertThat(department.getDepartment().getId(), equalTo("2"));

        StaffGroup service = groups.get(1);
        assertThat(service.getId(), equalTo(391L));
        assertThat(service.getAbcService().getId(), equalTo("2"));

        StaffGroup scope = groups.get(2);
        assertThat(scope.getId(), equalTo(129706L));
        assertThat(scope.getRoleScope(), equalTo("robots_management"));
        assertThat(scope.getParent().getAbcService().getId(), equalTo("1979"));
    }
}
