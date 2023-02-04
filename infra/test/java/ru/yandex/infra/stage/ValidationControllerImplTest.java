package ru.yandex.infra.stage;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Try;
import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.stage.deployunit.DeployUnitStats;
import ru.yandex.infra.stage.dto.Condition;
import ru.yandex.infra.stage.dto.StageSpec;
import ru.yandex.infra.stage.util.SettableClock;
import ru.yandex.infra.stage.yp.DeployObjectId;
import ru.yandex.infra.stage.yp.Retainment;
import ru.yandex.yp.client.api.AccessControl;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static ru.yandex.infra.stage.TestData.META_TIMESTAMP;
import static ru.yandex.infra.stage.TestData.PROJECT;
import static ru.yandex.infra.stage.TestData.PROJECT_ID;
import static ru.yandex.infra.stage.TestData.PROTO_STAGE_SPEC;
import static ru.yandex.infra.stage.TestData.STAGE_ACL;
import static ru.yandex.infra.stage.TestData.createACLEntry;
import static ru.yandex.infra.stage.TestData.createProjectWithAcl;
import static ru.yandex.infra.stage.TestData.createStageWithAcl;
import static ru.yandex.infra.stage.TestData.createStageWithAclAndProject;
import static ru.yandex.infra.stage.util.CustomMatchers.isTrue;
import static ru.yandex.yp.client.api.AccessControl.EAccessControlPermission.ACA_WRITE;

class ValidationControllerImplTest {
    private static final String STAGE_ID = TestData.DEFAULT_STAGE_ID;

    private static final TStageSpec INVALID_STAGE_SPEC = TStageSpec.newBuilder()
            .setAccountId("account; DROP TABLE stages")
            .build();
    private static final TStageStatus NON_EMPTY_STATUS = TStageStatus.newBuilder()
            .putDeployUnits(TestData.DEPLOY_UNIT_ID, TestData.CONVERTER.toProto(TestData.DEPLOY_UNIT_STATUS))
            .build();
    private static final Try<YpObject<StageMeta, TStageSpec, TStageStatus>> NON_EMPTY_STAGE = Try.success(
            new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                .setSpecAndTimestamp(PROTO_STAGE_SPEC, 1)
                .setStatus(NON_EMPTY_STATUS)
                .setMeta(TestData.STAGE.get().getMeta())
                .build());
    private static final Try<YpObject<StageMeta, TStageSpec, TStageStatus>> INVALID_STAGE = Try.success(
            new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                .setSpecAndTimestamp(INVALID_STAGE_SPEC, 2)
                .setStatus(TestData.STAGE.get().getStatus())
                .setMeta(TestData.STAGE.get().getMeta())
                .build());
    private static final Clock CLOCK = Clock.systemDefaultZone();
    private static final String VALIDATION_FAILURE_MESSAGE = "Invalid account";

    private static final StageValidator VALIDATOR = (spec, stageId, disabledClusters) -> {
        // ValidationController requires a mutable list
        // TODO: accept it as argument
        List<String> result = new ArrayList<>();
        if (spec.getAccountId().equals(INVALID_STAGE_SPEC.getAccountId())) {
            result.add(VALIDATION_FAILURE_MESSAGE);
        }
        return result;
    };

    private DummyStageStatusSender dummyStatusSender;
    private DummyStageController dummyStageController;
    private ValidationController controller;
    private final AclFilter aclFilter = new AclPrefixFilter("deploy:");

    @BeforeEach
    void init() {
        dummyStatusSender = new DummyStageStatusSender();
        dummyStageController = new DummyStageController();
        controller = new ValidationControllerImpl((a, b) -> dummyStageController, STAGE_ID, TestData.CONVERTER, VALIDATOR,
                CLOCK, dummyStatusSender, aclFilter, GlobalContext.EMPTY);
    }

    @Test
    void invalidStageIdTest() {
        String badId = "inv*";
        controller = new ValidationControllerImpl((a, b) -> dummyStageController, badId, TestData.CONVERTER, VALIDATOR,
                CLOCK, dummyStatusSender, aclFilter, GlobalContext.EMPTY);
        controller.sync(TestData.STAGE, Optional.of(PROJECT));
        controller.updateStatus();
        String expectedMessage = String.format("Stage id '%s' must match regexp '[A-Za-z0-9-_]+'", badId);
        verifyConditionFalse(dummyStatusSender.lastUpdatedStatus.getValidated(), "VALIDATION_FAILED", expectedMessage);
        assertThat(controller.getValidConditionType(), equalTo(ValidationController.ValidityType.INVALID));
    }

    @Test
    void invalidSpecRestoreTest() {
        controller.sync(INVALID_STAGE, Optional.of(PROJECT));
        controller.updateStatus();
        verifyConditionFalse(dummyStatusSender.lastUpdatedStatus.getValidated(), "VALIDATION_FAILED", VALIDATION_FAILURE_MESSAGE);
        assertThat(controller.getValidConditionType(), equalTo(ValidationController.ValidityType.INVALID));
        assertThat(dummyStageController.wasSynced, equalTo(false));

        controller.addStats(new DeployUnitStats.Builder());
        assertThat(dummyStageController.wasAddedMetric, equalTo(false));
    }

    @Test
    void updateInvalidSpecToValidTest() {
        controller.sync(INVALID_STAGE, Optional.of(PROJECT));
        controller.sync(Try.success(generateSpecAndStatus((int)INVALID_STAGE.get().getSpecTimestamp() + 1,
                NON_EMPTY_STATUS)), Optional.of(PROJECT));
        assertThat(dummyStageController.wasSynced, equalTo(true));
    }

    @Test
    void parseStageFailTest() {
        controller.sync(Try.failure(new RuntimeException("error")), Optional.empty());
        controller.updateStatus();
        assertThat(dummyStatusSender.lastUpdatedStatus.getValidated(), not(isTrue()));
        assertThat(dummyStatusSender.lastUpdatedStatus.getValidated().getReason(), equalTo("STAGE_SPEC_PARSING_FAILED"));
    }

    @Test
    void parseProjectFailTest() {
        controller.sync(TestData.STAGE, Optional.empty());
        controller.updateStatus();
        assertThat(dummyStatusSender.lastUpdatedStatus.getValidated(), not(isTrue()));
        assertThat(dummyStatusSender.lastUpdatedStatus.getValidated().getReason(), equalTo("PROJECT_SPEC_PARSING_FAILED"));

        controller.sync(TestData.STAGE, Optional.of(Try.failure(new RuntimeException("error"))));
        controller.updateStatus();
        assertThat(dummyStatusSender.lastUpdatedStatus.getValidated(), not(isTrue()));
        assertThat(dummyStatusSender.lastUpdatedStatus.getValidated().getReason(), equalTo("PROJECT_SPEC_PARSING_FAILED"));
    }

    @Test
    void removeFromSenderTest() {
        controller.sync(TestData.STAGE, Optional.of(PROJECT));
        controller.shutdown();
        assertThat(dummyStatusSender.lastRemovedId, equalTo(STAGE_ID));
    }

    @Test
    void restoreTestWhenProjectIsValid() {
        controller.sync(NON_EMPTY_STAGE, Optional.of(PROJECT));
        assertThat(dummyStageController.wasRestored, equalTo(true));
        assertThat(dummyStageController.wasSynced, equalTo(true));
    }

    @Test
    void syncStageControllerTest() {
        controller.sync(TestData.STAGE, Optional.of(PROJECT));
        assertThat(dummyStageController.wasSynced, equalTo(true));
    }

    @Test
    void notLoseStatusForNotValidSpec() {
        // STAGE_STATUS.getDeployUnits() isn't empty
        TStageStatus notEmptyStatus = TestData.CONVERTER.toProto(TestData.STAGE_STATUS);
        controller.sync(
                Try.success(new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                        .setSpecAndTimestamp(INVALID_STAGE_SPEC, 1)
                        .setStatus(notEmptyStatus)
                        .setMeta(TestData.STAGE.get().getMeta())
                        .build())
                , Optional.of(PROJECT));
        assertThat(dummyStageController.wasRestored, equalTo(true));
    }

    @Test
    void shouldRetainOnInvalidSpecTest() {
        controller.sync(NON_EMPTY_STAGE, Optional.empty());
        controller.sync(INVALID_STAGE, Optional.empty());

        String expectedRetainmentReason = String.format("Stage '%s' current spec is invalid", STAGE_ID);
        assertThat(controller.shouldRetain(new DeployObjectId(STAGE_ID, TestData.DEPLOY_UNIT_ID),
                TestData.CLUSTER_AND_TYPE), equalTo(new Retainment(true, expectedRetainmentReason)));
    }

    @Test
    void shouldDelegateForValidSpec() {
        controller.sync(NON_EMPTY_STAGE, Optional.of(PROJECT));
        assertThat(controller.shouldRetain(new DeployObjectId(STAGE_ID, TestData.DEPLOY_UNIT_ID),
                TestData.CLUSTER_AND_TYPE), equalTo(TestData.RETAINMENT));
    }

    @Test
    void validationFailOnPrimitiveTypeChange() {
        long timestamp = NON_EMPTY_STAGE.get().getSpecTimestamp();
        controller.sync(NON_EMPTY_STAGE, Optional.of(PROJECT));
        dummyStageController.wasSynced = false;
        StageMeta newMeta = new StageMeta(TestData.STAGE.get().getMeta().getId(), Acl.EMPTY, "", "", 0, TestData.STAGE.get().getMeta().getProjectId());
        controller.sync(Try.success(
                new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                        .setSpecAndTimestamp(
                                TestData.CONVERTER.toProto(new StageSpec(
                                        ImmutableMap.of(TestData.DEPLOY_UNIT_ID,
                                                TestData.DEPLOY_UNIT_SPEC.withDetails(TestData.MCRS_UNIT_SPEC)),
                                        "account-id", 2, false, emptyMap(), emptyMap())),
                                timestamp + 2)
                        .setStatus(NON_EMPTY_STAGE.get().getStatus())
                        .setMeta(newMeta)
                        .setMetaTimestamp(timestamp + 1)
                        .build())
                , Optional.of(PROJECT));
        assertThat(controller.getValidConditionType(), equalTo(ValidationController.ValidityType.INVALID));
        assertThat(dummyStageController.wasSynced, equalTo(false));
    }

    @Test
    void conditionTimestampTest() {
        SettableClock clock = new SettableClock();
        controller = new ValidationControllerImpl((a, b) -> dummyStageController, STAGE_ID, TestData.CONVERTER, VALIDATOR,
                clock, dummyStatusSender, aclFilter, GlobalContext.EMPTY);
        Instant firstReadyTimestamp = clock.instant();
        controller.sync(Try.success(generateSpecAndStatus(1, TStageStatus.getDefaultInstance())), Optional.empty());

        clock.incrementSecond();
        controller.sync(Try.success(generateSpecAndStatus(2, TStageStatus.getDefaultInstance())), Optional.empty());
        controller.updateStatus();
        assertThat(dummyStatusSender.lastUpdatedStatus.getValidated().getTimestamp(), equalTo(firstReadyTimestamp));
    }

    private void verifyConditionFalse(Condition condition, String reason, String message) {
        assertThat(condition.getStatus(), equalTo(Condition.Status.FALSE));
        assertThat(condition.getReason(), equalTo(reason));
        assertThat(condition.getMessage(), containsString(message));
    }

        private YpObject<StageMeta, TStageSpec, TStageStatus> generateSpecAndStatus(int iteration, TStageStatus status) {
        return new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                .setSpecAndTimestamp(generateSpec(iteration), iteration)
                .setStatus(status)
                .setMeta(TestData.STAGE.get().getMeta())
                .build();
    }

    private TStageSpec generateSpec(int revision) {
        return PROTO_STAGE_SPEC.toBuilder()
                .setRevision(revision)
                .build();
    }

    @Test
    void testProjectExtractionWhenMetaExist() {
        controller.sync(TestData.STAGE, Optional.of(PROJECT));
        assertThat(dummyStageController.lastProjectId, equalTo(TestData.STAGE.get().getMeta().getProjectId()));
    }

    @Test
    void testProjectExtractionWhenMetaDoesNotExist() {
        controller.sync(Try.success(
                new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                        .setSpecAndTimestamp(PROTO_STAGE_SPEC, 4)
                        .setStatus(TStageStatus.getDefaultInstance())
                        .setMeta(new StageMeta("id", STAGE_ACL, "", "", 0, ""))
                        .build())
                , Optional.of(PROJECT));
        assertThat(dummyStageController.lastProjectId, equalTo("UNKNOWN"));
    }

    @Test
    void testUseAccountIdFromProject() {
        TStageSpec specWithoutAccount = PROTO_STAGE_SPEC.toBuilder().clearAccountId().build();
        Try<YpObject<StageMeta, TStageSpec, TStageStatus>> stage =
                Try.success(new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                        .setSpecAndTimestamp(specWithoutAccount, 1)
                        .setStatus(TStageStatus.getDefaultInstance())
                        .setMeta(new StageMeta("id", STAGE_ACL, "", "", 0, PROJECT_ID))
                        .setMetaTimestamp(META_TIMESTAMP)
                        .build());

        controller = new ValidationControllerImpl((a, b) -> dummyStageController, STAGE_ID, TestData.CONVERTER, VALIDATOR,
                CLOCK, dummyStatusSender, aclFilter, GlobalContext.EMPTY);
        controller.sync(stage, Optional.of(PROJECT));
        assertThat(dummyStageController.lastSpec.getAccountId(), equalTo(PROJECT.get().getSpec().getAccountId()));
        assertThat(dummyStageController.wasSynced, equalTo(true));
    }

    @Test
    void testNotAppendAclFromProjectWithoutDeployPrefix() {
        controller.sync(TestData.STAGE, Optional.of(PROJECT));
        assertThat(dummyStageController.wasSynced, equalTo(true));

        assertThat(PROJECT.get().getMeta().getAcl().getEntries().get(0).getSubjects(0), equalTo("user"));
        assertThat(dummyStageController.lastAcl, equalTo(TestData.STAGE.get().getMeta().getAcl()));
    }

    @Test
    void testNotAppendAclFromProjectWhichExistsInStage() {
        List<AccessControl.TAccessControlEntry> stageEntries = new ArrayList<>(STAGE_ACL.getEntries());
        stageEntries.add(createACLEntry("deploy:project.OWNER", ACA_WRITE));

        List<AccessControl.TAccessControlEntry> projectEntries = ImmutableList.of(
                createACLEntry("deploy:project.OWNER", ACA_WRITE));

        controller.sync(createStageWithAcl(new Acl(stageEntries)),
                Optional.of(createProjectWithAcl(PROJECT_ID, new Acl(projectEntries))));

        assertThat(dummyStageController.wasSynced, equalTo(true));
        assertThat(dummyStageController.lastAcl.getEntries(), equalTo(stageEntries));
    }

    @Test
    void testAppendAclFromProject() {
        List<AccessControl.TAccessControlEntry> projectEntries = ImmutableList.of(
                createACLEntry(List.of("some_user", "deploy:project.OWNER", "some_group"), ACA_WRITE));
        List<AccessControl.TAccessControlEntry> projectWithoutUserSubjectsEntries = ImmutableList.of(
                createACLEntry("deploy:project.OWNER", ACA_WRITE));

        List<AccessControl.TAccessControlEntry> unionEntries = new ArrayList<>(STAGE_ACL.getEntries());
        unionEntries.addAll(projectWithoutUserSubjectsEntries);

        controller.sync(TestData.STAGE, Optional.of(createProjectWithAcl(PROJECT_ID, new Acl(projectEntries))));

        assertThat(dummyStageController.wasSynced, equalTo(true));
        assertThat(dummyStageController.lastAcl.getEntries(), equalTo(unionEntries));
    }

    @Test
    void testRevisionAndTimestampUpdatedOnFirstSync() {
        controller.sync(TestData.STAGE, Optional.of(PROJECT));
        controller.updateStatus();

        assertThat(dummyStatusSender.lastUpdatedStatus.getRevision(), not(equalTo(0)));
        assertThat(dummyStatusSender.lastUpdatedStatus.getSpecTimestamp(), not(equalTo(0)));
    }

    @Test
    void multipleSyncCallsShouldNotSendStatusUpdate() {
        assertThat(dummyStatusSender.updatesCount, equalTo(0));

        var v1 = Try.success(generateSpecAndStatus(1, TStageStatus.getDefaultInstance()));
        controller.sync(v1, Optional.of(PROJECT));
        assertThat(dummyStatusSender.updatesCount, equalTo(0));
        controller.updateStatus();
        assertThat(dummyStatusSender.updatesCount, equalTo(1));

        controller.sync(v1, Optional.of(PROJECT));
        controller.sync(v1, Optional.of(PROJECT));
        controller.sync(v1, Optional.of(PROJECT));
        assertThat(dummyStatusSender.updatesCount, equalTo(1));
        controller.updateStatus();
        assertThat(dummyStatusSender.updatesCount, equalTo(1));

        var v2 = Try.success(generateSpecAndStatus(2, TStageStatus.getDefaultInstance()));
        controller.sync(v2, Optional.of(PROJECT));
        controller.sync(v2, Optional.of(PROJECT));
        assertThat(dummyStatusSender.updatesCount, equalTo(1));
        controller.updateStatus();
        assertThat(dummyStatusSender.updatesCount, equalTo(2));
    }

    @Test
    void testRemoveAclsFromOldProject() {
        List<AccessControl.TAccessControlEntry> projectEntries = ImmutableList.of(
                createACLEntry(List.of(
                        "some_user",
                        "deploy:" + PROJECT_ID + ".OWNER",
                        "deploy:" + PROJECT_ID + ".SYSTEM_DEVELOPER",
                        "some_group"), ACA_WRITE));
        List<AccessControl.TAccessControlEntry> projectEntriesWithoutUserSubjects = ImmutableList.of(
                createACLEntry(List.of("deploy:" + PROJECT_ID + ".OWNER", "deploy:" + PROJECT_ID + ".SYSTEM_DEVELOPER"),
                        ACA_WRITE));

        controller.sync(TestData.STAGE, Optional.of(createProjectWithAcl(PROJECT_ID, new Acl(projectEntries))));

        List<AccessControl.TAccessControlEntry> unionEntries = new ArrayList<>(STAGE_ACL.getEntries());
        unionEntries.addAll(projectEntriesWithoutUserSubjects);

        assertThat(dummyStageController.wasSynced, equalTo(true));
        assertThat(dummyStageController.lastAcl.getEntries(), equalTo(unionEntries));

        String newProjectId = "new_project";
        List<AccessControl.TAccessControlEntry> newProjectEntries = ImmutableList.of(
                createACLEntry(List.of("some_user", "deploy:" + newProjectId + ".OWNER"), ACA_WRITE));
        List<AccessControl.TAccessControlEntry> newProjectEntriesWithoutUserSubjects = ImmutableList.of(
                createACLEntry("deploy:" + newProjectId + ".OWNER", ACA_WRITE));

        controller.sync(createStageWithAclAndProject(new Acl(unionEntries), newProjectId),
                Optional.of(createProjectWithAcl(newProjectId, new Acl(newProjectEntries))));

        List<AccessControl.TAccessControlEntry> newUnionEntries = new ArrayList<>(STAGE_ACL.getEntries());
        // Without last project entries
        newUnionEntries.addAll(newProjectEntriesWithoutUserSubjects);

        assertThat(dummyStageController.wasSynced, equalTo(true));
        assertThat(dummyStageController.lastAcl.getEntries(), equalTo(newUnionEntries));
    }

    @Test
    void testRemoveAclsFromOldProjectWhenUserAddHisSubject() {
        List<AccessControl.TAccessControlEntry> projectEntries = ImmutableList.of(
                createACLEntry(List.of("some_user", "deploy:" + PROJECT_ID + ".OWNER", "some_group"), ACA_WRITE));
        List<AccessControl.TAccessControlEntry> projectEntriesWithoutDeploySubjects = ImmutableList.of(
                createACLEntry(List.of("some_user", "some_group"), ACA_WRITE));

        controller.sync(TestData.STAGE, Optional.of(createProjectWithAcl(PROJECT_ID, new Acl(projectEntries))));

        List<AccessControl.TAccessControlEntry> updatedByUserStageEntries = new ArrayList<>(STAGE_ACL.getEntries());
        updatedByUserStageEntries.addAll(projectEntries);

        String newProjectId = "new_project";
        List<AccessControl.TAccessControlEntry> newProjectEntries = ImmutableList.of(
                createACLEntry(List.of("some_user", "deploy:" + newProjectId + ".OWNER"), ACA_WRITE));
        List<AccessControl.TAccessControlEntry> newProjectEntriesWithoutUserSubjects = ImmutableList.of(
                createACLEntry("deploy:" + newProjectId + ".OWNER", ACA_WRITE));

        // Situation when user added his specific subjects
        controller.sync(createStageWithAclAndProject(new Acl(updatedByUserStageEntries), newProjectId),
                Optional.of(createProjectWithAcl(newProjectId, new Acl(newProjectEntries))));

        List<AccessControl.TAccessControlEntry> newUnionEntries = new ArrayList<>(STAGE_ACL.getEntries());
        // Without last project subjects with prefix 'deploy:' but users subjects were not removed
        newUnionEntries.addAll(projectEntriesWithoutDeploySubjects);
        newUnionEntries.addAll(newProjectEntriesWithoutUserSubjects);

        assertThat(dummyStageController.wasSynced, equalTo(true));
        assertThat(dummyStageController.lastAcl.getEntries(), equalTo(newUnionEntries));
    }

    @Test
    void testInheritProjectAclToStage() {
        List<AccessControl.TAccessControlEntry> projectEntries = ImmutableList.of(
                createACLEntry(List.of("some_user", "deploy:project.OWNER", "some_group"), ACA_WRITE));
        List<AccessControl.TAccessControlEntry> projectWithoutUserSubjectsEntries = ImmutableList.of(
                createACLEntry("deploy:project.OWNER", ACA_WRITE));

        List<AccessControl.TAccessControlEntry> unionEntries = new ArrayList<>(STAGE_ACL.getEntries());
        unionEntries.addAll(projectWithoutUserSubjectsEntries);

        controller.sync(TestData.STAGE, Optional.of(createProjectWithAcl(PROJECT_ID, new Acl(projectEntries))));

        assertThat(dummyStageController.wasSynced, equalTo(true));
        assertThat(dummyStageController.lastAcl.getEntries(), equalTo(unionEntries));
    }

    @Test
    void dontSendTheSameStatusTwice() {
        var builder = new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                .setSpecAndTimestamp(PROTO_STAGE_SPEC, 1)
                .setStatus(NON_EMPTY_STATUS)
                .setMeta(TestData.STAGE.get().getMeta());

        controller.sync(Try.success(builder.build()), Optional.of(PROJECT));
        assertThat(dummyStatusSender.updatesCount, equalTo(0));
        controller.updateStatus();
        controller.updateStatus();
        controller.updateStatus();
        assertThat(dummyStatusSender.updatesCount, equalTo(1));

        // Update spec revision
        builder = builder.setSpecAndTimestamp(PROTO_STAGE_SPEC.toBuilder()
                .setRevision(2234)
                .build(), 1);

        controller.sync(Try.success(builder.build()), Optional.of(PROJECT));
        controller.updateStatus();
        assertThat(dummyStatusSender.updatesCount, equalTo(2));
    }

}
