package ru.yandex.intranet.d.services.sync

import com.yandex.ydb.table.transaction.TransactionMode.SERIALIZABLE_READ_WRITE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestProviders
import ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID
import ru.yandex.intranet.d.dao.sync.ProvidersSyncStatusDao
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.i18n.Locales
import ru.yandex.intranet.d.loaders.providers.ProvidersLoader
import ru.yandex.intranet.d.loaders.providers.ProvidersSyncStatusHolder
import ru.yandex.intranet.d.metrics.ProvidersSyncMetrics
import ru.yandex.intranet.d.model.sync.ProvidersSyncStatusModel
import ru.yandex.intranet.d.model.sync.ProvidersSyncStatusModel.SyncStatuses.DONE_ERROR
import ru.yandex.intranet.d.model.sync.ProvidersSyncStatusModel.SyncStatuses.DONE_OK
import ru.yandex.intranet.d.services.providers.ProvidersService
import ru.yandex.intranet.d.services.sync.AccountsSyncUtils.SYNC_DELAY
import ru.yandex.intranet.d.util.result.Result
import ru.yandex.intranet.d.web.model.providers.ProviderSyncStatusDto
import java.time.Clock
import java.time.Instant
import java.util.*
import ru.yandex.intranet.d.web.model.providers.ProviderSyncStatusDto.SyncStatuses.DONE_OK as DONE_OK_DTO

/**
 * SyncTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 20-04-2021
 */
@IntegrationTest
class SyncTest {
    @Autowired
    private lateinit var syncTestHelper: SyncTestHelper

    @Autowired
    private lateinit var accountsSyncService: AccountsSyncService

    @Autowired
    private lateinit var tableClient: YdbTableClient

    @Autowired
    private lateinit var providersLoader: ProvidersLoader

    @Autowired
    private lateinit var providersSyncStatusDao: ProvidersSyncStatusDao

    @Autowired
    private lateinit var statusHolder: ProvidersSyncStatusHolder

    @Autowired
    private lateinit var providersSyncMetrics: ProvidersSyncMetrics

    @Autowired
    private lateinit var providersService: ProvidersService

    @Test
    fun testSyncFail() {
        val provider = providersLoader.getProviderByIdImmediate(TestProviders.MDB_ID, DEFAULT_TENANT_ID).block()!!.get()

        accountsSyncService.syncOneProvider(provider, Locales.ENGLISH, Clock.systemUTC()).block()

        val status: ProvidersSyncStatusModel? = tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(SERIALIZABLE_READ_WRITE) { txSession ->
                providersSyncStatusDao.getById(txSession, provider.id, DEFAULT_TENANT_ID)
            }
        }.block()?.get()

        assertEquals(DONE_ERROR, status?.lastSyncStatus)
    }

    @Test
    fun testSyncStat() {
        syncTestHelper.initProviderStub { provider, user ->
            val status = accountsSyncService.syncOneProvider(provider, Locales.ENGLISH, Clock.systemUTC()).block()

            assertEquals(DONE_OK, status?.lastSyncStatus)

            statusHolder.refresh()

            assertEquals(1L, providersSyncMetrics.getAccountsCount(provider.key).get())

            val syncStatusResult: Result<ProviderSyncStatusDto> = providersService.getSyncStatus(
                provider.id, user, Locale.US
            ).block()!!

            assertTrue(syncStatusResult.isSuccess())
            syncStatusResult.doOnSuccess {
                assertEquals(DONE_OK_DTO, it.lastSyncStatus)
                assertEquals(1, it.accountsCount)
                assertEquals(1, it.quotasCount)
            }
        }
    }

    @Test
    fun testIsCoolDown() {
        val now = Instant.now()

        assertTrue(AccountsSyncUtils.isCoolDown(now, now.minusNanos(1)))
        assertTrue(AccountsSyncUtils.isCoolDown(now, now.minus(SYNC_DELAY)))
        assertTrue(AccountsSyncUtils.isCoolDown(now, now.minus(SYNC_DELAY).minusNanos(1)))
        assertTrue(AccountsSyncUtils.isCoolDown(now, now.plus(SYNC_DELAY).minusNanos(1)))

        assertFalse(AccountsSyncUtils.isCoolDown(now, now.plus(SYNC_DELAY).plusNanos(1)))
        assertFalse(AccountsSyncUtils.isCoolDown(now, now.plus(SYNC_DELAY).plusSeconds(100)))
    }
}
