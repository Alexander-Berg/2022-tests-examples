package ru.yandex.infra.auth.idm.api;

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

class IdmApiTest extends LocalHttpServerBasedTest {
    private DummyServlet servlet;
    private static String requestSystem = "test";
    private static int requestNodesOffset = 0;
    private static int requestNodesLimit = 6;

    @Override
    protected Map<String, HttpServlet> getServlets() {
        servlet = new DummyServlet(ResourceUtils.readResource("idm_role_nodes_response.json"));
        return ImmutableMap.of("/api/v1/rolenodes/", servlet);
    }

    @Test
    void getRoleNodesFromServer() {
        String token = "token";
        IdmApi idmApi = new IdmApiImpl(getClient(), getUrl(), token, requestSystem, GaugeRegistry.EMPTY);

        final RoleNodesResponse response = FutureUtils.get5s(idmApi.getRoleNodesByOffset(requestNodesOffset, requestNodesLimit));

        assertThat(response.getOffset(), equalTo(0));
        assertThat(response.getNext(), equalTo("next"));
        assertThat(response.getTotalCount(), equalTo(3));

        List<RoleNodeInfo> nodes = response.getNodes();
        assertThat(nodes, hasSize(3));
        assertThat(nodes.get(0).getId(), equalTo(123L));
        assertThat(nodes.get(1).getId(), equalTo(200L));
        assertThat(nodes.get(2).getId(), equalTo(201L));

        for (RoleNodeInfo node : nodes) {
            assertThat(node.getHelp().getEnName(), equalTo("helpEn"));
            assertThat(node.getSlug(), equalTo("slug"));
            assertThat(node.getValuePath(), equalTo("value_path"));
            assertThat(node.getParentPath(), equalTo("parent_path"));
            assertThat(node.getUniqueId(), equalTo("1fb67fc4-a9fae67d-ba50d255-c343665d"));
            assert (!node.isKey());
        }
    }
}
