package ru.yandex.intranet.d.grpc.aggregation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Component
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.backend.service.proto.AggregatesLimit
import ru.yandex.intranet.d.backend.service.proto.AggregatesPageToken
import ru.yandex.intranet.d.backend.service.proto.AggregationQueriesServiceGrpc
import ru.yandex.intranet.d.backend.service.proto.RankSubtreeAmountsRequest
import ru.yandex.intranet.d.backend.service.proto.RankSubtreeSortingParams
import ru.yandex.intranet.d.backend.service.proto.RankSubtreeSortingField
import ru.yandex.intranet.d.backend.service.proto.RankSubtreeSortingOrder
import ru.yandex.intranet.d.grpc.MockGrpcUser

@Component
class AggregationGrpcApiTestHelper {
    @GrpcClient("inProcess")
    private lateinit var aggregationService: AggregationQueriesServiceGrpc.AggregationQueriesServiceBlockingStub

    suspend fun rankSubtreeAmounts(
        rootServiceId: Long,
        resourceId: String,
        providerId: String,
        from: String?,
        limit: Long?,
        sortingField: RankSubtreeSortingField?,
        sortingOrder: RankSubtreeSortingOrder?
    ) = withContext(Dispatchers.IO) {
        val requestBuilder = RankSubtreeAmountsRequest.newBuilder()
            .setRootServiceId(rootServiceId)
            .setResourceId(resourceId)
            .setProviderId(providerId)

        if (limit != null) {
            requestBuilder.limit = AggregatesLimit.newBuilder()
                .setLimit(limit)
                .build()
        }
        if (from != null) {
            requestBuilder.from = AggregatesPageToken.newBuilder()
                .setToken(from)
                .build()
        }
        if (sortingOrder != null && sortingField != null) {
            requestBuilder.setSortingParams(
                RankSubtreeSortingParams.newBuilder()
                    .setSortingField(sortingField)
                    .setSortingOrder(sortingOrder)
            )
        }

        aggregationService
            .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_1_UID))
            .rankSubtreeAmounts(
                requestBuilder.build()
            )
    }
}

