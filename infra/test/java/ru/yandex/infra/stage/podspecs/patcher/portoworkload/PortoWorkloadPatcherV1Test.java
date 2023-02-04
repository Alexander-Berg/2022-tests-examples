package ru.yandex.infra.stage.podspecs.patcher.portoworkload;

import java.util.function.Function;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.infra.stage.podspecs.patcher.PatcherTestBase;
import ru.yandex.infra.stage.podspecs.patcher.monitoring.MonitoringPatcherUtils;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TWorkload;

import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.buildMutableWorkload;
import static ru.yandex.infra.stage.util.AssertUtils.assertCollectionMatched;

public class PortoWorkloadPatcherV1Test extends PatcherTestBase<PortoWorkloadPatcherV1Context> {

    private static final PortoWorkloadPatcherV1Context DEFAULT_PATCHER_CONTEXT = new PortoWorkloadPatcherV1Context();

    @ParameterizedTest
    @CsvSource({
            "tvm_workload, true",
            "logbroker_push_agent_workload, true",
            "box1_w1__dru, true",
            "box1-juggler-workload, true",
            "juggler-workload, false",
            "workload1, false"
    })
    void testAddWorkloadsToMonitoring(String workloadId, Boolean shouldAdd) {
        var spec = TPodTemplateSpec.newBuilder();
        addWorkload(spec, workloadId);

        var deployUnitContext = DEFAULT_UNIT_CONTEXT.withDeployUnitSpec(
                DeployUnitSpec::withPortoWorkloadMetrics, true
        );

        var res = patch(DEFAULT_PATCHER_CONTEXT, spec, deployUnitContext);

        assertCollectionMatched(res.getPodSpec().getHostInfra().getMonitoring().getWorkloadsList(),
                shouldAdd ? 1 : 0,
                w -> w.getInheritMissedLabels() == shouldAdd
        );

        assertCollectionMatched(res.getPodSpec().getHostInfra().getMonitoring().getWorkloadsList(),
                shouldAdd ? 1 : 0,
                w -> {
                    var pattern = PortoWorkloadPatcherV1.INFRA_SIDECARS.keySet().stream()
                            .filter(p -> p.matcher(w.getWorkloadId()).matches())
                            .findFirst()
                            .get();
                    return w.getLabelsMap().get(MonitoringPatcherUtils.ITYPE_KEY).equals(
                            PortoWorkloadPatcherV1.PREFIX + "_" + PortoWorkloadPatcherV1.INFRA_SIDECARS.get(pattern)
                    );
                }
        );
    }

    private static void addWorkload(TPodTemplateSpec.Builder podTemplate, String workloadId) {
        var agentSpec = podTemplate.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder();
        agentSpec.addWorkloads(TWorkload.newBuilder().setId(workloadId).build());
        agentSpec.addMutableWorkloads(buildMutableWorkload(workloadId));
    }

    @Override
    protected Function<PortoWorkloadPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return PortoWorkloadPatcherV1::new;
    }
}
