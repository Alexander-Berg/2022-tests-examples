package ru.yandex.infra.auth.servlets;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Try;
import ru.yandex.infra.auth.Role;
import ru.yandex.infra.auth.yp.DummyYpGroupsClient;
import ru.yandex.infra.auth.yp.DummyYpServiceImpl;
import ru.yandex.infra.auth.yp.YpClients;
import ru.yandex.infra.controller.dto.Acl;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.yp.DummyYpObjectRepository;
import ru.yandex.infra.controller.yp.SelectedObjects;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.misc.web.servlet.mock.MockHttpServletRequest;
import ru.yandex.misc.web.servlet.mock.MockHttpServletResponse;
import ru.yandex.yp.client.api.AccessControl;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AclServletTests {

    private DummyYpServiceImpl ypService;
    private DummyYpGroupsClient groupsClient;
    private DummyYpObjectRepository<StageMeta, TStageSpec, TStageStatus> stageRepository;
    private AclServlet servlet;

    @BeforeEach
    void before() {
        ypService = new DummyYpServiceImpl();
        groupsClient = new DummyYpGroupsClient();
        stageRepository = new DummyYpObjectRepository<>();
        ypService.ypClients = new YpClients(null, groupsClient, null, stageRepository, null, null, null);
        servlet = new AclServlet(ypService);
    }

    private static Try<YpObject<StageMeta, TStageSpec, TStageStatus>> createStageObject(String stageId, String projectId, Acl acl) {
        YpObject<StageMeta, TStageSpec, TStageStatus> ypObject = new YpObject.Builder<StageMeta, TStageSpec, TStageStatus>()
                .setSpecAndTimestamp(TStageSpec.newBuilder().setAccountId("accountId").build(), 0)
                .setMeta(new StageMeta(stageId, acl, "fqid...", "f47a0ca5-a9e02a53-1c314d8b-32f825e1", 0, projectId))
                .build();
        return Try.success(ypObject);
    }

    @Test
    void wrongSubjectsTest() throws UnsupportedEncodingException {

        ypService.roles = ImmutableSet.of(
            new Role("project.stage", "MAINTAINER"),
            new Role("project.stage", "DEVELOPER"),
            new Role("project.stage", "DEPLOYER"),
            new Role("project.stage2", "DEVELOPER")
        );
        ypService.systemName = "testsystem";
        groupsClient.getAllIdsResponse = CompletableFuture.completedFuture(ImmutableSet.of(
                "somegroup",
                "testsystem:project.stage.MAINTAINER",
                "testsystem:project.stage.APPROVER",
                "testsystem:project.stage2.DEVELOPER"
        ));
        stageRepository.selectResponse = CompletableFuture.completedFuture(new SelectedObjects<>(ImmutableMap.of(
                "stage", createStageObject("stage", "project", new Acl(List.of(
                        AccessControl.TAccessControlEntry.newBuilder()
                                .setAction(AccessControl.EAccessControlAction.ACA_ALLOW)
                                .addAllSubjects(List.of(
                                        "should_be_ignored",
                                        "testsystem:project.stage.MAINTAINER",//should be ok
                                        "testsystem:wrongproject.stage.MAINTAINER",
                                        "testsystem:project.wrongstage.MAINTAINER",
                                        "testsystem:project.stage.APPROVER",//missed role
                                        "testsystem:project.stage.DEPLOYER"//missed yp group
                                ))
                                .build()))),
                "stage2", createStageObject("stage2", "project", new Acl(List.of(
                        AccessControl.TAccessControlEntry.newBuilder()
                                .setAction(AccessControl.EAccessControlAction.ACA_ALLOW)
                                .addAllSubjects(List.of(
                                        "should_be_ignored",
                                        "testsystem:project.stage2.DEVELOPER"//should be ok
                                ))
                                .build())))

                ), 0L)
        );

        var req = new MockHttpServletRequest("GET", "/admin/check-acls");
        var response = new MockHttpServletResponse();
        servlet.doGet(req, response);

        String expectedResult =
                "project = project, stage = stage, YP group = testsystem:wrongproject.stage.MAINTAINER, fail reason = missed yp group\n" +
                "project = project, stage = stage, YP group = testsystem:wrongproject.stage.MAINTAINER, fail reason = wrong project name in ACE\n" +
                "project = project, stage = stage, YP group = testsystem:wrongproject.stage.MAINTAINER, fail reason = role was not found\n" +

                "project = project, stage = stage, YP group = testsystem:project.wrongstage.MAINTAINER, fail reason = missed yp group\n" +
                "project = project, stage = stage, YP group = testsystem:project.wrongstage.MAINTAINER, fail reason = wrong stage name in ACE\n" +
                "project = project, stage = stage, YP group = testsystem:project.wrongstage.MAINTAINER, fail reason = role was not found\n" +

                "project = project, stage = stage, YP group = testsystem:project.stage.APPROVER, fail reason = role was not found\n" +
                "project = project, stage = stage, YP group = testsystem:project.stage.DEPLOYER, fail reason = missed yp group\n" +
                "Checked 2 stages, 1 with wrong acl's, 0 fixed\n";
        compareStringsIgnoringOrder(expectedResult, response.getContentAsString());
    }

    private void compareStringsIgnoringOrder(String expected, String actual) {
        Set<String> expectedSet = Set.of(expected.replace("\r", "").split("\n"));
        Set<String> actualSet = Set.of(actual.replace("\r", "").split("\n"));
        assertEquals(expectedSet, actualSet);
    }
}
