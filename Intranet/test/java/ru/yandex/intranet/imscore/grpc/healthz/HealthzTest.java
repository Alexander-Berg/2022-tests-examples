package ru.yandex.intranet.imscore.grpc.healthz;

import java.util.List;

import com.google.protobuf.Empty;
import io.grpc.internal.testing.StreamRecorder;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.intranet.imscore.IntegrationTest;
import ru.yandex.intranet.imscore.core.services.healthz.HealthzService;
import ru.yandex.intranet.imscore.infrastructure.presentation.grpc.services.healthz.GrpcHealthzServiceImpl;
import ru.yandex.intranet.imscore.proto.healthz.HealthzCheckResponse;
import ru.yandex.intranet.imscore.proto.healthz.HealthzGrpc;

/**
 * Healthz grpc test.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@IntegrationTest
public class HealthzTest {
    @GrpcClient("inProcess")
    private HealthzGrpc.HealthzBlockingStub healthzBlockingStub;

    @Test
    public void livenessTest() {
        Empty empty = Empty.newBuilder().build();
        HealthzCheckResponse liveness = healthzBlockingStub.liveness(empty);
        Assertions.assertNotNull(liveness);
        Assertions.assertEquals(HealthzCheckResponse.Status.OK, liveness.getStatus());
    }

    @Test
    public void readinessTest() {
        Empty empty = Empty.newBuilder().build();
        HealthzCheckResponse readness = healthzBlockingStub.readness(empty);
        Assertions.assertNotNull(readness);
        Assertions.assertEquals(HealthzCheckResponse.Status.OK, readness.getStatus());
    }

    @Test
    public void readnessShouldCheckDBOnlyUntilFirstOKTest() {
        HealthzService mock = Mockito.mock(HealthzService.class);
        Mockito.when(mock.getReadiness()).thenReturn(true, false, false);
        GrpcHealthzServiceImpl grpcHealthzService = new GrpcHealthzServiceImpl(mock);

        StreamRecorder<HealthzCheckResponse> streamRecorder = StreamRecorder.create();
        Empty empty = Empty.newBuilder().build();

        grpcHealthzService.readness(empty, streamRecorder);
        List<HealthzCheckResponse> values = streamRecorder.getValues();
        Assertions.assertEquals(1, values.size());
        Assertions.assertEquals(values.get(0).getStatus(), HealthzCheckResponse.Status.OK);

        grpcHealthzService.readness(empty, streamRecorder);
        values = streamRecorder.getValues();
        Assertions.assertEquals(2, values.size());
        Assertions.assertEquals(values.get(1).getStatus(), HealthzCheckResponse.Status.OK);
    }
}
