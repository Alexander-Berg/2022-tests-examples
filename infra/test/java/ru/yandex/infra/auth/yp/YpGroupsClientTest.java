package ru.yandex.infra.auth.yp;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Try;
import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.yp.DummyYpObjectTransactionalRepository;
import ru.yandex.infra.controller.yp.SelectedObjects;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.yp.client.api.DataModel.TGroupSpec;
import ru.yandex.yp.client.api.DataModel.TGroupStatus;
import ru.yandex.yp.model.YpTransaction;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class YpGroupsClientTest {
    private static final String GROUP_ID = "idm:1234";
    private static final Set<String> MEMBERS = Set.of("login1", "login2", "login3");

    private DummyYpObjectTransactionalRepository<SchemaMeta, TGroupSpec, TGroupStatus> ypClient;
    private YpTransaction ypTransaction;
    private YpGroupsClient ypGroupsClient;

    @BeforeEach
    void before() {
        ypClient = new DummyYpObjectTransactionalRepository<>();
        ypGroupsClient = new YpGroupsClientImpl(ypClient, "robot", 1024);
        ypTransaction = new YpTransaction("id", 0, 0);
    }

    @Test
    void existsObjectTest() throws Exception {
        YpObject<SchemaMeta, TGroupSpec, TGroupStatus> object = new YpObject.Builder<SchemaMeta, TGroupSpec, TGroupStatus>()
                .setSpecAndTimestamp(TGroupSpec.newBuilder().addAllMembers(MEMBERS).build(), 0L)
                .build();
        ypClient.getResponses.put(GROUP_ID, CompletableFuture.completedFuture(Optional.of(object)));

        assertTrue(ypGroupsClient.exists(GROUP_ID, ypTransaction).get());
    }

    @Test
    void getNotExistObjectTest() throws Exception {
        ypClient.getResponses.put(GROUP_ID, CompletableFuture.completedFuture(Optional.empty()));

        assertFalse(ypGroupsClient.exists(GROUP_ID, ypTransaction).get());
    }

    @Test
    void addGroupTest() {
        ypClient.createResponse = CompletableFuture.completedFuture(null);
        assertDoesNotThrow(() -> ypGroupsClient.addGroup(GROUP_ID, MEMBERS, ypTransaction, emptyMap()).get());

        assertThat(ypClient.createdIds, contains(GROUP_ID));
    }

    @Test
    void removeGroupTest() {
        ypClient.removeResponse = CompletableFuture.completedFuture(0);
        assertDoesNotThrow(() -> ypGroupsClient.removeGroup(GROUP_ID, null).get());

        assertThat(ypClient.removedIds, contains(GROUP_ID));
    }

    @Test
    void addMembersTest() {
        ypClient.updateResponse = CompletableFuture.completedFuture(0);
        ypClient.getResponses.put(GROUP_ID, CompletableFuture.completedFuture(Optional.of(
                new YpObject.Builder<SchemaMeta, TGroupSpec, TGroupStatus>()
                        .setSpecAndTimestamp(TGroupSpec.newBuilder().build(), 0L)
                        .build())));

        assertDoesNotThrow(() -> ypGroupsClient.addMembers(GROUP_ID, MEMBERS, ypTransaction).get());

        assertThat(ypClient.updatedIds, contains(GROUP_ID));
        assertThat(ypClient.lastUpdateRequest.getSpec(), optionalWithValue());
        assertThat(Set.copyOf(ypClient.lastUpdateRequest.getSpec().get().getMembersList()), equalTo(MEMBERS));
    }

    @Test
    void addMembersForNotExistObjectTest() {
        ypClient.updateResponse = CompletableFuture.completedFuture(0);
        ypClient.getResponses.put(GROUP_ID, CompletableFuture.completedFuture(Optional.empty()));

        Assertions.assertThrows(Exception.class, () -> ypGroupsClient.addMembers(GROUP_ID, MEMBERS, ypTransaction).get());

        assertThat(ypClient.updatedIds, not(contains(GROUP_ID)));
        assertThat(ypClient.lastUpdateRequest.getSpec(), emptyOptional());
    }

    @Test
    void addMembersWhenSomeAlreadyExistTest() {
        ypClient.updateResponse = CompletableFuture.completedFuture(0);
        ypClient.getResponses.put(GROUP_ID, CompletableFuture.completedFuture(Optional.of(
                new YpObject.Builder<SchemaMeta, TGroupSpec, TGroupStatus>()
                        .setSpecAndTimestamp(TGroupSpec.newBuilder().addMembers("login1").build(), 0L)
                        .build())));

        assertDoesNotThrow(() -> ypGroupsClient.addMembers(GROUP_ID, MEMBERS, ypTransaction).get());

        assertThat(ypClient.updatedIds, contains(GROUP_ID));
        assertThat(ypClient.lastUpdateRequest.getSpec(), optionalWithValue());
        assertThat(Set.copyOf(ypClient.lastUpdateRequest.getSpec().get().getMembersList()), equalTo(Set.copyOf(MEMBERS)));
    }

    @Test
    void removeMembersTest() {
        ypClient.updateResponse = CompletableFuture.completedFuture(0);
        ypClient.getResponses.put(GROUP_ID, CompletableFuture.completedFuture(Optional.of(
                new YpObject.Builder<SchemaMeta, TGroupSpec, TGroupStatus>()
                        .setSpecAndTimestamp(TGroupSpec.newBuilder().addAllMembers(MEMBERS).build(), 0L)
                        .build())));


        assertDoesNotThrow(() -> ypGroupsClient.removeMembers(GROUP_ID, Set.of("login2"), ypTransaction).get());

        assertThat(ypClient.updatedIds, contains(GROUP_ID));
        assertThat(ypClient.lastUpdateRequest.getSpec(), optionalWithValue());
        assertThat(Set.copyOf(ypClient.lastUpdateRequest.getSpec().get().getMembersList()), containsInAnyOrder("login1", "login3"));
    }

    @Test
    void removeMembersForNotExistObjectTest() {
        ypClient.updateResponse = CompletableFuture.completedFuture(0);
        ypClient.getResponses.put(GROUP_ID, CompletableFuture.completedFuture(Optional.empty()));

        Assertions.assertThrows(Exception.class, () -> ypGroupsClient.removeMembers(GROUP_ID, MEMBERS, ypTransaction).get());

        assertThat(ypClient.updatedIds, not(contains(GROUP_ID)));
        assertThat(ypClient.lastUpdateRequest.getSpec(), emptyOptional());
    }

    @Test
    void removeMembersWhenSomeAlreadyNotExistTest() {
        ypClient.updateResponse = CompletableFuture.completedFuture(0);
        ypClient.getResponses.put(GROUP_ID, CompletableFuture.completedFuture(Optional.of(
                new YpObject.Builder<SchemaMeta, TGroupSpec, TGroupStatus>()
                        .setSpecAndTimestamp(TGroupSpec.newBuilder().addAllMembers(Set.of("login1", "login4")).build(), 0L)
                        .build())));

        assertDoesNotThrow(() -> ypGroupsClient.removeMembers(GROUP_ID, MEMBERS, ypTransaction).get());

        assertThat(ypClient.updatedIds, contains(GROUP_ID));
        assertThat(ypClient.lastUpdateRequest.getSpec(), optionalWithValue());
        assertThat(ypClient.lastUpdateRequest.getSpec().get().getMembersList(), containsInAnyOrder("login4"));
    }

    @Test
    void getGroupsWithPrefixTest() throws ExecutionException, InterruptedException {
        ypClient.selectResponse = CompletableFuture.completedFuture(new SelectedObjects<>(ImmutableMap.of(
                "idm:12", Try.success(createGroup(List.of("member1", "member2"))),
                "abc:34", Try.success(createGroup(List.of("member3"))
        )), 0L));
        Map<String, Set<String>> roleSubjects = ypGroupsClient.getGroupsWithPrefix("idm").get();

        assertThat(roleSubjects, hasKey("idm:12"));
        assertThat(roleSubjects.get("idm:12"), containsInAnyOrder("member1", "member2"));
        assertThat(roleSubjects, not(hasKey("abc:34")));
    }

    static YpObject<SchemaMeta, TGroupSpec, TGroupStatus> createGroup(List<String> members) {
        return new YpObject.Builder<SchemaMeta, TGroupSpec, TGroupStatus>()
                .setSpecAndTimestamp(TGroupSpec.newBuilder()
                        .addAllMembers(members)
                        .build(), 0)
                .build();
    }
}
