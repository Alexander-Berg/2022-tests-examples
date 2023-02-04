package ru.yandex.intranet.d.grpc.stub.providers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.google.protobuf.Empty;
import io.grpc.Metadata;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.backend.service.provider_proto.Account;
import ru.yandex.intranet.d.backend.service.provider_proto.AccountsServiceGrpc;
import ru.yandex.intranet.d.backend.service.provider_proto.CreateAccountAndProvideRequest;
import ru.yandex.intranet.d.backend.service.provider_proto.CreateAccountRequest;
import ru.yandex.intranet.d.backend.service.provider_proto.DeleteAccountRequest;
import ru.yandex.intranet.d.backend.service.provider_proto.GetAccountRequest;
import ru.yandex.intranet.d.backend.service.provider_proto.ListAccountsByFolderRequest;
import ru.yandex.intranet.d.backend.service.provider_proto.ListAccountsByFolderResponse;
import ru.yandex.intranet.d.backend.service.provider_proto.ListAccountsRequest;
import ru.yandex.intranet.d.backend.service.provider_proto.ListAccountsResponse;
import ru.yandex.intranet.d.backend.service.provider_proto.MoveAccountRequest;
import ru.yandex.intranet.d.backend.service.provider_proto.MoveProvisionRequest;
import ru.yandex.intranet.d.backend.service.provider_proto.MoveProvisionResponse;
import ru.yandex.intranet.d.backend.service.provider_proto.RenameAccountRequest;
import ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionRequest;
import ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse;

/**
 * Stub provider service implementation.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@GrpcService(interceptors = {MetadataInterceptor.class, RequestIdInterceptor.class})
public class StubProviderService extends AccountsServiceGrpc.AccountsServiceImplBase {

    private final AtomicLong updateProvisionCallCounter = new AtomicLong(0L);
    private final AtomicLong getAccountCallCounter = new AtomicLong(0L);
    private final AtomicLong createAccountCallCounter = new AtomicLong(0L);
    private final AtomicLong createAccountAndProvideCallCounter = new AtomicLong(0L);
    private final AtomicLong deleteAccountCallCounter = new AtomicLong(0L);
    private final AtomicLong renameAccountCallCounter = new AtomicLong(0L);
    private final AtomicLong moveAccountCallCounter = new AtomicLong(0L);
    private final AtomicLong moveProvisionCallCounter = new AtomicLong(0L);
    private final AtomicLong listAccountsCallCounter = new AtomicLong(0L);
    private final AtomicLong listAccountsByFolderCallCounter = new AtomicLong(0L);
    private final ConcurrentLinkedDeque<Tuple2<UpdateProvisionRequest, Metadata>> updateProvisionRequests
            = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Tuple2<GetAccountRequest, Metadata>> getAccountRequests
            = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Tuple2<CreateAccountRequest, Metadata>> createAccountRequests
            = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Tuple2<CreateAccountAndProvideRequest,
            Metadata>> createAccountAndProvideRequests = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Tuple2<DeleteAccountRequest, Metadata>> deleteAccountRequests
            = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Tuple2<RenameAccountRequest, Metadata>> renameAccountRequests
            = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Tuple2<MoveAccountRequest, Metadata>> moveAccountRequests
            = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Tuple2<MoveProvisionRequest, Metadata>> moveProvisionRequests
            = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Tuple2<ListAccountsRequest, Metadata>> listAccountsRequests
            = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Tuple2<ListAccountsByFolderRequest, Metadata>> listAccountsByFolderRequests
            = new ConcurrentLinkedDeque<>();
    private final AtomicReference<List<GrpcResponse<UpdateProvisionResponse>>> updateProvisionResponses
            = new AtomicReference<>(List.of());
    private final AtomicReference<List<GrpcResponse<Account>>> getAccountResponses
            = new AtomicReference<>(List.of());
    private final AtomicReference<List<GrpcResponse<Account>>> createAccountResponses
            = new AtomicReference<>(List.of());
    private final AtomicReference<List<GrpcResponse<Account>>> createAccountAndProvideResponses
            = new AtomicReference<>(List.of());
    private final AtomicReference<List<GrpcResponse<Empty>>> deleteAccountResponses
            = new AtomicReference<>(List.of());
    private final AtomicReference<List<GrpcResponse<Account>>> renameAccountResponses
            = new AtomicReference<>(List.of());
    private final AtomicReference<List<GrpcResponse<Account>>> moveAccountResponses
            = new AtomicReference<>(List.of());
    private final AtomicReference<List<GrpcResponse<MoveProvisionResponse>>> moveProvisionResponses
            = new AtomicReference<>(List.of());
    private final AtomicReference<List<GrpcResponse<ListAccountsResponse>>> listAccountsResponses
            = new AtomicReference<>(List.of());
    private final AtomicReference<List<GrpcResponse<ListAccountsByFolderResponse>>> listAccountsByFolderResponses
            = new AtomicReference<>(List.of());
    private final AtomicBoolean updateProvisionResponseByMappingFunction = new AtomicBoolean(false);
    private final Map<Object, GrpcResponse<UpdateProvisionResponse>> updateProvisionResponsesMap =
            new ConcurrentHashMap<>();
    private final AtomicReference<Function<UpdateProvisionRequest, Object>> updateProvisionRequestToKeyFunction =
            new AtomicReference<>();

    private final Map<String, List<GrpcResponse<Account>>> getAccountResponsesByAccountId = new ConcurrentHashMap<>();

    @Override
    public void updateProvision(UpdateProvisionRequest request,
                                StreamObserver<UpdateProvisionResponse> responseObserver) {
        if (updateProvisionResponseByMappingFunction.get()) {
            updateProvisionWithResponseMapping(request, responseObserver);
            return;
        }

        long callIndex = updateProvisionCallCounter.getAndIncrement();
        Metadata headers = MetadataInterceptor.METADATA_KEY.get();
        updateProvisionRequests.add(Tuples.of(request, headers));
        List<GrpcResponse<UpdateProvisionResponse>> responses = updateProvisionResponses.get();
        if (responses.isEmpty()) {
            responseObserver.onError(new IllegalArgumentException("No responses set"));
            return;
        }
        int index = callIndex >= responses.size() ? responses.size() - 1 : (int) callIndex;
        GrpcResponse<UpdateProvisionResponse> response = responses.get(index);
        if (response.getValue().isPresent()) {
            responseObserver.onNext(response.getValue().get());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(response.getError().get());
        }
    }

    public void updateProvisionWithResponseMapping(UpdateProvisionRequest request,
                                                   StreamObserver<UpdateProvisionResponse> responseObserver) {
        Function<UpdateProvisionRequest, Object> updateProvisionRequestObjectFunction =
                updateProvisionRequestToKeyFunction.get();
        if (updateProvisionRequestObjectFunction == null) {
            responseObserver.onError(new IllegalArgumentException("updateProvisionRequestToKeyFunction is not set"));
            return;
        }

        updateProvisionCallCounter.getAndIncrement();
        Metadata headers = MetadataInterceptor.METADATA_KEY.get();
        updateProvisionRequests.add(Tuples.of(request, headers));

        if (updateProvisionResponsesMap.isEmpty()) {
            responseObserver.onError(new IllegalArgumentException("No responses set"));
            return;
        }

        Object key = updateProvisionRequestObjectFunction.apply(request);
        if (!updateProvisionResponsesMap.containsKey(key)) {
            responseObserver.onError(new IllegalArgumentException(
                    String.format("No associated value with key %s", key)));
            return;
        }
        GrpcResponse<UpdateProvisionResponse> response = updateProvisionResponsesMap.get(key);
        if (response.getValue().isPresent()) {
            responseObserver.onNext(response.getValue().get());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(response.getError().orElseThrow());
        }
    }

    @Override
    public void getAccount(GetAccountRequest request, StreamObserver<Account> responseObserver) {
        long callIndex = getAccountCallCounter.getAndIncrement();
        Metadata headers = MetadataInterceptor.METADATA_KEY.get();
        getAccountRequests.add(Tuples.of(request, headers));
        List<GrpcResponse<Account>> responsesByAccount = getAccountResponsesByAccountId.getOrDefault(
                request.getAccountId(), List.of());
        List<GrpcResponse<Account>> responses = !responsesByAccount.isEmpty()
                ? responsesByAccount
                : getAccountResponses.get();
        if (responses.isEmpty()) {
            responseObserver.onError(new IllegalArgumentException("No responses set"));
            return;
        }
        int index = callIndex >= responses.size() ? responses.size() - 1 : (int) callIndex;
        GrpcResponse<Account> response = responses.get(index);
        if (response.getValue().isPresent()) {
            responseObserver.onNext(response.getValue().get());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(response.getError().get());
        }
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<Account> responseObserver) {
        long callIndex = createAccountCallCounter.getAndIncrement();
        Metadata headers = MetadataInterceptor.METADATA_KEY.get();
        createAccountRequests.add(Tuples.of(request, headers));
        List<GrpcResponse<Account>> responses = createAccountResponses.get();
        if (responses.isEmpty()) {
            responseObserver.onError(new IllegalArgumentException("No responses set"));
            return;
        }
        int index = callIndex >= responses.size() ? responses.size() - 1 : (int) callIndex;
        GrpcResponse<Account> response = responses.get(index);
        if (response.getValue().isPresent()) {
            responseObserver.onNext(response.getValue().get());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(response.getError().get());
        }
    }

    @Override
    public void createAccountAndProvide(CreateAccountAndProvideRequest request,
                                        StreamObserver<Account> responseObserver) {
        long callIndex = createAccountAndProvideCallCounter.getAndIncrement();
        Metadata headers = MetadataInterceptor.METADATA_KEY.get();
        createAccountAndProvideRequests.add(Tuples.of(request, headers));
        List<GrpcResponse<Account>> responses = createAccountAndProvideResponses.get();
        if (responses.isEmpty()) {
            responseObserver.onError(new IllegalArgumentException("No responses set"));
            return;
        }
        int index = callIndex >= responses.size() ? responses.size() - 1 : (int) callIndex;
        GrpcResponse<Account> response = responses.get(index);
        if (response.getValue().isPresent()) {
            responseObserver.onNext(response.getValue().get());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(response.getError().get());
        }
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<Empty> responseObserver) {
        long callIndex = deleteAccountCallCounter.getAndIncrement();
        Metadata headers = MetadataInterceptor.METADATA_KEY.get();
        deleteAccountRequests.add(Tuples.of(request, headers));
        List<GrpcResponse<Empty>> responses = deleteAccountResponses.get();
        if (responses.isEmpty()) {
            responseObserver.onError(new IllegalArgumentException("No responses set"));
            return;
        }
        int index = callIndex >= responses.size() ? responses.size() - 1 : (int) callIndex;
        GrpcResponse<Empty> response = responses.get(index);
        if (response.getValue().isPresent()) {
            responseObserver.onNext(response.getValue().get());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(response.getError().get());
        }
    }

    @Override
    public void renameAccount(RenameAccountRequest request, StreamObserver<Account> responseObserver) {
        long callIndex = renameAccountCallCounter.getAndIncrement();
        Metadata headers = MetadataInterceptor.METADATA_KEY.get();
        renameAccountRequests.add(Tuples.of(request, headers));
        List<GrpcResponse<Account>> responses = renameAccountResponses.get();
        if (responses.isEmpty()) {
            responseObserver.onError(new IllegalArgumentException("No responses set"));
            return;
        }
        int index = callIndex >= responses.size() ? responses.size() - 1 : (int) callIndex;
        GrpcResponse<Account> response = responses.get(index);
        if (response.getValue().isPresent()) {
            responseObserver.onNext(response.getValue().get());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(response.getError().get());
        }
    }

    @Override
    public void moveAccount(MoveAccountRequest request, StreamObserver<Account> responseObserver) {
        long callIndex = moveAccountCallCounter.getAndIncrement();
        Metadata headers = MetadataInterceptor.METADATA_KEY.get();
        moveAccountRequests.add(Tuples.of(request, headers));
        List<GrpcResponse<Account>> responses = moveAccountResponses.get();
        if (responses.isEmpty()) {
            responseObserver.onError(new IllegalArgumentException("No responses set"));
            return;
        }
        int index = callIndex >= responses.size() ? responses.size() - 1 : (int) callIndex;
        GrpcResponse<Account> response = responses.get(index);
        if (response.getValue().isPresent()) {
            responseObserver.onNext(response.getValue().get());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(response.getError().get());
        }
    }

    @Override
    public void moveProvision(MoveProvisionRequest request, StreamObserver<MoveProvisionResponse> responseObserver) {
        long callIndex = moveProvisionCallCounter.getAndIncrement();
        Metadata headers = MetadataInterceptor.METADATA_KEY.get();
        moveProvisionRequests.add(Tuples.of(request, headers));
        List<GrpcResponse<MoveProvisionResponse>> responses = moveProvisionResponses.get();
        if (responses.isEmpty()) {
            responseObserver.onError(new IllegalArgumentException("No responses set"));
            return;
        }
        int index = callIndex >= responses.size() ? responses.size() - 1 : (int) callIndex;
        GrpcResponse<MoveProvisionResponse> response = responses.get(index);
        if (response.getValue().isPresent()) {
            responseObserver.onNext(response.getValue().get());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(response.getError().get());
        }
    }

    @Override
    public void listAccounts(ListAccountsRequest request, StreamObserver<ListAccountsResponse> responseObserver) {
        long callIndex = listAccountsCallCounter.getAndIncrement();
        Metadata headers = MetadataInterceptor.METADATA_KEY.get();
        listAccountsRequests.add(Tuples.of(request, headers));
        List<GrpcResponse<ListAccountsResponse>> responses = listAccountsResponses.get();
        if (responses.isEmpty()) {
            responseObserver.onError(new IllegalArgumentException("No responses set"));
            return;
        }
        int index = callIndex >= responses.size() ? responses.size() - 1 : (int) callIndex;
        GrpcResponse<ListAccountsResponse> response = responses.get(index);
        if (response.getValue().isPresent()) {
            responseObserver.onNext(response.getValue().get());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(response.getError().get());
        }
    }

    @Override
    public void listAccountsByFolder(ListAccountsByFolderRequest request,
                                     StreamObserver<ListAccountsByFolderResponse> responseObserver) {
        long callIndex = listAccountsByFolderCallCounter.getAndIncrement();
        Metadata headers = MetadataInterceptor.METADATA_KEY.get();
        listAccountsByFolderRequests.add(Tuples.of(request, headers));
        List<GrpcResponse<ListAccountsByFolderResponse>> responses = listAccountsByFolderResponses.get();
        if (responses.isEmpty()) {
            responseObserver.onError(new IllegalArgumentException("No responses set"));
            return;
        }
        int index = callIndex >= responses.size() ? responses.size() - 1 : (int) callIndex;
        GrpcResponse<ListAccountsByFolderResponse> response = responses.get(index);
        if (response.getValue().isPresent()) {
            responseObserver.onNext(response.getValue().get());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(response.getError().get());
        }
    }

    public void reset() {
        updateProvisionCallCounter.set(0L);
        getAccountCallCounter.set(0L);
        createAccountCallCounter.set(0L);
        createAccountAndProvideCallCounter.set(0L);
        deleteAccountCallCounter.set(0L);
        renameAccountCallCounter.set(0L);
        moveAccountCallCounter.set(0L);
        moveProvisionCallCounter.set(0L);
        listAccountsCallCounter.set(0L);
        listAccountsByFolderCallCounter.set(0L);
        updateProvisionRequests.clear();
        getAccountRequests.clear();
        createAccountRequests.clear();
        createAccountAndProvideRequests.clear();
        deleteAccountRequests.clear();
        renameAccountRequests.clear();
        moveAccountRequests.clear();
        moveProvisionRequests.clear();
        listAccountsRequests.clear();
        listAccountsByFolderRequests.clear();
        updateProvisionResponses.set(List.of());
        getAccountResponses.set(List.of());
        createAccountResponses.set(List.of());
        createAccountAndProvideResponses.set(List.of());
        deleteAccountResponses.set(List.of());
        renameAccountResponses.set(List.of());
        moveAccountResponses.set(List.of());
        moveProvisionResponses.set(List.of());
        listAccountsResponses.set(List.of());
        listAccountsByFolderResponses.set(List.of());
        updateProvisionResponseByMappingFunction.set(false);
        updateProvisionResponsesMap.clear();
        updateProvisionRequestToKeyFunction.set(null);
        getAccountResponsesByAccountId.clear();
    }

    public long getUpdateProvisionCallCount() {
        return updateProvisionCallCounter.get();
    }

    public long getGetAccountCallCount() {
        return getAccountCallCounter.get();
    }

    public long getCreateAccountCallCount() {
        return createAccountCallCounter.get();
    }

    public long getCreateAccountAndProvideCallCount() {
        return createAccountAndProvideCallCounter.get();
    }

    public long getDeleteAccountCallCount() {
        return deleteAccountCallCounter.get();
    }

    public long getRenameAccountCallCount() {
        return renameAccountCallCounter.get();
    }

    public long getMoveAccountCallCount() {
        return moveAccountCallCounter.get();
    }

    public long getMoveProvisionCallCount() {
        return moveProvisionCallCounter.get();
    }

    public long getListAccountsCallCount() {
        return listAccountsCallCounter.get();
    }

    public long getListAccountsByFolderCallCount() {
        return listAccountsByFolderCallCounter.get();
    }

    public Deque<Tuple2<UpdateProvisionRequest, Metadata>> getUpdateProvisionRequests() {
        return new ArrayDeque<>(updateProvisionRequests);
    }

    public Deque<Tuple2<GetAccountRequest, Metadata>> getGetAccountRequests() {
        return new ArrayDeque<>(getAccountRequests);
    }

    public Deque<Tuple2<CreateAccountRequest, Metadata>> getCreateAccountRequests() {
        return new ArrayDeque<>(createAccountRequests);
    }

    public Deque<Tuple2<CreateAccountAndProvideRequest, Metadata>> getCreateAccountAndProvideRequests() {
        return new ArrayDeque<>(createAccountAndProvideRequests);
    }

    public Deque<Tuple2<DeleteAccountRequest, Metadata>> getDeleteAccountRequests() {
        return new ArrayDeque<>(deleteAccountRequests);
    }

    public Deque<Tuple2<RenameAccountRequest, Metadata>> getRenameAccountRequests() {
        return new ArrayDeque<>(renameAccountRequests);
    }

    public Deque<Tuple2<MoveAccountRequest, Metadata>> getMoveAccountRequests() {
        return new ArrayDeque<>(moveAccountRequests);
    }

    public Deque<Tuple2<MoveProvisionRequest, Metadata>> getMoveProvisionRequests() {
        return new ArrayDeque<>(moveProvisionRequests);
    }

    public Deque<Tuple2<ListAccountsRequest, Metadata>> getListAccountsRequests() {
        return new ArrayDeque<>(listAccountsRequests);
    }

    public Deque<Tuple2<ListAccountsByFolderRequest, Metadata>> getListAccountsByFolderRequests() {
        return new ArrayDeque<>(listAccountsByFolderRequests);
    }

    public void setUpdateProvisionResponses(List<GrpcResponse<UpdateProvisionResponse>> list) {
        updateProvisionResponses.set(new ArrayList<>(list));
    }

    public void setGetAccountResponses(List<GrpcResponse<Account>> list) {
        getAccountResponses.set(new ArrayList<>(list));
    }

    public void setCreateAccountResponses(List<GrpcResponse<Account>> list) {
        createAccountResponses.set(new ArrayList<>(list));
    }

    public void setCreateAccountAndProvideResponses(List<GrpcResponse<Account>> list) {
        createAccountAndProvideResponses.set(new ArrayList<>(list));
    }

    public void setDeleteAccountResponses(List<GrpcResponse<Empty>> list) {
        deleteAccountResponses.set(new ArrayList<>(list));
    }

    public void setRenameAccountResponses(List<GrpcResponse<Account>> list) {
        renameAccountResponses.set(new ArrayList<>(list));
    }

    public void setMoveAccountResponses(List<GrpcResponse<Account>> list) {
        moveAccountResponses.set(new ArrayList<>(list));
    }

    public void setMoveProvisionResponses(List<GrpcResponse<MoveProvisionResponse>> list) {
        moveProvisionResponses.set(new ArrayList<>(list));
    }

    public void setListAccountsResponses(List<GrpcResponse<ListAccountsResponse>> list) {
        listAccountsResponses.set(new ArrayList<>(list));
    }

    public void setListAccountsByFolderResponses(List<GrpcResponse<ListAccountsByFolderResponse>> list) {
        listAccountsByFolderResponses.set(new ArrayList<>(list));
    }

    public void setUpdateProvisionResponseByMappingFunction(boolean b) {
        updateProvisionResponseByMappingFunction.set(b);
    }

    public void setUpdateProvisionRequestToKeyFunction(Function<UpdateProvisionRequest, Object> f) {
        updateProvisionRequestToKeyFunction.set(f);
    }

    public void addAllToUpdateProvisionResponsesMap(Map<Object, GrpcResponse<UpdateProvisionResponse>> map) {
        updateProvisionResponsesMap.putAll(map);
    }

    public void addGetAccountResponse(String accountId, GrpcResponse<Account> response) {
        getAccountResponsesByAccountId.computeIfAbsent(accountId, x -> new ArrayList<>()).add(response);
    }

    public void addGetAccountResponses(String accountId, List<GrpcResponse<Account>> responses) {
        getAccountResponsesByAccountId.computeIfAbsent(accountId, x -> new ArrayList<>()).addAll(responses);
    }
}
