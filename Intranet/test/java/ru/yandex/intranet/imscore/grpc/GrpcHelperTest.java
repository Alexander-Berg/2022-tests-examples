package ru.yandex.intranet.imscore.grpc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import org.junit.jupiter.api.Assertions;

import ru.yandex.intranet.imscore.proto.error.ErrorDetails;
import ru.yandex.intranet.imscore.proto.identity.Identity;

public final class GrpcHelperTest {
    public static final String GRPC_STRING_DEFAULT = com.google.protobuf.StringValue.getDefaultInstance().toString();

    private GrpcHelperTest() {
    }

    public static void assertMetadata(Throwable t, String key, String code) {
        assertMetadata(t, 1, new String[]{key, code});
    }

    public static void assertMetadata(Throwable t, int size, String[]... fieldErrors) {
        Status status = StatusProto.fromThrowable(t);
        Assertions.assertNotNull(status);
        Assertions.assertEquals(1, status.getDetailsList().size());

        for (Any any : status.getDetailsList()) {
            ErrorDetails error;
            try {
                error = any.unpack(ErrorDetails.class);
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }

            Assertions.assertEquals(size, error.getFieldErrorsCount());
            List<String[]> list = error.getFieldErrorsList().stream()
                    .map(fieldError -> new String[]{fieldError.getKey(), fieldError.getCode()})
                    .toList();
            Assertions.assertArrayEquals(fieldErrors, list.toArray());
        }
    }

    public static void startThreadsSimultaneously(Function<CyclicBarrier, Runnable>[] runnableFunc) {
        final CyclicBarrier gate = new CyclicBarrier(runnableFunc.length + 1);

        Future<?>[] futures = new Future<?>[runnableFunc.length];
        int i = 0;
        for (Function<CyclicBarrier, Runnable> func : runnableFunc) {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            futures[i++] = executorService.submit(func.apply(gate));
        }

        try {
            gate.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void assertEqualsWithoutUpdateDate(Set<Identity> one, Set<Identity> two) {
        Assertions.assertEquals(one.size(), two.size());
        Map<String, Identity> map = two.stream()
                .collect(Collectors.toMap(Identity::getId, Function.identity()));

        one.forEach(identity -> {
            Identity identity1 = map.get(identity.getId());
            Assertions.assertNotNull(identity1);
            assertEqualsWithoutUpdateDate(identity, identity1);
        });
    }

    public static void assertEqualsWithoutUpdateDate(Identity one, Identity two) {
        Assertions.assertEquals(one.getId(), two.getId());
        Assertions.assertEquals(one.getParentId(), two.getParentId());
        Assertions.assertEquals(one.getExternalId(), two.getExternalId());
        Assertions.assertEquals(one.getType(), two.getType());
        Assertions.assertEquals(one.getData(), two.getData());
        Assertions.assertEquals(one.getCreatedAt(), two.getCreatedAt());
    }
}
