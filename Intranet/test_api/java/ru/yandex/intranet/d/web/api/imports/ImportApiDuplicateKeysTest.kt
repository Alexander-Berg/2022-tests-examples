package ru.yandex.intranet.d.web.api.imports

import com.yandex.ydb.table.transaction.TransactionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.dao.resources.segmentations.ResourceSegmentationsDao
import ru.yandex.intranet.d.dao.resources.segments.ResourceSegmentsDao
import ru.yandex.intranet.d.dao.resources.types.ResourceTypesDao
import ru.yandex.intranet.d.datasource.model.YdbSession
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.datasource.model.YdbTxSession
import ru.yandex.intranet.d.model.TenantId
import ru.yandex.intranet.d.model.resources.segmentations.ResourceSegmentationModel
import ru.yandex.intranet.d.model.resources.segments.ResourceSegmentModel
import ru.yandex.intranet.d.model.resources.types.ResourceTypeModel
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.ErrorCollectionDto
import ru.yandex.intranet.d.web.model.imports.AccountSpaceIdentityDto
import ru.yandex.intranet.d.web.model.imports.ImportAccountDto
import ru.yandex.intranet.d.web.model.imports.ImportAccountProvisionDto
import ru.yandex.intranet.d.web.model.imports.ImportDto
import ru.yandex.intranet.d.web.model.imports.ImportFolderDto
import ru.yandex.intranet.d.web.model.imports.ImportResourceDto
import ru.yandex.intranet.d.web.model.imports.ImportResultDto
import ru.yandex.intranet.d.web.model.imports.ResourceIdentityDto
import ru.yandex.intranet.d.web.model.imports.SegmentKey

/**
 * Import API duplicate keys test.
 *
 * @author Petr Surkov <petrsurkov@yandex-team.ru>
 */
@IntegrationTest
class ImportApiDuplicateKeysTest {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var quotasDao: QuotasDao

    @Autowired
    private lateinit var tableClient: YdbTableClient

    @Autowired
    private lateinit var resourceSegmentationsDao: ResourceSegmentationsDao

    @Autowired
    private lateinit var resourceSegmentsDao: ResourceSegmentsDao

    @Autowired
    private lateinit var resourceTypesDao: ResourceTypesDao

    @Test
    fun `Importing a resource with an existing resource type key`() {
        val resourceType = ResourceTypeModel(
            "e879ff48-f098-4b5d-806c-eff968dab296",
            TenantId("7c8749f6-92f2-4bd3-82b9-6ed98125d517"),
            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
            0,
            "hdd", // duplicate
            "HDD-key-duplicate",
            "HDD-key-duplicate",
            "HDD-key-duplicate",
            "HDD-key-duplicate",
            false,
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            null,
            null,
            null
        )
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        val accountSpaceIdentityDto = AccountSpaceIdentityDto(
            "978bd75a-cf67-44ac-b944-e8ca949bdf7e",
            null
        )
        val resourceIdentityDto = ResourceIdentityDto(
            "hdd",
            accountSpaceIdentityDto,
            null
        )
        val quotasToImport = ImportDto(
            listOf(
                ImportFolderDto(
                    "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                    null,
                    listOf(
                        ImportResourceDto(
                            null,
                            resourceIdentityDto,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            100L,
                            "gigabytes",
                            50L,
                            "gigabytes"
                        )
                    ),
                    listOf(
                        ImportAccountDto(
                            "1",
                            "test",
                            "Test",
                            false,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            listOf(
                                ImportAccountProvisionDto(
                                    null,
                                    resourceIdentityDto,
                                    50L,
                                    "gigabytes",
                                    25L,
                                    "gigabytes"
                                )
                            ),
                            accountSpaceIdentityDto,
                            false)
                    )
                )
            )
        )
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/import/_importQuotas")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(quotasToImport)
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(result)
    }

    @Test
    fun `Importing a resource with an existing but deleted resource type key`() {
        val resourceType = ResourceTypeModel(
            "e879ff48-f098-4b5d-806c-eff968dab296",
            TenantId("7c8749f6-92f2-4bd3-82b9-6ed98125d517"),
            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
            0,
            "hdd", // duplicate
            "HDD-deleted-key-duplicate",
            "HDD-deleted-key-duplicate",
            "HDD-deleted-key-duplicate",
            "HDD-deleted-key-duplicate",
            true,
            "b02344bf-96af-4cc5-937c-66a479989ce8",
            null,
            null,
            null
        )
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                resourceTypesDao.upsertResourceTypeRetryable(txSession, resourceType)
            }
        }.block()
        val accountSpaceIdentityDto = AccountSpaceIdentityDto(
            "978bd75a-cf67-44ac-b944-e8ca949bdf7e",
            null
        )
        val resourceIdentityDto = ResourceIdentityDto(
            "hdd",
            accountSpaceIdentityDto,
            null
        )
        val quotasToImport = ImportDto(
            listOf(
                ImportFolderDto(
                    "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                    null,
                    listOf(
                        ImportResourceDto(
                            null,
                            resourceIdentityDto,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            100L,
                            "gigabytes",
                            50L,
                            "gigabytes"
                        )
                    ),
                    listOf(
                        ImportAccountDto(
                            "1",
                            "test",
                            "Test",
                            false,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            listOf(
                                ImportAccountProvisionDto(
                                    null,
                                    resourceIdentityDto,
                                    50L,
                                    "gigabytes",
                                    25L,
                                    "gigabytes"
                                )
                            ),
                            accountSpaceIdentityDto,
                            false)
                    )
                )
            )
        )
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/import/_importQuotas")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(quotasToImport)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ImportResultDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.importFailures.isEmpty())
        Assertions.assertEquals(1, result.successfullyImported.size)
        val newQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { ts: YdbTxSession? ->
                quotasDao.getByFolders(ts, listOf("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1"),
                    Tenants.DEFAULT_TENANT_ID)
            }
        }.block()
        Assertions.assertNotNull(newQuotas)
        Assertions.assertEquals(1, newQuotas!!.size)
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", newQuotas[0].folderId)
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", newQuotas[0].providerId)
        Assertions.assertEquals("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", newQuotas[0].resourceId)
    }

    @Test
    fun `Importing a resource with an existing segment key`() {
        val resourceSegment = ResourceSegmentModel(
            "09d2b350-879a-4ac4-b439-eebfaf682982",
            TenantId("7c8749f6-92f2-4bd3-82b9-6ed98125d517"),
            "7fbd778f-d803-44c8-831a-c1de5c05885c",
            0,
            "sas", // duplicate
            "sas-key-duplicate",
            "sas-key-duplicate",
            "sas-key-duplicate",
            "sas-key-duplicate",
            false,
            false
        )
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, resourceSegment)
            }
        }.block()
        val segments = listOf(
            SegmentKey("location", "sas"),
            SegmentKey("segment", "default")
        )
        val accountSpaceIdentityDto = AccountSpaceIdentityDto(
            null,
            segments
        )
        val resourceIdentityDto = ResourceIdentityDto(
            "hdd",
            accountSpaceIdentityDto,
            segments
        )
        val quotasToImport = ImportDto(
            listOf(
                ImportFolderDto(
                    "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                    null,
                    listOf(
                        ImportResourceDto(
                            null,
                            resourceIdentityDto,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            100L,
                            "gigabytes",
                            50L,
                            "gigabytes"
                        )
                    ),
                    listOf(
                        ImportAccountDto(
                            "1",
                            "test",
                            "Test",
                            false,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            listOf(
                                ImportAccountProvisionDto(
                                    null,
                                    resourceIdentityDto,
                                    50L,
                                    "gigabytes",
                                    25L,
                                    "gigabytes"
                                )
                            ),
                            accountSpaceIdentityDto,
                            false)
                    )
                )
            )
        )
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/import/_importQuotas")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(quotasToImport)
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(result)
    }

    @Test
    fun `Importing a resource with an existing but deleted segment key`() {
        val resourceSegment = ResourceSegmentModel(
            "09d2b350-879a-4ac4-b439-eebfaf682982",
            TenantId("7c8749f6-92f2-4bd3-82b9-6ed98125d517"),
            "7fbd778f-d803-44c8-831a-c1de5c05885c",
            0,
            "sas", // duplicate
            "sas-key-duplicate",
            "sas-key-duplicate",
            "sas-key-duplicate",
            "sas-key-duplicate",
            true,
            false
        )
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                resourceSegmentsDao.upsertResourceSegmentRetryable(txSession, resourceSegment)
            }
        }.block()
        val segments = listOf(
            SegmentKey("location", "sas"),
            SegmentKey("segment", "default")
        )
        val accountSpaceIdentityDto = AccountSpaceIdentityDto(
            null,
            segments
        )
        val resourceIdentityDto = ResourceIdentityDto(
            "hdd",
            accountSpaceIdentityDto,
            segments
        )
        val quotasToImport = ImportDto(
            listOf(
                ImportFolderDto(
                    "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                    null,
                    listOf(
                        ImportResourceDto(
                            null,
                            resourceIdentityDto,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            100L,
                            "gigabytes",
                            50L,
                            "gigabytes"
                        )
                    ),
                    listOf(
                        ImportAccountDto(
                            "1",
                            "test",
                            "Test",
                            false,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            listOf(
                                ImportAccountProvisionDto(
                                    null,
                                    resourceIdentityDto,
                                    50L,
                                    "gigabytes",
                                    25L,
                                    "gigabytes"
                                )
                            ),
                            accountSpaceIdentityDto,
                            false)
                    )
                )
            )
        )
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/import/_importQuotas")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(quotasToImport)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ImportResultDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.importFailures.isEmpty())
        Assertions.assertEquals(1, result.successfullyImported.size)
        val newQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { ts: YdbTxSession? ->
                quotasDao.getByFolders(ts, listOf("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1"),
                    Tenants.DEFAULT_TENANT_ID)
            }
        }.block()
        Assertions.assertNotNull(newQuotas)
        Assertions.assertEquals(1, newQuotas!!.size)
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", newQuotas[0].folderId)
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", newQuotas[0].providerId)
        Assertions.assertEquals("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", newQuotas[0].resourceId)
    }

    @Test
    fun `Importing a resource with an existing segmentation key`() {
        val resourceSegmentation = ResourceSegmentationModel(
            "752fb425-e29a-450a-b4dc-3b983ed0f2c6",
            TenantId("7c8749f6-92f2-4bd3-82b9-6ed98125d517"),
            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
            0,
            "location", // duplicate
            "location-key-duplicate",
            "location-key-duplicate",
            "location-key-duplicate",
            "location-key-duplicate",
            false,
            0,
            null
        )
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, resourceSegmentation)
            }
        }.block()
        val segments = listOf(
            SegmentKey("location", "sas"),
            SegmentKey("segment", "default")
        )
        val accountSpaceIdentityDto = AccountSpaceIdentityDto(
            null,
            segments
        )
        val resourceIdentityDto = ResourceIdentityDto(
            "hdd",
            accountSpaceIdentityDto,
            segments
        )
        val quotasToImport = ImportDto(
            listOf(
                ImportFolderDto(
                    "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                    null,
                    listOf(
                        ImportResourceDto(
                            null,
                            resourceIdentityDto,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            100L,
                            "gigabytes",
                            50L,
                            "gigabytes"
                        )
                    ),
                    listOf(
                        ImportAccountDto(
                            "1",
                            "test",
                            "Test",
                            false,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            listOf(
                                ImportAccountProvisionDto(
                                    null,
                                    resourceIdentityDto,
                                    50L,
                                    "gigabytes",
                                    25L,
                                    "gigabytes"
                                )
                            ),
                            accountSpaceIdentityDto,
                            false)
                    )
                )
            )
        )
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/import/_importQuotas")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(quotasToImport)
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(result)
    }

    @Test
    fun `Importing a resource with an existing but deleted segmentation key`() {
        val resourceSegmentation = ResourceSegmentationModel(
            "752fb425-e29a-450a-b4dc-3b983ed0f2c6",
            TenantId("7c8749f6-92f2-4bd3-82b9-6ed98125d517"),
            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
            0,
            "location", // duplicate
            "location-key-duplicate",
            "location-key-duplicate",
            "location-key-duplicate",
            "location-key-duplicate",
            true,
            0,
            null
        )
        tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                resourceSegmentationsDao.upsertResourceSegmentationRetryable(txSession, resourceSegmentation)
            }
        }.block()
        val segments = listOf(
            SegmentKey("location", "sas"),
            SegmentKey("segment", "default")
        )
        val accountSpaceIdentityDto = AccountSpaceIdentityDto(
            null,
            segments
        )
        val resourceIdentityDto = ResourceIdentityDto(
            "hdd",
            accountSpaceIdentityDto,
            segments
        )
        val quotasToImport = ImportDto(
            listOf(
                ImportFolderDto(
                    "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1",
                    null,
                    listOf(
                        ImportResourceDto(
                            null,
                            resourceIdentityDto,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            100L,
                            "gigabytes",
                            50L,
                            "gigabytes"
                        )
                    ),
                    listOf(
                        ImportAccountDto(
                            "1",
                            "test",
                            "Test",
                            false,
                            "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                            listOf(
                                ImportAccountProvisionDto(
                                    null,
                                    resourceIdentityDto,
                                    50L,
                                    "gigabytes",
                                    25L,
                                    "gigabytes"
                                )
                            ),
                            accountSpaceIdentityDto,
                            false)
                    )
                )
            )
        )
        val result = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/import/_importQuotas")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(quotasToImport)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ImportResultDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.importFailures.isEmpty())
        Assertions.assertEquals(1, result.successfullyImported.size)
        val newQuotas = tableClient.usingSessionMonoRetryable { session: YdbSession ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { ts: YdbTxSession? ->
                quotasDao.getByFolders(ts, listOf("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1"),
                    Tenants.DEFAULT_TENANT_ID)
            }
        }.block()
        Assertions.assertNotNull(newQuotas)
        Assertions.assertEquals(1, newQuotas!!.size)
        Assertions.assertEquals("2e8f9b1c-7b3a-41ba-9067-2dee22b046a1", newQuotas[0].folderId)
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", newQuotas[0].providerId)
        Assertions.assertEquals("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", newQuotas[0].resourceId)
    }
}

