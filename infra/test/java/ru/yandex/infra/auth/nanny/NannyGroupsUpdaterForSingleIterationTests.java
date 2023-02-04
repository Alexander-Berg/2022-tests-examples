package ru.yandex.infra.auth.nanny;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.infra.auth.yp.DummyYpGroupsClient;
import ru.yandex.infra.auth.yp.YpClients;
import ru.yandex.infra.auth.yp.YpGroup;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.infra.controller.testutil.FutureUtils.get1s;

public class NannyGroupsUpdaterForSingleIterationTests {

    private Set<String> ypUsers;
    private Map<String, String> staffToYpGroup;
    private DummyYpGroupsClient groupsClient;
    private YpClients ypClients;

    private final long STAFF_ID_1 = 158394L;
    private final long STAFF_ID_2 = 64444L;
    private final String YP_GROUP_ID = "abc:service-scope:3494:13";

    @BeforeEach
    void before() {

        staffToYpGroup = Map.of(
            "1", "staff:department:123",
            "10", "staff:department:256",
            "333", "abc:service:3494",
            "111", "abc:service:1979",
            String.valueOf(STAFF_ID_2), YP_GROUP_ID,
            String.valueOf(STAFF_ID_1), "abc:service-scope:1979:2"
        );

        ypUsers = Set.of("user1", "user2", "user3");

        groupsClient = new DummyYpGroupsClient();

        ypClients = new YpClients(null, groupsClient);
    }

    @Test
    void loadNannyGroupsTest() {
        final YpGroup group = new YpGroup("nanny:taxi-processing-testing.owners:222802e3-960c62dc-6100e087-b29652fa",
                emptySet(), Map.of(
                "service", "nanny_service1",
                "role", "owner",
                "some label", "some value"
        ));
        final YpGroup group2 = new YpGroup("group name",
                emptySet(), Map.of(
                "service", "service2",
                "role", "conf_managers"
        ));

        Map<String, YpGroup> ypGroups = Map.of(
                group.getId(), group,
                group2.getId(), group2,
                "id", new YpGroup("group with wrong labels", emptySet(), emptyMap()),
                "id2", new YpGroup("nanny:service.owners:222802e3-960c62dc-6100e087-b29652fa", emptySet(), Map.of(
                        "service", "service"
                ))
        );

        groupsClient.getGroupsByLabelsResponse = CompletableFuture.completedFuture(ypGroups);
        var updater = new NannyGroupsUpdaterForSingleIteration("test", Collections.emptyMap(), ypClients, staffToYpGroup, ypUsers);
        var nannyGroups = get1s(updater.loadNannyGroups());
        assertThat(nannyGroups.size(), equalTo(2));
        assertThat(nannyGroups.get(Tuple2.tuple("nanny_service1", "owner")), equalTo(group));
        assertThat(nannyGroups.get(Tuple2.tuple("service2", "conf_managers")), equalTo(group2));
    }

    @Test
    void syncTest() {
        final YpGroup group = new YpGroup("nanny:taxi-processing-testing.owners:222802e3-960c62dc-6100e087-b29652fa",
                Set.of(
                        "user1",
                        String.valueOf(STAFF_ID_1),
                        "wrong_group"
                ), Map.of(
                "service", "nanny_service1",
                "role", "owner",
                "some label", "some value"
        ));
        groupsClient.getGroupsByLabelsResponse = CompletableFuture.completedFuture(Map.of(group.getId(), group));
        groupsClient.createdGroup.add("nanny:taxi-processing-testing.owners:222802e3-960c62dc-6100e087-b29652fa");
        groupsClient.updateMembersResponse = CompletableFuture.completedFuture(null);

        var updater = new NannyGroupsUpdaterForSingleIteration("test", Collections.emptyMap(), ypClients, staffToYpGroup, ypUsers);
        var nannyGroups = get1s(updater.loadNannyGroups());
        assertThat(nannyGroups.size(), equalTo(1));

        var info = new NannyServiceInfo("nanny_service1", Map.of(
                "owner", new NannyAuthGroup(
                        Set.of("user1", "user2", "missed_user"),
                        Set.of(String.valueOf(STAFF_ID_2), "30303030")),
                "missed should be skipped by update", new NannyAuthGroup(
                        Set.of("user1", "user2"),
                        Set.of(String.valueOf(STAFF_ID_2)))
        ), null, null, null, 0);

        updater.syncNannyService(info);
        assertEquals("nanny:taxi-processing-testing.owners:222802e3-960c62dc-6100e087-b29652fa", groupsClient.lastUpdatedGroup);
        assertThat(groupsClient.currentResponseCounter, equalTo(1));
        assertThat(groupsClient.lastMembers, equalTo(Set.of("user1", "user2", YP_GROUP_ID)));
    }
}
