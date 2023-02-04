package ru.yandex.intranet.d.dao.resources

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestProviders.YP_ID
import ru.yandex.intranet.d.TestProviders.YT_ID
import ru.yandex.intranet.d.TestResourceTypes
import ru.yandex.intranet.d.TestSegmentations
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.resources.types.ExchangeableResourceTypesDao
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.resources.types.ExchangeableResourceTypeKey
import ru.yandex.intranet.d.model.resources.types.ExchangeableResourceTypeModel
import ru.yandex.intranet.d.model.resources.types.ExchangeableResourceTypeSegments
import java.util.*

@IntegrationTest
class ExchangeableResourceTypesDaoTest @Autowired constructor(
    private val tableClient: YdbTableClient,
    private val exchangeableResourceTypesDao: ExchangeableResourceTypesDao
) {
    @Test
    fun testGetById(): Unit = runBlocking {
        val model = ExchangeableResourceTypeModel(
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
        dbSessionRetryable(tableClient) {
            exchangeableResourceTypesDao.upsertOneRetryable(rwSingleRetryableCommit(), model)
        }
        val result = dbSessionRetryable(tableClient) {
            exchangeableResourceTypesDao.getById(
                roStaleSingleRetryableCommit(),
                ExchangeableResourceTypeKey(
                    model.key.tenantId, model.key.providerId, model.key.toResourceTypeId, model.key.fromResourceTypeId
                )
            )
        }
        assertEquals(model, result)
    }

    @Test
    fun testGetByIds(): Unit = runBlocking {
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
                rwSingleRetryableCommit(),
                listOf(modelOne, modelTwo, modelThree)
            )
        }
        val result = dbSessionRetryable(tableClient) {
            exchangeableResourceTypesDao.getByIds(
                roStaleSingleRetryableCommit(),
                listOf(
                    ExchangeableResourceTypeKey(
                        modelOne.key.tenantId,
                        modelOne.key.providerId,
                        modelOne.key.toResourceTypeId,
                        modelOne.key.fromResourceTypeId
                    ),
                    ExchangeableResourceTypeKey(
                        modelTwo.key.tenantId,
                        modelTwo.key.providerId,
                        modelTwo.key.toResourceTypeId,
                        modelTwo.key.fromResourceTypeId
                    ),
                )
            )
        }!!
        assertEquals(2, result.size)
        assertTrue(result.contains(modelOne))
        assertTrue(result.contains(modelTwo))
    }

    @Test
    fun testGetByProvider(): Unit = runBlocking {
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
                rwSingleRetryableCommit(),
                listOf(modelOne, modelTwo, modelThree)
            )
        }
        val result = dbSessionRetryable(tableClient) {
            exchangeableResourceTypesDao.getByProvider(
                roStaleSingleRetryableCommit(),
                Tenants.DEFAULT_TENANT_ID,
                YP_ID
            )
        }!!
        assertEquals(2, result.size)
        assertTrue(result.contains(modelOne))
        assertTrue(result.contains(modelTwo))
    }

    @Test
    fun testGetProviders(): Unit = runBlocking {
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
                rwSingleRetryableCommit(),
                listOf(modelOne, modelTwo, modelThree)
            )
        }
        val result = dbSessionRetryable(tableClient) {
            exchangeableResourceTypesDao.getProviders(roStaleSingleRetryableCommit())
        }!!
        assertEquals(2, result.size)
        assertTrue(YP_ID in result)
        assertTrue(YT_ID in result)
    }
}
