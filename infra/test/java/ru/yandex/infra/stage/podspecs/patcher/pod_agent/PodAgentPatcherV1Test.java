package ru.yandex.infra.stage.podspecs.patcher.pod_agent;

import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.DeployUnitSpecDetails;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TPodTemplateSpec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PodAgentPatcherV1Test {
    @Test
    public void patchAllFields() {
        PodAgentPatcherV1 podAgentPatcherV1 = new PodAgentPatcherV1();
        DataModel.TPodSpec.Builder newBuilder = DataModel.TPodSpec.newBuilder();
        newBuilder
                .getResourceRequestsBuilder()
                .setVcpuLimit(1000)
                .setVcpuGuarantee(2000)
                .setMemoryLimit(3000)
                .setMemoryGuarantee(4000)
                .setAnonymousMemoryLimit(5000)
                .setThreadLimit(6);

        DeployUnitContext deployUnitContext = mock(DeployUnitContext.class);
        DeployUnitSpec deployUnitSpec = mock(DeployUnitSpec.class);
        DeployUnitSpecDetails deployUnitSpecDetails = mock(DeployUnitSpecDetails.class);
        when(deployUnitSpecDetails.getPodSpec()).thenReturn(newBuilder.build());
        when(deployUnitSpec.getDetails()).thenReturn(deployUnitSpecDetails);
        when(deployUnitContext.getSpec()).thenReturn(deployUnitSpec);

        TPodTemplateSpec.Builder podTemplateSpec = TPodTemplateSpec.newBuilder();
        podAgentPatcherV1.patch(podTemplateSpec, deployUnitContext, new YTreeBuilder());

        DataModel.TPodSpec.TPodAgentContainerResources computeResources =
                podTemplateSpec.getSpec().getPodAgentPayload().getMeta().getComputeResources();

        DataModel.TPodSpec.TResourceRequests resourceRequests = newBuilder.getResourceRequests();

        assertThat(computeResources.getVcpuLimit(), equalTo(resourceRequests.getVcpuLimit()));
        assertThat(computeResources.getVcpuGuarantee(), equalTo(resourceRequests.getVcpuGuarantee()));
        assertThat(computeResources.getMemoryLimit(), equalTo(resourceRequests.getMemoryLimit()));
        assertThat(computeResources.getMemoryGuarantee(), equalTo(resourceRequests.getMemoryGuarantee()));
        assertThat(computeResources.getAnonymousMemoryLimit(), equalTo(resourceRequests.getAnonymousMemoryLimit()));
        assertThat(computeResources.getThreadLimit(), equalTo(resourceRequests.getThreadLimit()));
    }
}
