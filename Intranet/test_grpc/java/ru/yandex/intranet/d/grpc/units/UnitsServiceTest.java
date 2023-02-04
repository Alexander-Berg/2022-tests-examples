package ru.yandex.intranet.d.grpc.units;

import java.util.Optional;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.GetUnitRequest;
import ru.yandex.intranet.d.backend.service.proto.GetUnitsEnsembleRequest;
import ru.yandex.intranet.d.backend.service.proto.ListUnitsEnsemblesRequest;
import ru.yandex.intranet.d.backend.service.proto.ListUnitsEnsemblesResponse;
import ru.yandex.intranet.d.backend.service.proto.Unit;
import ru.yandex.intranet.d.backend.service.proto.UnitsEnsemble;
import ru.yandex.intranet.d.backend.service.proto.UnitsLimit;
import ru.yandex.intranet.d.backend.service.proto.UnitsPageToken;
import ru.yandex.intranet.d.backend.service.proto.UnitsServiceGrpc;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.utils.ErrorsHelper;

/**
 * Units GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class UnitsServiceTest {

    @GrpcClient("inProcess")
    private UnitsServiceGrpc.UnitsServiceBlockingStub unitsService;

    @Test
    public void getEnsembleTest() {
        GetUnitsEnsembleRequest unitsEnsembleRequest = GetUnitsEnsembleRequest.newBuilder()
                .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                .build();
        UnitsEnsemble unitsEnsemble = unitsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getUnitsEnsemble(unitsEnsembleRequest);
        Assertions.assertNotNull(unitsEnsemble);
        Assertions.assertEquals("b02344bf-96af-4cc5-937c-66a479989ce8", unitsEnsemble.getUnitsEnsembleId());
    }

    @Test
    public void getEnsembleNotFoundTest() {
        GetUnitsEnsembleRequest unitsEnsembleRequest = GetUnitsEnsembleRequest.newBuilder()
                .setUnitsEnsembleId("12345678-9012-3456-7890-123456789012")
                .build();
        try {
            unitsService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getUnitsEnsemble(unitsEnsembleRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void getUnitTest() {
        GetUnitRequest unitsEnsembleRequest = GetUnitRequest.newBuilder()
                .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                .setUnitId("b15101c2-da50-4d6f-9a8e-b90160871b0a")
                .build();
        Unit unit = unitsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getUnit(unitsEnsembleRequest);
        Assertions.assertNotNull(unit);
        Assertions.assertEquals("b15101c2-da50-4d6f-9a8e-b90160871b0a", unit.getUnitId());
    }

    @Test
    public void getUnitNotFoundTest() {
        GetUnitRequest unitsEnsembleRequest = GetUnitRequest.newBuilder()
                .setUnitsEnsembleId("b02344bf-96af-4cc5-937c-66a479989ce8")
                .setUnitId("12345678-9012-3456-7890-123456789012")
                .build();
        try {
            unitsService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getUnit(unitsEnsembleRequest);
        } catch (StatusRuntimeException e) {
            Assertions.assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
            Optional<ErrorDetails> details = ErrorsHelper.extractErrorDetails(e);
            Assertions.assertTrue(details.isPresent());
            Assertions.assertTrue(details.get().getErrorsCount() > 0);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void getEnsemblesPageTest() {
        ListUnitsEnsemblesRequest unitsEnsembleRequest = ListUnitsEnsemblesRequest.newBuilder()
                .build();
        ListUnitsEnsemblesResponse page = unitsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listUnitsEnsembles(unitsEnsembleRequest);
        Assertions.assertNotNull(page);
        Assertions.assertTrue(page.getUnitsEnsemblesCount() > 0);
    }

    @Test
    public void getEnsemblesTwoPagesTest() {
        ListUnitsEnsemblesRequest firstRequest = ListUnitsEnsemblesRequest.newBuilder()
                .setLimit(UnitsLimit.newBuilder().setLimit(1L).build())
                .build();
        ListUnitsEnsemblesResponse firstPage = unitsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listUnitsEnsembles(firstRequest);
        Assertions.assertNotNull(firstPage);
        Assertions.assertEquals(1, firstPage.getUnitsEnsemblesCount());
        Assertions.assertTrue(firstPage.hasNextPageToken());
        ListUnitsEnsemblesRequest secondRequest = ListUnitsEnsemblesRequest.newBuilder()
                .setPageToken(UnitsPageToken.newBuilder()
                        .setToken(firstPage.getNextPageToken().getToken()).build())
                .build();
        ListUnitsEnsemblesResponse secondPage = unitsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .listUnitsEnsembles(secondRequest);
        Assertions.assertNotNull(secondPage);
        Assertions.assertTrue(secondPage.getUnitsEnsemblesCount() > 0);
    }

}
