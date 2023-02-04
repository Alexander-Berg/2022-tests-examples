package ru.yandex.infra.auth.yp;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.Message;

import ru.yandex.infra.controller.dto.SchemaMeta;
import ru.yandex.infra.controller.util.YsonUtils;
import ru.yandex.infra.controller.yp.CreateObjectRequest;
import ru.yandex.infra.controller.yp.LabelBasedRepository;
import ru.yandex.infra.controller.yp.Paths;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeProtoUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.yp.YpRawObjectService;
import ru.yandex.yp.client.api.AccessControl;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.model.YpGetStatement;
import ru.yandex.yp.model.YpObjectType;
import ru.yandex.yp.model.YpPayloadFormat;
import ru.yandex.yp.model.YpSelectStatement;
import ru.yandex.yp.model.YpTypedId;
import ru.yandex.yp.model.YpTypedObject;

import static java.util.Collections.emptyList;
import static ru.yandex.infra.controller.testutil.FutureUtils.get5s;
import static ru.yandex.infra.controller.util.YsonUtils.payloadToYson;
import static ru.yandex.yp.model.YpObjectType.ACCOUNT;
import static ru.yandex.yp.model.YpObjectType.APPROVAL_POLICY;
import static ru.yandex.yp.model.YpObjectType.GROUP;
import static ru.yandex.yp.model.YpObjectType.PROJECT;
import static ru.yandex.yp.model.YpObjectType.STAGE;
import static ru.yandex.yp.model.YpObjectType.USER;

public class YpServiceTestUtils {
    static public void createGroup(LabelBasedRepository<SchemaMeta, DataModel.TGroupSpec, DataModel.TGroupStatus> groupRepository,
            String id, Set<String> members, Map<String, Object> labels) {
        get5s(groupRepository.createObject(id, new CreateObjectRequest.Builder<>(
                DataModel.TGroupSpec.newBuilder()
                        .addAllMembers(members)
                        .build())
                .setLabels(labels)
                .build()));
    }

    static public void createGroup(YpRawObjectService ypClient, String id, Set<String> members,
            Map<String, Object> labels) {
        YTreeBuilder builder = YTree.mapBuilder()
                .key("meta").beginMap()
                    .key("id").value(id)
                .endMap()
                .key("spec").beginMap()
                    .key("members").beginList();
        members.forEach(builder::value);
        builder.endList().endMap();

        builder.key("labels").beginMap();
        labels.forEach((key, value) -> builder.key(key).value(value));
        builder.endMap();

        get5s(ypClient.createObject(new YpTypedObject(GROUP, YsonUtils.toYsonPayload(builder.buildMap()))));
    }

    static public void createStage(YpRawObjectService ypClient, String id, String project) {
        get5s(ypClient.createObject(new YpTypedObject(STAGE, YsonUtils.toYsonPayload(YTree.mapBuilder()
                .key("meta").beginMap()
                    .key("id").value(id)
                    .key("project_id").value(project)
                .endMap()
                .key("spec").beginMap()
                    .key("account_id").value("tmp")
                .endMap().buildMap()))));
    }

    static public void createApprovalPolicy(YpRawObjectService ypClient, String id, String stage) {
        get5s(ypClient.createObject(new YpTypedObject(APPROVAL_POLICY, YsonUtils.toYsonPayload(YTree.mapBuilder()
                .key("meta").beginMap()
                    .key("id").value(id)
                    .key("stage_id").value(stage)
                .endMap()
                .key("spec").beginMap()
                    .key("multiple_approval").beginMap()
                        .key("approvals_count").value(1)
                    .endMap()
                .endMap().buildMap()))));
    }

    static public void createProject(YpRawObjectService ypClient, String id) {
        createProject(ypClient, id, emptyList());
    }

    static public void createProject(YpRawObjectService ypClient, String id, List<String> specificBoxTypes) {
        YTreeBuilder builder = YTree.mapBuilder()
                .key("meta").beginMap()
                    .key("id").value(id)
                .endMap()
                .key("spec").beginMap()
                    .key("account_id").value("tmp")
                    .key("user_specific_box_types").beginList();
        specificBoxTypes.forEach(builder::value);
        builder.endList().endMap();
        get5s(ypClient.createObject(new YpTypedObject(PROJECT, YsonUtils.toYsonPayload(builder.buildMap()))));
    }

    static public void createUser(YpRawObjectService ypClient, String id) {
        get5s(ypClient.createObject(new YpTypedObject(USER, YsonUtils.toYsonPayload(YTree.mapBuilder()
                .key("meta").beginMap()
                    .key("id").value(id)
                .endMap().buildMap()))));
    }

    static public void createAccount(YpRawObjectService ypClient, String id) {
        get5s(ypClient.createObject(new YpTypedObject(ACCOUNT, YsonUtils.toYsonPayload(YTree.mapBuilder()
                .key("meta").beginMap()
                    .key("id").value(id)
                .endMap().buildMap()))));
    }

    static public <Meta> Meta getMeta(YpRawObjectService ypClient, Message.Builder metaBuilder, String id, YpObjectType objectType) {
        YpGetStatement.Builder builder = YpGetStatement.ysonBuilder(new YpTypedId(id, objectType));
        builder.addSelector(Paths.META);
        return get5s(ypClient.getObject(builder.build(),
                payloads -> (Meta) YTreeProtoUtils.unmarshal(payloadToYson(payloads.get(0)), metaBuilder)));
    }

    static public AccessControl.TAccessControlEntry generateACE(String subject,
            Iterable<AccessControl.EAccessControlPermission> permissions) {
        return YpUtils.generateACE(subject, permissions, emptyList());
    }

    static public boolean isExist(YpRawObjectService ypClient, String id, YpObjectType objectType) {
        return get5s(ypClient.selectObjects(YpSelectStatement.builder(objectType, YpPayloadFormat.YSON)
                .setFilter(String.format("[/meta/id]='%s'", id)).addSelector("/meta/id").build(),
                payloads -> payloadToYson(payloads.get(0)).stringValue())).getResults().size() > 0;
    }
}
