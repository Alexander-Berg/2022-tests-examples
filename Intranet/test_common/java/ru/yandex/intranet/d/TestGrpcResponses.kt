package ru.yandex.intranet.d

import com.google.protobuf.Any
import com.google.rpc.BadRequest
import com.google.rpc.Code
import com.google.rpc.Status
import io.grpc.protobuf.StatusProto
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse

/**
 * Test Grpc responses.
 *
 * @author Petr Surkov <petrsurkov@yandex-team.ru>
 */
object TestGrpcResponses {

    @JvmStatic
    private fun <T> getErrorTestResponse(code: Code): GrpcResponse<T> {
        return when (code) {
            Code.OK, Code.UNRECOGNIZED -> throw IllegalArgumentException()
            else -> GrpcResponse.failure(StatusProto.toStatusRuntimeException(Status.newBuilder()
                .setCode(code.number)
                .setMessage("Test error")
                .addDetails(Any.pack(BadRequest.newBuilder()
                    .addFieldViolations(BadRequest.FieldViolation.newBuilder()
                        .setField("test")
                        .setDescription("Test error description")
                        .build())
                    .build()))
                .build()))
        }
    }

    @JvmStatic
    fun <T> alreadyExistsTestResponse(): GrpcResponse<T> {
        return getErrorTestResponse(Code.ALREADY_EXISTS)
    }

    @JvmStatic
    fun <T> unavailableTestResponse(): GrpcResponse<T> {
        return getErrorTestResponse(Code.UNAVAILABLE)
    }

    @JvmStatic
    fun <T> permissionDeniedTestResponse(): GrpcResponse<T> {
        return getErrorTestResponse(Code.PERMISSION_DENIED)
    }

    @JvmStatic
    fun <T> internalTestResponse(): GrpcResponse<T> {
        return getErrorTestResponse(Code.INTERNAL)
    }

    @JvmStatic
    fun <T> failedPreconditionTestResponse(): GrpcResponse<T> {
        return getErrorTestResponse(Code.FAILED_PRECONDITION)
    }

    @JvmStatic
    fun <T> unknownTestResponse(): GrpcResponse<T> {
        return getErrorTestResponse(Code.UNKNOWN)
    }
}
