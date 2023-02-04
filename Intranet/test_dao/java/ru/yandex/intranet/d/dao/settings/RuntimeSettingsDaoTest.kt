package ru.yandex.intranet.d.dao.settings

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.datasource.dbSessionRetryable
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.settings.KnownProviders
import ru.yandex.intranet.d.model.settings.SettingsKey
import ru.yandex.intranet.d.model.settings.YtUsageSyncSettings
import java.util.*

/**
 * Runtime settings DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class RuntimeSettingsDaoTest(@Autowired private val dao: RuntimeSettingsDao,
                             @Autowired private val tableClient: YdbTableClient) {

    @Test
    fun testUpsert(): Unit = runBlocking {
        val settingsOne = KnownProviders(UUID.randomUUID().toString(), UUID.randomUUID().toString(), null)
        val settingsTwo = YtUsageSyncSettings(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
            UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
            UUID.randomUUID().toString(), true, 300L, UUID.randomUUID().toString(), UUID.randomUUID().toString())
        val (resultOne, resultTwo) = dbSessionRetryable(tableClient) {
            rwTxRetryable {
                dao.upsertOneRetryable(txSession, Tenants.DEFAULT_TENANT_ID, SettingsKey.KNOWN_PROVIDERS, settingsOne)
                dao.upsertOneRetryable(txSession, Tenants.DEFAULT_TENANT_ID, SettingsKey.YT_USAGE_SYNC_SETTINGS, settingsTwo)
            }
            rwTxRetryable {
                val resultFirst = dao.get(txSession, Tenants.DEFAULT_TENANT_ID, SettingsKey.KNOWN_PROVIDERS)
                val resultSecond = dao.get(txSession, Tenants.DEFAULT_TENANT_ID, SettingsKey.YT_USAGE_SYNC_SETTINGS)
                Pair(resultFirst, resultSecond)
            }
        }!!
        Assertions.assertEquals(settingsOne, resultOne)
        Assertions.assertEquals(settingsTwo, resultTwo)
    }

}
