package ru.yandex.infra.controller.yp;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.protobuf.Message;

import ru.yandex.infra.controller.testutil.FutureUtils;
import ru.yandex.infra.controller.util.YsonUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeProtoUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yp.YpRawObjectService;
import ru.yandex.yp.model.YpGetStatement;
import ru.yandex.yp.model.YpObjectType;
import ru.yandex.yp.model.YpPayloadFormat;
import ru.yandex.yp.model.YpSelectStatement;
import ru.yandex.yp.model.YpTypedId;
import ru.yandex.yp.model.YpTypedObject;

import static ru.yandex.infra.controller.util.YsonUtils.payloadToYson;

public class TestYpRepositoryUtils {

    static public YTreeMapNode getLabelsYson(YpRawObjectService ypClient, String id, YpObjectType objectType) {
        return getObjectsYson(ypClient, id, objectType, Paths.LABELS);
    }

    static public YTreeMapNode getAnnotationsYson(YpRawObjectService ypClient, String id, YpObjectType objectType) {
        return getObjectsYson(ypClient, id, objectType, Paths.ANNOTATIONS);
    }

    static public YTreeMapNode getObjectsYson(YpRawObjectService ypClient, String id, YpObjectType objectType, String selector) {
        YpGetStatement.Builder builder = YpGetStatement.ysonBuilder(new YpTypedId(id, objectType));
        builder.addSelector(selector);
        return FutureUtils.get5s(ypClient.getObject(builder.build(), payloads -> payloadToYson(payloads.get(0))))
                .mapNode();
    }

    static public <Spec> Spec getSpec(YpRawObjectService ypClient, String id, Message.Builder specBuilder, YpObjectType objectType) {
        YpGetStatement.Builder builder = YpGetStatement.ysonBuilder(new YpTypedId(id, objectType));
        builder.addSelector(Paths.SPEC);
        return FutureUtils.get5s(ypClient.getObject(builder.build(),
                payloads -> (Spec) YTreeProtoUtils.unmarshal(payloadToYson(payloads.get(0)), specBuilder)));
    }

    static public void cleanup(YpRawObjectService ypClient, YpObjectType objectType) throws Exception {
        cleanup(ypClient, objectType, Collections.emptySet());
    }

    static public void cleanup(YpRawObjectService ypClient, YpObjectType objectType, Set<String> excludedIds) throws Exception {
        YpSelectStatement.Builder statement = YpSelectStatement.builder(objectType, YpPayloadFormat.YSON);
        statement.addSelector(Paths.ID);
        ypClient.selectObjects(statement.build(), payloads -> payloadToYson(payloads.get(0)).stringValue())
                .thenCompose(ids -> CompletableFuture.allOf(
                        ids.getResults().stream()
                                .filter(id -> !excludedIds.contains(id))
                                .map(id -> ypClient.removeObject(new YpTypedId(id, objectType)))
                                .collect(Collectors.toList()).toArray(new CompletableFuture[]{})
                )).get(10, TimeUnit.SECONDS);
    }

    static public <Spec extends Message> void createReplicaSet(YpRawObjectService ypClient, String id, Spec spec, Map<String, ?> labels) {
        create(ypClient, YpObjectType.REPLICA_SET, id, spec, labels, Collections.emptyMap(), Collections.emptyMap());
    }

    static public <Spec extends Message> void create(YpRawObjectService ypClient,
                                                     YpObjectType ypObjectType,
                                                     String id,
                                                     Spec spec,
                                                     Map<String, ?> labels,
                                                     Map<String, ?> annotations,
                                                     Map<String, ?> additionalMetaKeys) {
        YTreeBuilder builder = YTree.mapBuilder()
                .key("meta").beginMap()
                    .key("id").value(id);
        additionalMetaKeys.forEach((key, value) -> builder.key(key).value(value));
        builder.endMap()
                .key("spec").value(YTreeProtoUtils.marshal(spec, true))
                .key("labels").beginMap();
        labels.forEach((key, value) -> builder.key(key).value(value));
        builder.endMap();
        builder.key("annotations").beginMap();
        annotations.forEach((key, value) -> builder.key(key).value(value));
        YTreeNode node = builder.endMap().buildMap();
        FutureUtils.get5s(ypClient.createObject(new YpTypedObject(ypObjectType, YsonUtils.toYsonPayload(node))));
    }
}
