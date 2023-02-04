package ru.yandex.infra.auth.servlets;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.infra.auth.yp.DummyYpGroupsClient;
import ru.yandex.infra.auth.yp.DummyYpServiceImpl;
import ru.yandex.infra.auth.yp.YpClients;
import ru.yandex.misc.web.servlet.mock.MockHttpServletRequest;
import ru.yandex.misc.web.servlet.mock.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GroupsGCServletTests {

    private DummyYpServiceImpl ypService;
    private DummyYpGroupsClient groupsClient;
    private DummyYpGroupsClient slaveGroupsClient;
    private GroupsGCServlet servlet;

    @BeforeEach
    void before() {
        ypService = new DummyYpServiceImpl();
        groupsClient = new DummyYpGroupsClient();
        slaveGroupsClient = new DummyYpGroupsClient();
        ypService.ypClients = new YpClients(null, groupsClient, null, null, null, null, null);
        ypService.ypSlaveClients = Map.of("man-pre", new YpClients(null, slaveGroupsClient, null, null, null, null, null));
        servlet = new GroupsGCServlet(ypService);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void getGroupsMissedOnSlaveClusterTest(boolean fix) throws IOException {

        ypService.systemName = "testsystem";
        groupsClient.getGroupsWithPrefixResponse = CompletableFuture.completedFuture(Stream.of(
                "group1",
                "groupOnlyOnMaster",
                "group2",
                "groupOnlyOnMaster2"
        ).collect(Collectors.toMap(g -> g, g -> Collections.emptySet())));
        slaveGroupsClient.getGroupsWithPrefixResponse = CompletableFuture.completedFuture(Stream.of(
                "onlyOnSlave",
                "group2",
                "onlyOnSlave2",
                "group1"
        ).collect(Collectors.toMap(g -> g, g -> Collections.emptySet())));

        var req = new MockHttpServletRequest(fix ? "POST" : "GET", "/admin/groups-gc");
        req.addParameter("slave-cluster", "man-pre");
        var response = new MockHttpServletResponse();
        servlet.doGet(req, response);

        String expectedResult = "onlyOnSlave\nonlyOnSlave2\n";
        assertEquals(expectedResult, response.getContentAsString());

        if (fix) {
            slaveGroupsClient.removeGroupsResponse = CompletableFuture.completedFuture(null);
            req.addParameter("fix", "true");
            servlet.doPost(req, response);
            assertEquals(List.of("onlyOnSlave", "onlyOnSlave2"), slaveGroupsClient.lastRemovedGroups);
        }
    }

    @Test
    void removeMasterGroupsTest() throws IOException {
        ypService.garbageGroups = Set.of("group1", "group2");
        var req = new MockHttpServletRequest("GET", "/admin/groups-gc");
        var response = new MockHttpServletResponse();
        servlet.doGet(req, response);

        String expectedResult = "group1\ngroup2\n";
        assertEquals(expectedResult, response.getContentAsString());
    }
}
