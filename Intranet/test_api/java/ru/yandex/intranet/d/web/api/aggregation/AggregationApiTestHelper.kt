package ru.yandex.intranet.d.web.api.aggregation

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.aggregation.RankSubtreeSortingParamsDto
import ru.yandex.intranet.d.web.model.aggregation.api.RankSubtreeAmountsApiRequestDto
import ru.yandex.intranet.d.web.model.aggregation.api.RankSubtreeAmountsApiResponseDto

@Component
class AggregationApiTestHelper {
    @Autowired
    lateinit var webClient: WebTestClient

    suspend fun rankSubtreeAmounts(
        rootServiceId: Long,
        resourceId: String,
        providerId: String,
        from: String?,
        limit: Long,
        sortingParams: RankSubtreeSortingParamsDto,
        includeUsage: Boolean?,
        includeUsageRaw: Boolean?
    ): RankSubtreeAmountsApiResponseDto? = webClient
        .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
        .post()
        .uri("/api/v1/aggregation/_rankSubtreeAmounts")
        .bodyValue(
            RankSubtreeAmountsApiRequestDto(
                rootServiceId,
                resourceId,
                providerId,
                from,
                limit,
                sortingParams,
                includeUsage,
                includeUsageRaw
            )
        )
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(RankSubtreeAmountsApiResponseDto::class.java)
        .responseBody
        .awaitSingle()
}
