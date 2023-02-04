package ru.yandex.payments.cloud;

import java.util.List;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import ru.yandex.payments.util.cloud.CloudMetadata;
import ru.yandex.payments.util.cloud.CloudMetadataProvider;
import ru.yandex.payments.util.cloud.CloudMetadataResolver;
import ru.yandex.payments.util.cloud.DC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

record TestMetadata(DC dc,
                    String cloudName,
                    String instanceId) implements CloudMetadata {
}

class CloudMetadataProviderTest {
    private static final CloudMetadata METADATA = new TestMetadata(DC.VLA, "test-cloud", "test-instance");
    private static final CloudMetadata METADATA2 = new TestMetadata(DC.IVA, "test-cloud-2", "test-instance-2");

    @Test
    @DisplayName("Verify that `findMetadata` returns cached metadata")
    void testMetadataResolve() {
        val resolverMock = mock(CloudMetadataResolver.class);
        when(resolverMock.resolveMetadata())
                .thenReturn(Mono.just(METADATA));

        val provider = new CloudMetadataProvider(List.of(resolverMock));

        for (int i = 0; i < 3; i++) {
            assertThat(provider.findMetadata().block())
                    .isNotNull()
                    .isEqualTo(METADATA);
        }

        verify(resolverMock, only()).resolveMetadata();
    }

    @Test
    @DisplayName("Verify that `findMetadata` returns nothing metadata is not resolved")
    void testEmptyMetadataResolve() {
        val resolverMock = mock(CloudMetadataResolver.class);
        when(resolverMock.resolveMetadata())
                .thenReturn(Mono.empty());

        val provider = new CloudMetadataProvider(List.of(resolverMock));

        for (int i = 0; i < 3; i++) {
            assertThat(provider.findMetadata().block())
                    .isNull();
        }

        verify(resolverMock, only()).resolveMetadata();
    }

    static class ResolveException extends RuntimeException {
        ResolveException() {
            super();
        }
    }

    @Test
    @DisplayName("Verify that `findMetadata` always retries resolve on error")
    void testFailedMetadataResolve() {
        val resolverMock = mock(CloudMetadataResolver.class);
        when(resolverMock.resolveMetadata())
                .thenReturn(Mono.error(new ResolveException()));

        val provider = new CloudMetadataProvider(List.of(resolverMock));

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> provider.findMetadata().block())
                    .isInstanceOf(ResolveException.class);
        }

        verify(resolverMock, times(3)).resolveMetadata();
        verifyNoMoreInteractions(resolverMock);
    }

    @Test
    @DisplayName("Verify that `findMetadata` returns one of the resolved metadata instances")
    void testMultipleMetadataResolve() {
        val resolverMock = mock(CloudMetadataResolver.class);
        when(resolverMock.resolveMetadata())
                .thenReturn(Mono.just(METADATA));

        val resolverMock2 = mock(CloudMetadataResolver.class);
        when(resolverMock2.resolveMetadata())
                .thenReturn(Mono.just(METADATA2));

        val provider = new CloudMetadataProvider(List.of(resolverMock, resolverMock2));

        val metadata = provider.findMetadata().block();
        assertThat(metadata)
                .isNotNull()
                .isIn(METADATA, METADATA2);

        for (int i = 0; i < 2; i++) {
            assertThat(provider.findMetadata().block())
                    .isNotNull()
                    .isEqualTo(metadata);
        }

        verify(resolverMock, only()).resolveMetadata();
        verify(resolverMock2, only()).resolveMetadata();
    }
}
