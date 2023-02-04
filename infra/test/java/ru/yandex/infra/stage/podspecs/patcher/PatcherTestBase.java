package ru.yandex.infra.stage.podspecs.patcher;

import java.util.function.Function;

import com.google.protobuf.Message;

import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TPodAgentSpec;

public abstract class PatcherTestBase<PatcherContextClass> {

    protected static TPodTemplateSpec.Builder createPodSpecBuilder() {
        return TPodTemplateSpec.newBuilder();
    }

    protected abstract Function<PatcherContextClass, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor();

    protected SpecPatcher<TPodTemplateSpec.Builder> createPatcher(PatcherContextClass context) {
        return getPatcherConstructor().apply(context);
    }

    public static class PatchResult {

        private final Message podTemplateSpec;
        private final YTreeNode labels;

        public PatchResult(Message podTemplateSpec, YTreeNode labels) {
            this.podTemplateSpec = podTemplateSpec;
            this.labels = labels;
        }

        public DataModel.TPodSpec getPodSpec() {
            TPodTemplateSpec podTemplateSpecN = (TPodTemplateSpec) podTemplateSpec;
            return podTemplateSpecN.getSpec();
        }

        public DataModel.TPodSpec.TPodAgentPayload getPodAgentPayload() {
            return getPodSpec().getPodAgentPayload();
        }

        public TPodAgentSpec getPodAgentSpec() {
            return getPodAgentPayload().getSpec();
        }

        public YTreeNode getLabels() {
            return labels;
        }
    }

    protected PatchResult patch(PatcherContextClass patcherContext, TPodTemplateSpec.Builder podTemplateSpecBuilder,
                                DeployUnitContext context) {
        var patcher = createPatcher(patcherContext);
        return patch(patcher, podTemplateSpecBuilder, context);
    }

    protected PatchResult patch(SpecPatcher<TPodTemplateSpec.Builder> patcher,
                                TPodTemplateSpec.Builder podTemplateSpecBuilder, DeployUnitContext context) {
        var labelsBuilder = new YTreeBuilder().beginMap();

        patcher.patch(podTemplateSpecBuilder, context, labelsBuilder);

        YTreeNode labels = labelsBuilder.endMap().build();

        return new PatchResult(podTemplateSpecBuilder.build(), labels);
    }
}
