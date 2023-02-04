package ru.yandex.infra.stage.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class AllComputeResourcesTest {

    public static final AllComputeResources DEFAULT_RESOURCES = new AllComputeResources(
            1, 2, 3, 4, 5, 6, 7
    );

    @Test
    void multiplyTest() {
        int multiplier = 10;

        var resources = DEFAULT_RESOURCES;

        var actualMultiplyResult = resources.multiply(multiplier);
        var expectedMultiplyResult = new AllComputeResources(
                resources.getVcpuGuarantee() * multiplier,
                resources.getVcpuLimit() * multiplier,
                resources.getMemoryGuarantee() * multiplier,
                resources.getMemoryLimit() * multiplier,
                resources.getAnonymousMemoryLimit() * multiplier,
                resources.getDiskCapacity() * multiplier,
                resources.getThreadLimit() * multiplier
        );

        assertThatEquals(actualMultiplyResult, expectedMultiplyResult);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 10 })
    void toProtoTest(int threadLimit) {
        var resources = DEFAULT_RESOURCES.toBuilder()
                .withThreadLimit(threadLimit)
                .build();

        var actualProto = resources.toProto();

        assertThatEquals(actualProto.getVcpuLimit(), resources.getVcpuLimit());
        assertThatEquals(actualProto.getVcpuGuarantee(), resources.getVcpuGuarantee());
        assertThatEquals(actualProto.getMemoryLimit(), resources.getMemoryLimit());
        assertThatEquals(actualProto.getMemoryGuarantee(), resources.getMemoryGuarantee());
        assertThatEquals(actualProto.getAnonymousMemoryLimit(), resources.getAnonymousMemoryLimit());

        if (0 != threadLimit) {
            assertThatEquals(actualProto.getThreadLimit(), resources.getThreadLimit());
        }
    }

    @Test
    void constructorFromProtoTest() {
        long otherDiskCapacity = DEFAULT_RESOURCES.getDiskCapacity() + 10;
        long otherThreadLimit = DEFAULT_RESOURCES.getThreadLimit() + 10;

        var protoResources = DEFAULT_RESOURCES.toProto();

        var actualResources = new AllComputeResources(
                protoResources,
                otherDiskCapacity,
                otherThreadLimit
        );

        var expectedResources = new AllComputeResources(
                protoResources.getVcpuGuarantee(),
                protoResources.getVcpuLimit(),
                protoResources.getMemoryGuarantee(),
                protoResources.getMemoryLimit(),
                protoResources.getAnonymousMemoryLimit(),
                otherDiskCapacity,
                otherThreadLimit
        );

        assertThatEquals(actualResources, expectedResources);
    }

    @Test
    void constructorGuaranteeEqualsLimitTest() {
        long vcpu = DEFAULT_RESOURCES.getVcpuGuarantee();
        long memory = DEFAULT_RESOURCES.getMemoryGuarantee();

        var actualResources = new AllComputeResources(
                vcpu,
                memory,
                DEFAULT_RESOURCES.getAnonymousMemoryLimit(),
                DEFAULT_RESOURCES.getDiskCapacity(),
                DEFAULT_RESOURCES.getThreadLimit()
        );

        var expectedResources = new AllComputeResources(
                vcpu,
                vcpu,
                memory,
                memory,
                DEFAULT_RESOURCES.getAnonymousMemoryLimit(),
                DEFAULT_RESOURCES.getDiskCapacity(),
                DEFAULT_RESOURCES.getThreadLimit()
        );

        assertThatEquals(actualResources, expectedResources);
    }

    @Test
    void constructorAnonymousLimitEqualsMemoryLimitTest() {
        long vcpu = DEFAULT_RESOURCES.getVcpuGuarantee();
        long memory = DEFAULT_RESOURCES.getMemoryGuarantee();

        var actualResources = new AllComputeResources(
                vcpu,
                memory,
                DEFAULT_RESOURCES.getDiskCapacity(),
                DEFAULT_RESOURCES.getThreadLimit()
        );

        var expectedResources = new AllComputeResources(
                vcpu,
                memory,
                memory,
                DEFAULT_RESOURCES.getDiskCapacity(),
                DEFAULT_RESOURCES.getThreadLimit()
        );

        assertThatEquals(actualResources, expectedResources);
    }

    @Test
    void builderTest() {
        var initialResources = DEFAULT_RESOURCES;
        var testShift = 100;

        var expectedResources = new AllComputeResources(
                initialResources.getVcpuGuarantee() + testShift,
                initialResources.getVcpuLimit() + testShift,
                initialResources.getMemoryGuarantee() + testShift,
                initialResources.getMemoryLimit() + testShift,
                initialResources.getAnonymousMemoryLimit() + testShift,
                initialResources.getDiskCapacity() + testShift,
                initialResources.getThreadLimit() + testShift
        );

        var actualResources = initialResources.toBuilder()
                .withVcpuGuarantee(expectedResources.getVcpuGuarantee())
                .withVcpuLimit(expectedResources.getVcpuLimit())
                .withMemoryGuarantee(expectedResources.getMemoryGuarantee())
                .withMemoryLimit(expectedResources.getMemoryLimit())
                .withAnonymousMemoryLimit(expectedResources.getAnonymousMemoryLimit())
                .withDiskCapacity(expectedResources.getDiskCapacity())
                .withThreadLimit(expectedResources.getThreadLimit())
                .build();

        assertThatEquals(actualResources, expectedResources);
    }
}
