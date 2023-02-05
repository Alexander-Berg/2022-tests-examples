package com.edadeal.android.model.webapp

import android.graphics.Rect
import com.edadeal.android.data.endpoints.EndpointsRepository
import com.edadeal.android.metrics.YandexKit
import com.edadeal.android.model.EnvironmentInfoProvider
import com.edadeal.android.model.EnvironmentInfoProvider.Item
import com.edadeal.android.model.api.endpoints.Endpoints
import com.edadeal.android.model.auth.passport.PassportContext
import com.edadeal.android.model.calibrator.Configs
import com.edadeal.android.model.macros.PlaceHolder
import com.edadeal.android.model.macros.PlaceholderResolver
import com.edadeal.android.ui.common.webapp.WebAppPermission
import com.edadeal.android.util.DefaultUrls
import com.edadeal.platform.JsonRepresentable
import com.edadeal.platform.JsonValue
import com.edadeal.platform.WebAppConfig
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import io.reactivex.Single
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class JsConfigProviderTest {

    @Mock
    private lateinit var viewport: Rect
    @Mock
    private lateinit var configs: Configs
    @Mock
    private lateinit var yandexKit: YandexKit
    @Mock
    private lateinit var webAppConfig: WebAppConfig
    @Mock
    private lateinit var envInfoProvider: EnvironmentInfoProvider
    @Mock
    private lateinit var permissionProvider: WebAppPermissionStateProvider
    @Mock
    private lateinit var endpointsRepository: EndpointsRepository
    @Mock
    private lateinit var placeholderResolver: PlaceholderResolver

    private val osVersion = "10.0"
    private val appVersion = "5.9.0"
    private val yandexUid = "7067432912203667774"
    private val duid = "2DEC9C1A-F27B-4592-BEB1-0B315C4A2A17"
    private val abt = "{\"CONTEXT\":{\"EDADEAL_UI_WV\":{\"key\":\"value\"}}}"
    private val navigationInfoProvider = NavigationInfoProvider(stack = "@edadeal/webapp", historyLength = 1)
    private val methods = Config.Support.Methods(
        remote = hashSetOf("analytics.event", "storage.get"),
        local = JsMessage.values().mapTo(HashSet()) { it.id }
    )
    private val permissions = listOf(
        WebAppPermission.geolocation to WebAppPermission.State.prompt,
        WebAppPermission.notifications to WebAppPermission.State.granted,
    )
    private val endpoints = Endpoints.create(DefaultUrls.Edadeal, PassportContext.PRODUCTION)
    private val yaUuid = "befg3e31ae074af9b062548302da486d"
    private val deviceModel = "SM-A515F"
    private val deviceManufacturer = "SAMSUNG"
    private val yaDeviceId = "d8b7d6a0ab29531a32836c0d77c31176"
    private val uid = "7f801276-1d8s-4235-b6c3-98dd787fc632"
    private val puid = "133542131"

    @BeforeTest
    fun prepare() {
        whenever(viewport.width()).thenReturn(320)
        whenever(viewport.height()).thenReturn(720)

        whenever(webAppConfig.manifest).thenReturn("{}")

        whenever(configs.getConfigsJson()).thenReturn(Single.just(JsonValue("{}")))

        whenever(envInfoProvider.getItemValue(eq(Item.DUID))).thenReturn(duid)
        whenever(envInfoProvider.getItemValue(eq(Item.OS_VERSION))).thenReturn(osVersion)
        whenever(envInfoProvider.getItemValue(eq(Item.APP_VERSION))).thenReturn(appVersion)
        whenever(envInfoProvider.getItemValue(eq(Item.ABT))).thenReturn(abt)
        whenever(envInfoProvider.getItemValue(eq(Item.UID))).thenReturn(uid)
        whenever(yandexKit.getRtmUserId()).thenReturn(yandexUid)

        whenever(permissionProvider.getWebAppPermissionStates()).thenReturn(permissions)

        whenever(endpointsRepository.endpoints).thenReturn(endpoints)
        whenever(placeholderResolver.getPlaceholderValue(eq(PlaceHolder.YaUuid))).thenReturn(yaUuid)
        whenever(placeholderResolver.getPlaceholderValue(eq(PlaceHolder.DeviceManufacturer))).thenReturn(deviceManufacturer)
        whenever(placeholderResolver.getPlaceholderValue(eq(PlaceHolder.DeviceModel))).thenReturn(deviceModel)
        whenever(placeholderResolver.getPlaceholderValue(eq(PlaceHolder.YaDeviceId))).thenReturn(yaDeviceId)
        whenever(placeholderResolver.getPlaceholderValue(eq(PlaceHolder.Puid))).thenReturn(puid)
    }

    @Test
    fun `config provider should return correct json`() {
        val expectedConfig = Config(
            manifest = emptyMap(),
            prefs = mapOf("isDebug" to true),
            ui = Config.Ui(
                viewport = Config.Ui.Viewport(viewport.width(), viewport.height())
            ),
            navigation = Config.Navigation(
                stack = navigationInfoProvider.stack,
                historyLength = navigationInfoProvider.historyLength
            ),
            environment = Config.Environment(
                duid = duid,
                abt = fromJson(abt),
                config = emptyMap(),
                osVersion = osVersion,
                appVersion = appVersion,
                yandexUid = yandexUid,
                yaUuid = yaUuid,
                deviceManufacturer = deviceManufacturer,
                deviceModel = deviceModel,
                yaDeviceId = yaDeviceId
            ),
            transient = Config.Transient(Config.Transient.Environment(uid, puid)),
            support = Config.Support(
                methods = methods,
                fixes = JsConfigProviderFactory.Support.fixes.toSet(),
                features = JsConfigProviderFactory.Support.features.toSet()
            ),
            system = mapOf("permissions" to permissions.map { it.first.name to it.second.name }.toMap())
        )
        val configProviderFactory = JsConfigProviderFactoryImpl(
            configs, yandexKit,
            environmentInfoProvider = envInfoProvider,
            getConfigPrefs = { "{\"isDebug\":true}" },
            endpointsRepository = endpointsRepository,
            permissionStateProvider = permissionProvider,
            placeholderResolver = placeholderResolver
        )
        val configProvider = configProviderFactory.createProvider(
            viewport = viewport,
            config = webAppConfig,
            methods = methods.remote,
            navigationInfoProvider = navigationInfoProvider
        )

        val actualConfigJson = configProvider.getConfig(extras = emptyMap()).toJson()
        val actualConfig = fromJson<Config>(actualConfigJson)
        assertEquals(expectedConfig, actualConfig)
    }

    private inline fun <reified T> fromJson(json: String): T {
        val adapter = Moshi.Builder().build().adapter<T>(T::class.java)
        return adapter.fromJson(json) ?: throw JsonDataException()
    }

    class NavigationInfoProvider(
        val stack: String,
        val historyLength: Int
    ) : JsNavigationInfoProvider {

        override fun getNavigationInfo(): JsonRepresentable {
            return NavigationInfo(
                stackSlugJson = JsonValue.fromString(stack).toJson(),
                historyLength = historyLength
            )
        }
    }

    @JsonClass(generateAdapter = true)
    data class Config(
        val ui: Ui,
        val support: Support,
        val navigation: Navigation,
        val environment: Environment,
        val transient: Transient,
        val prefs: Map<String, Any>,
        val manifest: Map<String, Any>,
        val system: Map<String, Any>
    ) {

        @JsonClass(generateAdapter = true)
        data class Navigation(
            val stack: String,
            val historyLength: Int
        )

        @JsonClass(generateAdapter = true)
        data class Ui(
            val viewport: Viewport
        ) {

            @JsonClass(generateAdapter = true)
            data class Viewport(
                val initialWidth: Int,
                val initialHeight: Int
            )
        }

        @JsonClass(generateAdapter = true)
        data class Environment(
            val appVersion: String,
            val osVersion: String,
            val duid: String,
            val abt: Abt,
            val config: Map<String, Any>,
            val yandexUid: String,
            val yaUuid: String,
            val deviceManufacturer: String,
            val deviceModel: String,
            val yaDeviceId: String
        ) {

            @JsonClass(generateAdapter = true)
            data class Abt(
                val CONTEXT: Context
            ) {

                @JsonClass(generateAdapter = true)
                data class Context(
                    val EDADEAL_UI_WV: Map<String, String>
                )
            }
        }

        @JsonClass(generateAdapter = true)
        data class Support(
            val methods: Methods,
            val fixes: Set<String>,
            val features: Set<String>
        ) {

            @JsonClass(generateAdapter = true)
            data class Methods(
                val local: Set<String>,
                val remote: Set<String>
            )
        }

        @JsonClass(generateAdapter = true)
        data class Transient(
            val environment: Environment
        ) {

            @JsonClass(generateAdapter = true)
            data class Environment(
                val uid: String,
                val puid: String
            )
        }
    }
}
