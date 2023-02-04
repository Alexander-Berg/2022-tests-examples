package ru.yandex.intranet.d.web.front.resources

import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestProviders.YP_ID
import ru.yandex.intranet.d.TestProviders.YT_ID
import ru.yandex.intranet.d.TestResourceTypes
import ru.yandex.intranet.d.TestSegmentations
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.resources.types.ExchangeableResourceTypesDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.resources.types.ExchangeableResourceTypeKey
import ru.yandex.intranet.d.model.resources.types.ExchangeableResourceTypeModel
import ru.yandex.intranet.d.model.resources.types.ExchangeableResourceTypeSegments
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.providers.FrontGetProvidersForExchangeResponseDto
import ru.yandex.intranet.d.web.model.resources.exchangeable.FrontExchangeableResourceTypesDto
import ru.yandex.intranet.d.web.model.resources.exchangeable.FrontGetExchangeableResourcesResponseDto
import java.util.*

@IntegrationTest
class ResourcesExchangeControllerTest @Autowired constructor(
    private val webClient: WebTestClient,
    private val exchangeableResourceTypesDao: ExchangeableResourceTypesDao,
    private val tableClient: YdbTableClient
) {
    @Test
    fun testGetExchangeableResourceTypesByProvider(): Unit = runBlocking {
        val modelOne = ExchangeableResourceTypeModel(
            key = ExchangeableResourceTypeKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = YP_ID,
                toResourceTypeId = TestResourceTypes.YP_HDD,
                fromResourceTypeId = TestResourceTypes.YP_HDD
            ),
            numerator = 1L,
            denominator = 1L,
            available_segments = ExchangeableResourceTypeSegments(
                mapOf(
                    TestSegmentations.YP_LOCATION to setOf(
                        TestSegmentations.YP_LOCATION_MAN,
                        TestSegmentations.YP_LOCATION_VLA
                    ),
                    TestSegmentations.YP_SEGMENT to setOf(TestSegmentations.YP_SEGMENT_DEFAULT)
                )
            )
        )
        val modelTwo = ExchangeableResourceTypeModel(
            key = ExchangeableResourceTypeKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = YP_ID,
                toResourceTypeId = TestResourceTypes.YP_SSD,
                fromResourceTypeId = TestResourceTypes.YP_SSD
            ),
            numerator = 1L,
            denominator = 1L,
            available_segments = ExchangeableResourceTypeSegments(
                mapOf(
                    TestSegmentations.YP_LOCATION to setOf(
                        TestSegmentations.YP_LOCATION_MAN,
                        TestSegmentations.YP_LOCATION_VLA
                    ),
                    TestSegmentations.YP_SEGMENT to setOf(TestSegmentations.YP_SEGMENT_DEFAULT)
                )
            )
        )
        val modelThree = ExchangeableResourceTypeModel(
            key = ExchangeableResourceTypeKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = UUID.randomUUID().toString(),
                toResourceTypeId = UUID.randomUUID().toString(),
                fromResourceTypeId = UUID.randomUUID().toString()
            ),
            numerator = 1L,
            denominator = 1L,
            available_segments = ExchangeableResourceTypeSegments(
                mapOf(UUID.randomUUID().toString() to setOf(UUID.randomUUID().toString()))
            )
        )
        dbSessionRetryable(tableClient) {
            exchangeableResourceTypesDao.upsertManyRetryable(
                rwSingleRetryableCommit(), listOf(modelOne, modelTwo, modelThree)
            )
        }
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/exchange/$YP_ID/_exchangeableResourceTypes")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(FrontGetExchangeableResourcesResponseDto::class.java)
            .responseBody
            .awaitSingle()!!
        assertEquals(2, result.exchangeableResourceTypes.size)
        assertTrue(FrontExchangeableResourceTypesDto(modelOne) in result.exchangeableResourceTypes)
        assertTrue(FrontExchangeableResourceTypesDto(modelTwo) in result.exchangeableResourceTypes)
    }

    @Test
    fun testGetExchangeableResourceTypesByProviderEmptyResult(): Unit = runBlocking {
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/exchange/$YP_ID/_exchangeableResourceTypes")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(FrontGetExchangeableResourcesResponseDto::class.java)
            .responseBody
            .awaitSingle()!!
        assertTrue(result.exchangeableResourceTypes.isEmpty())
    }

    @Test
    fun testGetProvidersForResourceExchange(): Unit = runBlocking {
        val modelOne = ExchangeableResourceTypeModel(
            key = ExchangeableResourceTypeKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = YP_ID,
                toResourceTypeId = TestResourceTypes.YP_HDD,
                fromResourceTypeId = TestResourceTypes.YP_HDD
            ),
            numerator = 1L,
            denominator = 1L,
            available_segments = ExchangeableResourceTypeSegments(
                mapOf(
                    TestSegmentations.YP_LOCATION to setOf(
                        TestSegmentations.YP_LOCATION_MAN,
                        TestSegmentations.YP_LOCATION_VLA
                    ),
                    TestSegmentations.YP_SEGMENT to setOf(TestSegmentations.YP_SEGMENT_DEFAULT)
                )
            )
        )
        val modelTwo = ExchangeableResourceTypeModel(
            key = ExchangeableResourceTypeKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = YP_ID,
                toResourceTypeId = TestResourceTypes.YP_SSD,
                fromResourceTypeId = TestResourceTypes.YP_SSD
            ),
            numerator = 1L,
            denominator = 1L,
            available_segments = ExchangeableResourceTypeSegments(
                mapOf(
                    TestSegmentations.YP_LOCATION to setOf(
                        TestSegmentations.YP_LOCATION_MAN,
                        TestSegmentations.YP_LOCATION_VLA
                    ),
                    TestSegmentations.YP_SEGMENT to setOf(TestSegmentations.YP_SEGMENT_DEFAULT)
                )
            )
        )
        val modelThree = ExchangeableResourceTypeModel(
            key = ExchangeableResourceTypeKey(
                tenantId = Tenants.DEFAULT_TENANT_ID,
                providerId = YT_ID,
                toResourceTypeId = UUID.randomUUID().toString(),
                fromResourceTypeId = UUID.randomUUID().toString()
            ),
            numerator = 1L,
            denominator = 1L,
            available_segments = ExchangeableResourceTypeSegments(
                mapOf(UUID.randomUUID().toString() to setOf(UUID.randomUUID().toString()))
            )
        )
        dbSessionRetryable(tableClient) {
            exchangeableResourceTypesDao.upsertManyRetryable(
                rwSingleRetryableCommit(), listOf(modelOne, modelTwo, modelThree)
            )
        }
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/exchange/_providers")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(FrontGetProvidersForExchangeResponseDto::class.java)
            .responseBody
            .awaitSingle()!!
        assertEquals(2, result.providers.size)
        assertTrue(YP_ID in result.providers.keys)
        assertTrue(YT_ID in result.providers.keys)
    }

    @Test
    fun testGetProvidersForResourceExchangeEmptyResult(): Unit = runBlocking {
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/exchange/_providers")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(FrontGetProvidersForExchangeResponseDto::class.java)
            .responseBody
            .awaitSingle()!!
        assertTrue(result.providers.isEmpty())
    }
}
