package ru.yandex.infra.stage.podspecs.patcher.docker;

import java.util.List;
import java.util.function.Function;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TEnvVar;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;

public class DockerPatcherV3Test extends DockerPatcherV2Test {
    @Override
    protected Function<DockerPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return DockerPatcherV3::new;
    }

    @Override
    protected void checkWorkloadVariable(TEnvVar expectedVar, List<TEnvVar> workloadVars) {
        //We already put all env variables from docker image into Box,
        //No reason to override them in workload again
        assertThat(expectedVar, not(in(workloadVars)));
    }
}
