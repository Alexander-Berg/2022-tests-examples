package ru.yandex.intranet.d.grpc.operations;

import java.util.Optional;

import com.google.protobuf.util.Timestamps;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;
import ru.yandex.intranet.d.backend.service.proto.GetOperationStateRequest;
import ru.yandex.intranet.d.backend.service.proto.OperationState;
import ru.yandex.intranet.d.backend.service.proto.OperationStatus;
import ru.yandex.intranet.d.backend.service.proto.OperationsServiceGrpc;
import ru.yandex.intranet.d.grpc.MockGrpcUser;
import ru.yandex.intranet.d.utils.ErrorsHelper;

/**
 * Operations GRPC API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class OperationsServiceTest {

    @GrpcClient("inProcess")
    private OperationsServiceGrpc.OperationsServiceBlockingStub operationsService;

    @Test
    public void getOperationStateTest() {
        GetOperationStateRequest operationStateRequest = GetOperationStateRequest.newBuilder()
                .setOperationId("273c0fee-5cf1-4083-b9dc-8ec0e855e150")
                .build();
        OperationState operationState = operationsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getOperationState(operationStateRequest);
        Assertions.assertNotNull(operationState);
        Assertions.assertEquals("273c0fee-5cf1-4083-b9dc-8ec0e855e150", operationState.getId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", operationState.getProviderId());
        Assertions.assertFalse(operationState.hasAccountsSpaceId());
        Assertions.assertEquals(OperationStatus.SUCCESS, operationState.getStatus());
        Assertions.assertEquals(1603385085, Timestamps.toSeconds(operationState.getCreatedAt()));
        Assertions.assertFalse(operationState.hasFailure());
    }

    @Test
    public void getOperationStateNotFoundTest() {
        GetOperationStateRequest operationStateRequest = GetOperationStateRequest.newBuilder()
                .setOperationId("12345678-9012-3456-7890-123456789012")
                .build();
        try {
            operationsService
                    .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                    .getOperationState(operationStateRequest);
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
    public void getOperationWithErrorTest() {
        GetOperationStateRequest operationStateRequest = GetOperationStateRequest.newBuilder()
                .setOperationId("4596ab7c-4535-9623-bc12-13ab94ef341d")
                .build();
        OperationState operationStateWithError = operationsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getOperationState(operationStateRequest);
        Assertions.assertNotNull(operationStateWithError);
        Assertions.assertEquals("4596ab7c-4535-9623-bc12-13ab94ef341d", operationStateWithError.getId());
        Assertions.assertEquals(OperationStatus.FAILURE, operationStateWithError.getStatus());
        Assertions.assertNotNull(operationStateWithError.getFailure());
        Assertions.assertEquals(1, operationStateWithError.getFailure().getErrorsCount());
        Assertions.assertEquals("Test error", operationStateWithError.getFailure().getErrors(0));

        GetOperationStateRequest operationStateRequest2 = GetOperationStateRequest.newBuilder()
                .setOperationId("513cab44-ac21-87da-b99c-bbe10cad123c")
                .build();
        OperationState operationStateWithFullError = operationsService
                .withCallCredentials(MockGrpcUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .getOperationState(operationStateRequest2);
        Assertions.assertNotNull(operationStateWithFullError);
        Assertions.assertEquals("513cab44-ac21-87da-b99c-bbe10cad123c", operationStateWithFullError.getId());
        Assertions.assertEquals(OperationStatus.FAILURE, operationStateWithFullError.getStatus());
        Assertions.assertNotNull(operationStateWithFullError.getFailure());
        Assertions.assertEquals(1, operationStateWithFullError.getFailure().getErrorsCount());
        Assertions.assertEquals("Test error", operationStateWithFullError.getFailure().getErrors(0));
    }

}
