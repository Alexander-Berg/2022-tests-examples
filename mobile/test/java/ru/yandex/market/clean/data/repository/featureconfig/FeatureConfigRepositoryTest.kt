package ru.yandex.market.clean.data.repository.featureconfig


// TODO https://st.yandex-team.ru/BLUEMARKETAPPS-14890
/*
import com.google.gson.Gson
import com.google.gson.JsonElement
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import ru.yandex.market.clean.data.store.remoteconfig.OverriddenConfigDataStore
import ru.yandex.market.clean.data.store.remoteconfig.RemoteConfigDataStore
import ru.yandex.market.gson.GsonFactory

class FeatureConfigRepositoryTest {

    private val remoteConfigDataStore = mock<RemoteConfigDataStore>()
    private val overriddenConfigDataStore = mock<OverriddenConfigDataStore>()
    private lateinit var gson: Gson

    private val repository by lazy {
        FeatureConfigRepository(
            remoteConfigDataStore = remoteConfigDataStore,
            overriddenConfigDataStore = overriddenConfigDataStore,
            gson = gson
        )
    }

    @Before
    fun setUp() {
        gson = GsonFactory.get()
    }

    @Test
    fun `Returns value from remote if not overridden`() {
        val key = "config_key"
        val value = """
            { "test": 42 }
        """.trimIndent()
        whenever(remoteConfigDataStore.getRemoteConfig(key)) doReturn wrapAndroidConfig(value)
        whenever(overriddenConfigDataStore.getConfigOverride(key)) doReturn null

        val result = repository.fetch(key, allowOverride = true)

        assertThat(result?.source, equalTo(FeatureConfigSource.FIREBASE))
        assertThat(result?.value?.toJsonElement(), equalTo(value.toJsonElement()))
    }

    @Test
    fun `Returns overridden value if set`() {
        val key = "config_key"
        val remoteValue = """
            { "source": "remote" }
        """.trimIndent()
        val overriddenValue = """
            { "source": "override" }
        """.trimIndent()
        whenever(remoteConfigDataStore.getRemoteConfig(key)) doReturn wrapAndroidConfig(remoteValue)
        whenever(overriddenConfigDataStore.getConfigOverride(key)) doReturn overriddenValue

        val result = repository.fetch(key, allowOverride = true)

        assertThat(result?.source, equalTo(FeatureConfigSource.OVERRIDE))
        assertThat(result?.value?.toJsonElement(), equalTo(overriddenValue.toJsonElement()))
    }

    @Test
    fun `Returns value from remote if override not allowed`() {
        val key = "config_key"
        val remoteValue = """
            { "source": "remote" }
        """.trimIndent()
        val overriddenValue = """
            { "source": "override" }
        """.trimIndent()
        whenever(remoteConfigDataStore.getRemoteConfig(key)) doReturn wrapAndroidConfig(remoteValue)
        whenever(overriddenConfigDataStore.getConfigOverride(key)) doReturn overriddenValue

        val result = repository.fetch(key, allowOverride = false)

        assertThat(result?.source, equalTo(FeatureConfigSource.FIREBASE))
        assertThat(result?.value?.toJsonElement(), equalTo(remoteValue.toJsonElement()))
    }

    @Test
    fun `Returns null from remote if android is empty`() {
        val key = "config_key"
        val remoteValue = """
            { "source": "remote" }
        """.trimIndent()
        whenever(remoteConfigDataStore.getRemoteConfig(key)) doReturn wrapIosConfig(remoteValue)
        whenever(overriddenConfigDataStore.getConfigOverride(key)) doReturn null

        val result = repository.fetch(key, allowOverride = false)

        assertThat(result, nullValue())
    }

    @Test
    fun `Returns null from remote if json is invalid`() {
        val key = "config_key"
        val value = "{ 123456 }"
        whenever(remoteConfigDataStore.getRemoteConfig(key)) doReturn value
        whenever(overriddenConfigDataStore.getConfigOverride(key)) doReturn null

        val result = repository.fetch(key, allowOverride = false)

        assertThat(result, nullValue())
    }

    private fun wrapAndroidConfig(androidConfig: String): String {
        return """
            {
                "android": $androidConfig
            }
        """.trimIndent()
    }

    private fun wrapIosConfig(iosConfig: String): String {
        return """
            {
                "ios": $iosConfig
            }
        """.trimIndent()
    }

    private fun String.toJsonElement(): JsonElement? {
        return gson.fromJson(this, JsonElement::class.java)
    }

}*/
