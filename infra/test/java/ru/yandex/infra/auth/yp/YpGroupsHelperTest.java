package ru.yandex.infra.auth.yp;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.yp.DummyYpTransactionClient;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.controller.testutil.FutureUtils.get5s;

class YpGroupsHelperTest {
    private DummyYpGroupsClient ypGroupsClient;
    private DummyYpTransactionClient ypTransactionClient;

    @BeforeEach
    void before() {
        ypGroupsClient = new DummyYpGroupsClient();
        ypTransactionClient = new DummyYpTransactionClient();
    }

    @Test
    void updateGroupMembers() {

        Set<String> logins = ImmutableSet.of("login1", "login2");
        String groupId = "groupId1";

        ypGroupsClient.existsResponse = completedFuture(true);
        ypGroupsClient.getMembersResponse = Map.of(groupId, ImmutableList.of("login1", "someOldLogin"));
        ypGroupsClient.createdGroup.add(groupId);
        ypGroupsClient.updateMembersResponse = completedFuture(null);

        get5s(ypTransactionClient.runWithTransaction(ypTransaction ->
                YpGroupsHelper.doAddMembersToGroup(ypGroupsClient, groupId, logins, ypTransaction, "sas-test", true))
        );

        assertThat(ypGroupsClient.lastCheckedGroup, equalTo(groupId));
        assertThat(ypGroupsClient.lastUpdatedGroup, equalTo(groupId));
        assertThat(ypGroupsClient.lastMembers, equalTo(logins));
        assertThat(ypTransactionClient.commitTransactionCallsCount, equalTo(1));
    }
}
