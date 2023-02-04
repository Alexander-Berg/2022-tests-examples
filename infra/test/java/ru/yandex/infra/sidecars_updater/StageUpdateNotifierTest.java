package ru.yandex.infra.sidecars_updater;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.controller.dto.ProjectMeta;
import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.yp.DummyYpObjectRepository;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.sidecars_updater.staff.DummyStaffClient;
import ru.yandex.infra.sidecars_updater.startrek.DummyIssues;
import ru.yandex.infra.sidecars_updater.startrek.DummySession;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.yp.client.api.AccessControl;
import ru.yandex.yp.client.api.DataModel.TGroupSpec;
import ru.yandex.yp.client.api.DataModel.TGroupStatus;
import ru.yandex.yp.client.api.TProjectSpec;
import ru.yandex.yp.client.api.TProjectStatus;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StageUpdateNotifierTest {
    private static final String USER = "user";
    private static final String ROOT = "root";
    private static final String STAGE_ID = "stage-id";
    private static final String PROJECT_ID = "project-id";
    private static final String STARTREK_QUEUE_NAME = "project-id";

    DummySession session;
    DummyYpObjectRepository<SchemaMeta, TGroupSpec, TGroupStatus> groupRepository;
    DummyYpObjectRepository<ProjectMeta, TProjectSpec, TProjectStatus> projectRepository;
    DummyStaffClient staffClient;
    StageUpdateNotifier updateNotifier;

    YpObject<ProjectMeta, TProjectSpec, TProjectStatus> project;

    @BeforeEach
    void before() {
        session = new DummySession();
        session.issues = new DummyIssues();
        session.issues.createResult =
                new Issue("ISSUE-1", URI.create(""), "", "", 1, Cf.map(), session);
        groupRepository = new DummyYpObjectRepository<>();
        projectRepository = new DummyYpObjectRepository<>();
        staffClient = new DummyStaffClient();
        updateNotifier = new StageUpdateNotifier(projectRepository, groupRepository, session, staffClient,
                emptyList(), false, true, false, STARTREK_QUEUE_NAME);
    }

    @Test
    void updateNotifyResponsibleFromStageAcl() {
        AccessControl.TAccessControlEntry ace = createStageAce("RESPONSIBLE");
        String groupId = ace.getSubjects(0);
        groupRepository.getResponses.put(groupId, createGroupResponse(USER));

        projectRepository.getResponses.put(PROJECT_ID, completedFuture(Optional.of(createProject(emptyList(), ROOT))));
        StageMeta stageMeta = createStageMeta(List.of(ace));

        assertDoesNotThrow(() -> updateNotifier.updateNotify(stageMeta, emptyList()));

        assertThat(projectRepository.getObjectRequestIds, containsInAnyOrder(PROJECT_ID));
        assertThat(groupRepository.getObjectRequestIds, containsInAnyOrder(groupId));

        assertThat(session.issues.lastIssueCreate.getValues().getOrThrow("assignee"), equalTo(USER));
        assertThat(session.issues.lastIssueCreate.getComment().get().getSummonees(), equalTo(Cf.list(USER)));
        assertThat(session.issues.createCalls, equalTo(1));
    }

    @Test
    void updateNotifyResponsibleFromProjectAcl() {
        AccessControl.TAccessControlEntry stageAce = createStageAce("RESPONSIBLE");
        String stageGroupId = stageAce.getSubjects(0);
        groupRepository.getResponses.put(stageGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry projectAce = createProjectAce("RESPONSIBLE");
        String projectGroupId = projectAce.getSubjects(0);
        groupRepository.getResponses.put(projectGroupId, createGroupResponse(USER));

        StageMeta stageMeta = createStageMeta(List.of(stageAce));
        projectRepository.getResponses.put(PROJECT_ID,
                completedFuture(Optional.of(createProject(List.of(projectAce), ROOT))));

        assertDoesNotThrow(() -> updateNotifier.updateNotify(stageMeta, emptyList()));

        assertThat(projectRepository.getObjectRequestIds, containsInAnyOrder(PROJECT_ID));
        assertThat(groupRepository.getObjectRequestIds, containsInAnyOrder(stageGroupId, projectGroupId));

        assertThat(session.issues.lastIssueCreate.getValues().getOrThrow("assignee"), equalTo(USER));
        assertThat(session.issues.lastIssueCreate.getComment().get().getSummonees(), equalTo(Cf.list(USER)));
        assertThat(session.issues.createCalls, equalTo(1));
    }

    @Test
    void updateNotifyMaintainerFromStageAcl() {
        AccessControl.TAccessControlEntry stageResponsibleAce = createStageAce("RESPONSIBLE");
        String stageResponsibleGroupId = stageResponsibleAce.getSubjects(0);
        groupRepository.getResponses.put(stageResponsibleGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry projectAce = createProjectAce("RESPONSIBLE");
        String projectResponsibleGroupId = projectAce.getSubjects(0);
        groupRepository.getResponses.put(projectResponsibleGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry stageMaintainerAce = createStageAce("MAINTAINER");
        String stageMaintainerGroupId = stageMaintainerAce.getSubjects(0);
        groupRepository.getResponses.put(stageMaintainerGroupId, createGroupResponse(USER));

        StageMeta stageMeta = createStageMeta(List.of(stageMaintainerAce, stageResponsibleAce));
        projectRepository.getResponses.put(PROJECT_ID,
                completedFuture(Optional.of(createProject(List.of(projectAce), ROOT))));

        assertDoesNotThrow(() -> updateNotifier.updateNotify(stageMeta, emptyList()));

        assertThat(projectRepository.getObjectRequestIds, containsInAnyOrder(PROJECT_ID));

        assertThat(groupRepository.getObjectRequestIds,
                containsInAnyOrder(stageMaintainerGroupId, stageResponsibleGroupId, projectResponsibleGroupId));

        assertThat(session.issues.lastIssueCreate.getValues().getOrThrow("assignee"), equalTo(USER));
        assertThat(session.issues.lastIssueCreate.getComment().get().getSummonees(), equalTo(Cf.list(USER)));
        assertThat(session.issues.createCalls, equalTo(1));
    }

    @Test
    void updateNotifyOwnerFromProjectOwnerId() {
        AccessControl.TAccessControlEntry stageResponsibleAce = createStageAce("RESPONSIBLE");
        String stageResponsibleGroupId = stageResponsibleAce.getSubjects(0);
        groupRepository.getResponses.put(stageResponsibleGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry projectAce = createProjectAce("RESPONSIBLE");
        String projectResponsibleGroupId = projectAce.getSubjects(0);
        groupRepository.getResponses.put(projectResponsibleGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry stageMaintainerAce = createStageAce("MAINTAINER");
        String stageMaintainerGroupId = stageMaintainerAce.getSubjects(0);
        groupRepository.getResponses.put(stageMaintainerGroupId, createGroupResponse(ROOT));

        StageMeta stageMeta = createStageMeta(List.of(stageResponsibleAce, stageMaintainerAce));
        projectRepository.getResponses.put(PROJECT_ID,
                completedFuture(Optional.of(createProject(List.of(projectAce), USER))));

        assertDoesNotThrow(() -> updateNotifier.updateNotify(stageMeta, emptyList()));

        assertThat(projectRepository.getObjectRequestIds, containsInAnyOrder(PROJECT_ID));
        assertThat(groupRepository.getObjectRequestIds,
                containsInAnyOrder(stageMaintainerGroupId, stageResponsibleGroupId, projectResponsibleGroupId));

        assertThat(session.issues.lastIssueCreate.getValues().getOrThrow("assignee"), equalTo(USER));
        assertThat(session.issues.lastIssueCreate.getComment().get().getSummonees(), equalTo(Cf.list(USER)));
        assertThat(session.issues.createCalls, equalTo(1));
    }

    @Test
    void updateNotifyOwnerFromProjectAcl() {
        AccessControl.TAccessControlEntry stageResponsibleAce = createStageAce("RESPONSIBLE");
        String stageResponsibleGroupId = stageResponsibleAce.getSubjects(0);
        groupRepository.getResponses.put(stageResponsibleGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry projectResponsibleAce = createProjectAce("RESPONSIBLE");
        String projectResponsibleGroupId = projectResponsibleAce.getSubjects(0);
        groupRepository.getResponses.put(projectResponsibleGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry stageMaintainerAce = createStageAce("MAINTAINER");
        String stageMaintainerGroupId = stageMaintainerAce.getSubjects(0);
        groupRepository.getResponses.put(stageMaintainerGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry projectOwnerAce = createProjectAce("OWNER");
        String projectOwnerGroupId = projectOwnerAce.getSubjects(0);
        groupRepository.getResponses.put(projectOwnerGroupId, createGroupResponse(USER));

        StageMeta stageMeta = createStageMeta(List.of(stageResponsibleAce, stageMaintainerAce));
        projectRepository.getResponses.put(PROJECT_ID,
                completedFuture(Optional.of(createProject(List.of(projectOwnerAce, projectResponsibleAce), ROOT))));


        assertDoesNotThrow(() -> updateNotifier.updateNotify(stageMeta, emptyList()));

        assertThat(projectRepository.getObjectRequestIds, containsInAnyOrder(PROJECT_ID));
        assertThat(groupRepository.getObjectRequestIds, containsInAnyOrder(stageMaintainerGroupId, projectOwnerGroupId,
                stageResponsibleGroupId, projectResponsibleGroupId));

        assertThat(session.issues.lastIssueCreate.getValues().getOrThrow("assignee"), equalTo(USER));
        assertThat(session.issues.lastIssueCreate.getComment().get().getSummonees(), equalTo(Cf.list(USER)));
        assertThat(session.issues.createCalls, equalTo(1));
    }

    @Test
    void updateNotifyMaintainerFromProjectAcl() {
        AccessControl.TAccessControlEntry stageResponsibleAce = createStageAce("RESPONSIBLE");
        String stageResponsibleGroupId = stageResponsibleAce.getSubjects(0);
        groupRepository.getResponses.put(stageResponsibleGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry projectResponsibleAce = createProjectAce("RESPONSIBLE");
        String projectResponsibleGroupId = projectResponsibleAce.getSubjects(0);
        groupRepository.getResponses.put(projectResponsibleGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry stageMaintainerAce = createStageAce("MAINTAINER");
        String stageMaintainerGroupId = stageMaintainerAce.getSubjects(0);
        groupRepository.getResponses.put(stageMaintainerGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry projectOwnerAce = createProjectAce("OWNER");
        String projectOwnerGroupId = projectOwnerAce.getSubjects(0);
        groupRepository.getResponses.put(projectOwnerGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry projectMaintainerAce = createProjectAce("MAINTAINER");
        String projectMaintainerGroupId = projectMaintainerAce.getSubjects(0);
        groupRepository.getResponses.put(projectMaintainerGroupId, createGroupResponse(USER));

        StageMeta stageMeta = createStageMeta(List.of(stageMaintainerAce, stageResponsibleAce));
        projectRepository.getResponses.put(PROJECT_ID,
                completedFuture(Optional.of(
                        createProject(List.of(projectOwnerAce, projectMaintainerAce, projectResponsibleAce), ROOT))));

        assertDoesNotThrow(() -> updateNotifier.updateNotify(stageMeta, emptyList()));

        assertThat(projectRepository.getObjectRequestIds, containsInAnyOrder(PROJECT_ID));
        assertThat(groupRepository.getObjectRequestIds, containsInAnyOrder(
                projectOwnerGroupId, stageMaintainerGroupId, projectMaintainerGroupId, stageResponsibleGroupId,
                projectResponsibleGroupId));

        assertThat(session.issues.lastIssueCreate.getValues().getOrThrow("assignee"), equalTo(USER));
        assertThat(session.issues.lastIssueCreate.getComment().get().getSummonees(), equalTo(Cf.list(USER)));
        assertThat(session.issues.createCalls, equalTo(1));
    }

    @Test
    void updateNotifyUserNotFound() {
        AccessControl.TAccessControlEntry stageResponsibleAce = createStageAce("RESPONSIBLE");
        String stageResponsibleGroupId = stageResponsibleAce.getSubjects(0);
        groupRepository.getResponses.put(stageResponsibleGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry projectResponsibleAce = createProjectAce("RESPONSIBLE");
        String projectResponsibleGroupId = projectResponsibleAce.getSubjects(0);
        groupRepository.getResponses.put(projectResponsibleGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry stageMaintainerAce = createStageAce("MAINTAINER");
        String stageMaintainerGroupId = stageMaintainerAce.getSubjects(0);
        groupRepository.getResponses.put(stageMaintainerGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry projectOwnerAce = createProjectAce("OWNER");
        String projectOwnerGroupId = projectOwnerAce.getSubjects(0);
        groupRepository.getResponses.put(projectOwnerGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry projectMaintainerAce = createProjectAce("MAINTAINER");
        String projectMaintainerGroupId = projectMaintainerAce.getSubjects(0);
        groupRepository.getResponses.put(projectMaintainerGroupId, createGroupResponse(ROOT));

        StageMeta stageMeta = createStageMeta(List.of(stageMaintainerAce, stageResponsibleAce));
        projectRepository.getResponses.put(PROJECT_ID,
                completedFuture(Optional.of(
                        createProject(List.of(projectOwnerAce, projectMaintainerAce, projectResponsibleAce), ROOT))));

        assertThrows(StageUpdateNotifierError.class, () -> updateNotifier.updateNotify(stageMeta, emptyList()));

        assertThat(projectRepository.getObjectRequestIds, containsInAnyOrder(PROJECT_ID));
        assertThat(groupRepository.getObjectRequestIds, containsInAnyOrder(
                projectOwnerGroupId, stageMaintainerGroupId, projectMaintainerGroupId, stageResponsibleGroupId,
                projectResponsibleGroupId));

        assertThat(session.issues.lastIssueCreate, nullValue());
        assertThat(session.issues.createCalls, equalTo(0));
    }

    @Test
    void updateNotifyDryRunMode() {
        updateNotifier = new StageUpdateNotifier(projectRepository, groupRepository, session, staffClient,
                emptyList(), false, false, false, STARTREK_QUEUE_NAME);
        AccessControl.TAccessControlEntry ace = createStageAce("MAINTAINER");
        String groupId = ace.getSubjects(0);
        groupRepository.getResponses.put(groupId, createGroupResponse(USER));

        projectRepository.getResponses.put(PROJECT_ID, completedFuture(Optional.of(createProject(emptyList(), ROOT))));
        StageMeta stageMeta = createStageMeta(List.of(ace));

        assertDoesNotThrow(() -> updateNotifier.updateNotify(stageMeta, emptyList()));

        assertThat(projectRepository.getObjectRequestIds, containsInAnyOrder(PROJECT_ID));
        assertThat(groupRepository.getObjectRequestIds, containsInAnyOrder(groupId));
        assertThat(session.issues.lastIssueCreate, nullValue());
        assertThat(session.issues.createCalls, equalTo(0));
    }

    @Test
    void updateNotifyWithIgnoredLogins() {
        String ignoredUser = "another_user";
        updateNotifier = new StageUpdateNotifier(projectRepository, groupRepository, session, staffClient,
                List.of(ignoredUser), false, true, false, STARTREK_QUEUE_NAME);
        AccessControl.TAccessControlEntry ace = createStageAce("MAINTAINER");
        String groupId = ace.getSubjects(0);
        groupRepository.getResponses.put(groupId, createGroupResponse(List.of(ignoredUser, USER)));

        projectRepository.getResponses.put(PROJECT_ID, completedFuture(Optional.of(createProject(emptyList(), ROOT))));
        StageMeta stageMeta = createStageMeta(List.of(ace));

        assertDoesNotThrow(() -> updateNotifier.updateNotify(stageMeta, emptyList()));

        assertThat(projectRepository.getObjectRequestIds, containsInAnyOrder(PROJECT_ID));
        assertThat(groupRepository.getObjectRequestIds, containsInAnyOrder(groupId));
        assertThat(session.issues.lastIssueCreate.getValues().getOrThrow("assignee"), equalTo(USER));
        assertThat(session.issues.lastIssueCreate.getComment().get().getSummonees(), equalTo(Cf.list(USER)));
        assertThat(session.issues.createCalls, equalTo(1));
    }

    @Test
    void updateNotifyWithExtraSummonees() {
        updateNotifier = new StageUpdateNotifier(projectRepository, groupRepository, session, staffClient,
                emptyList(), true, true, false, STARTREK_QUEUE_NAME);
        AccessControl.TAccessControlEntry ace = createStageAce("MAINTAINER");
        String groupId = ace.getSubjects(0);
        String anotherUser = "another_user";
        groupRepository.getResponses.put(groupId, createGroupResponse(List.of(anotherUser, USER)));

        projectRepository.getResponses.put(PROJECT_ID, completedFuture(Optional.of(createProject(emptyList(), ROOT))));
        StageMeta stageMeta = createStageMeta(List.of(ace));

        assertDoesNotThrow(() -> updateNotifier.updateNotify(stageMeta, emptyList()));

        assertThat(projectRepository.getObjectRequestIds, containsInAnyOrder(PROJECT_ID));
        assertThat(groupRepository.getObjectRequestIds, containsInAnyOrder(groupId));
        assertThat(session.issues.lastIssueCreate.getValues().getOrThrow("assignee"), equalTo(anotherUser));
        assertThat(session.issues.lastIssueCreate.getComment().get().getSummonees(), equalTo(Cf.list(anotherUser, USER)));
        assertThat(session.issues.createCalls, equalTo(1));
    }

    @Test
    void updateNotifyWhenProjectDoesNotExistOrErrorWhileParsing() {
        AccessControl.TAccessControlEntry stageAce = createStageAce("RESPONSIBLE");
        String stageGroupId = stageAce.getSubjects(0);
        groupRepository.getResponses.put(stageGroupId, createGroupResponse(ROOT));

        AccessControl.TAccessControlEntry projectAce = createProjectAce("OWNER");
        String projectGroupId = projectAce.getSubjects(0);
        groupRepository.getResponses.put(projectGroupId, createGroupResponse(USER));

        StageMeta stageMeta = createStageMeta(List.of(stageAce));
        projectRepository.getResponses.put(PROJECT_ID, completedFuture(Optional.empty()));

        assertThrows(StageUpdateNotifierError.class, () -> updateNotifier.updateNotify(stageMeta, emptyList()));

        assertThat(projectRepository.getObjectRequestIds, containsInAnyOrder(PROJECT_ID));
        assertThat(groupRepository.getObjectRequestIds, containsInAnyOrder(stageGroupId));
        assertThat(session.issues.lastIssueCreate, nullValue());
        assertThat(session.issues.createCalls, equalTo(0));

        projectRepository.getResponses.put(PROJECT_ID, failedFuture(new RuntimeException()));

        assertThrows(StageUpdateNotifierError.class, () -> updateNotifier.updateNotify(stageMeta, emptyList()));

        assertThat(projectRepository.getObjectRequestIds, containsInAnyOrder(PROJECT_ID, PROJECT_ID));
        assertThat(groupRepository.getObjectRequestIds, containsInAnyOrder(stageGroupId, stageGroupId));
        assertThat(session.issues.lastIssueCreate, nullValue());
        assertThat(session.issues.createCalls, equalTo(0));
    }


    static StageMeta createStageMeta(List<AccessControl.TAccessControlEntry> acl) {
        return new StageMeta(STAGE_ID, new Acl(acl), "", "", 0, PROJECT_ID);
    }

    static YpObject<ProjectMeta, TProjectSpec, TProjectStatus> createProject(
            List<AccessControl.TAccessControlEntry> acl, String ownerId) {
        return new YpObject.Builder<ProjectMeta, TProjectSpec, TProjectStatus>()
                .setMeta(new ProjectMeta(PROJECT_ID, new Acl(acl), "", "", 0, ownerId))
                .build();
    }

    static AccessControl.TAccessControlEntry createStageAce(String role) {
        return createACE(format("deploy:%s.%s.%s", PROJECT_ID, STAGE_ID, role));
    }

    static AccessControl.TAccessControlEntry createProjectAce(String role) {
        return createACE(format("deploy:%s.%s", PROJECT_ID, role));
    }

    static AccessControl.TAccessControlEntry createACE(String subjectId) {
        return AccessControl.TAccessControlEntry.newBuilder()
                .addSubjects(subjectId)
                .addPermissions(AccessControl.EAccessControlPermission.ACA_WRITE)
                .setAction(AccessControl.EAccessControlAction.ACA_ALLOW)
                .build();
    }

    static CompletableFuture<Optional<YpObject<SchemaMeta, TGroupSpec, TGroupStatus>>> createGroupResponse(
            String member) {
        return createGroupResponse(List.of(member));
    }

    static CompletableFuture<Optional<YpObject<SchemaMeta, TGroupSpec, TGroupStatus>>> createGroupResponse(
            List<String> members) {
        return completedFuture(Optional.of(
                new YpObject.Builder<SchemaMeta, TGroupSpec, TGroupStatus>()
                        .setSpecAndTimestamp(
                                TGroupSpec.newBuilder().addAllMembers(members).build(), 1).build()));
    }
}
