package ru.yandex.infra.stage.podspecs.patcher.default_anon_limit;

import java.util.OptionalLong;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.infra.stage.podspecs.patcher.PatcherTestBase;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TPodTemplateSpec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;

public abstract class AnonLimitPatcherBaseTest extends PatcherTestBase<AnonLimitPatcherContext> {

    @Override
    protected Function<AnonLimitPatcherContext, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return AnonLimitPatcherV1::new;
    }

    protected void anonymousMemoryLimitScenario(long podMemoryLimit, OptionalLong presentAnonymousMemoryLimit, long expectedAnonymousMemoryLimit) {

        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();

        podTemplateSpecBuilder.getSpecBuilder().getResourceRequestsBuilder().setMemoryLimit(podMemoryLimit);

        presentAnonymousMemoryLimit.ifPresent(
                podTemplateSpecBuilder.getSpecBuilder().getResourceRequestsBuilder()::setAnonymousMemoryLimit
        );

        var patchResult = patch(new AnonLimitPatcherContext(), podTemplateSpecBuilder, DEFAULT_UNIT_CONTEXT);

        assertThat(patchResult.getPodSpec().getResourceRequests().getAnonymousMemoryLimit(),
                equalTo(expectedAnonymousMemoryLimit));
    }

    static TPodTemplateSpec.Builder createDefaultPodTemplateSpecBuilder() {
        return TPodTemplateSpec.newBuilder().setSpec(DataModel.TPodSpec.newBuilder());
    }

    @Test
    protected void shouldThrowExceptionIfMemoryLimitIsNotSet() {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();
        podTemplateSpecBuilder.getSpecBuilder().getResourceRequestsBuilder().clearMemoryLimit();

        Assertions.assertThrows(RuntimeException.class, () -> patch(new AnonLimitPatcherContext(),
                podTemplateSpecBuilder, DEFAULT_UNIT_CONTEXT));
    }

    @ParameterizedTest
    @CsvSource({
            "1000000000, 987654321",
            "100000000, 87654321",
            "50000000, 40654321",
    })
    void dontOverridePresentAnonLimit1GTest(long podMemory, long anonLimit) {
        anonymousMemoryLimitScenario(podMemory, OptionalLong.of(anonLimit), anonLimit);
    }
}
