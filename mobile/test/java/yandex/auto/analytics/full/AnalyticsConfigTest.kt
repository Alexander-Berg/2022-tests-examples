package ru.yandex.yandexnavi.analytics.full

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.os.YapDevice
import androidx.core.content.ContextCompat
import com.crashlytics.android.Crashlytics
import com.yandex.metrica.YandexMetrica
import com.yandex.metrica.YandexMetricaInternal
import com.yandex.metrica.YandexMetricaInternalConfig
import com.yandex.metrica.profile.UserProfile
import io.fabric.sdk.android.Fabric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import ru.yandex.yandexnavi.analytics.Analytics
import ru.yandex.yap.sysutils.MultipleUsers
import yandex.auto.ownerdeviceidsdk.OwnerDeviceIdSdk

class AnalyticsConfigTest {

    companion object {
        private const val KEY_HEAD_ID = "HeadId"
        private const val KEY_OWNER_DEVICE_ID = "OwnerDeviceId"

        private const val KEY_VENDOR = "vendor"
        private const val KEY_MODEL = "model"
        private const val KEY_TYPE = "type"
        private const val KEY_MCU = "mcu"

        private const val KEY_FIRMWARE_BUILD_NUMBER = "FirmwareBuildNumber"
        private const val KEY_FIRMWARE_BUILD_DATE_UTC = "FirmwareBuildDateUtc"

        private const val KEY_IS_BOOTER = "IsBooter"
        private const val KEY_FLAVOR = "flavor"
        private const val KEY_BUILD_TIME = "BuildTime"

        private const val headId = "111111111111"
        private const val ownerDeviceId = "22222222222222222222222222222222"
        private const val firmwareBuildNumber = "123456789"
        private const val firmwareBuildDate = "08.10.2019"

        private const val apiKey = "testapikey"
        private const val version = "123.45.67"
        private const val flavor = "internal"
        private const val buildTime = "12.12.2019"

        private const val UNKNOWN_VALUE = "unknown"
        private const val EMPTY_FIRMWARE = "null"
    }

    private lateinit var application: Application
    private lateinit var context: Context
    private lateinit var activityManager: ActivityManager

    private val expectedLimitedParams = mutableMapOf(
        KEY_HEAD_ID to headId,
        KEY_OWNER_DEVICE_ID to ownerDeviceId,
        KEY_FIRMWARE_BUILD_NUMBER to firmwareBuildNumber,
        KEY_FIRMWARE_BUILD_DATE_UTC to firmwareBuildDate
    )

    private val expectedExtendedParams = mutableMapOf(
        KEY_VENDOR to "test_vendor",
        KEY_MODEL to "test_model",
        KEY_TYPE to "carsharing",
        KEY_MCU to "test_mcu"
    ).apply { putAll(expectedLimitedParams) }

    private val expectedBoolParams = mutableMapOf(KEY_IS_BOOTER to false)

    @Before
    fun setup() {
        application = mock(Application::class.java)
        context = mock(Context::class.java)
        activityManager = mock(ActivityManager::class.java)

        `when`(application.applicationContext).thenReturn(context)
        `when`(application.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(activityManager)
        `when`(application.packageName).thenReturn("yandex.auto")
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.mainLooper).thenReturn(Looper.getMainLooper())

        YapDevice.prepare(
            headId,
            vendor = "test_vendor",
            model = "test_model",
            type = "carsharing",
            mcu = "test_mcu",
            firmwareBuildNumber = firmwareBuildNumber,
            firmwareBuildDate = firmwareBuildDate
        )
        OwnerDeviceIdSdk.prepare(ownerDeviceId)

        Crashlytics.clear()
        YandexMetricaInternalConfig.clear()
        UserProfile.clear()
        YandexMetricaInternal.clear()
        YandexMetrica.clear()
        Fabric.clear()
        ActivityManagerMock.reset()
        ContextCompat.reset()
        MultipleUsers.reset()
    }

    @Test
    fun `when analytics config initialized then crashlytics initialized with correct params`() {
        initializeAndCheck(createConfig())

        checkParams(
            expectedExtendedParams.apply {
                put(KEY_FLAVOR, "internal")
                put(KEY_BUILD_TIME, "12.12.2019")
            },
            Crashlytics.params
        )
        checkParams(expectedBoolParams, Crashlytics.boolParams)
    }

    @Test
    fun `when analytics config initialized then metrica environment initialized with correct params`() {
        initializeAndCheck(createConfig())

        checkParams(
            expectedLimitedParams.apply {
                put(KEY_IS_BOOTER, "false")
            },
            YandexMetricaInternalConfig.params
        )
    }

    @Test
    fun `when analytics config initialized then metrica user profile initialized with correct params`() {
        initializeAndCheck(createConfig())

        checkParams(expectedExtendedParams, UserProfile.params)
        checkParams(expectedBoolParams, UserProfile.boolParams)
    }

    @Test
    fun `when optional params are not provided then they do not appear in analytics params`() {
        initializeAndCheck(createConfig(flavor = null, buildTime = null))

        checkParams(expectedExtendedParams, Crashlytics.params)
        checkParams(expectedBoolParams, Crashlytics.boolParams)
        checkParams(expectedLimitedParams.apply { put(KEY_IS_BOOTER, "false") }, YandexMetricaInternalConfig.params)
        checkParams(expectedExtendedParams, UserProfile.params)
        checkParams(expectedBoolParams, UserProfile.boolParams)

        val noParams = setOf(KEY_FLAVOR, KEY_BUILD_TIME)
        checkNoParams(noParams, Crashlytics.params)
        checkNoParams(noParams, YandexMetricaInternalConfig.params)
        checkNoParams(noParams, UserProfile.params)
    }

    @Test
    fun `when it is booter then isBooter is true`() {
        MultipleUsers.currentUserId = MultipleUsers.USER_OWNER
        expectedBoolParams[KEY_IS_BOOTER] = true

        initializeAndCheck(createConfig())

        checkParams(expectedExtendedParams, UserProfile.params)
        checkParams(expectedBoolParams, UserProfile.boolParams)
        checkParams(
            expectedExtendedParams.apply {
                put(KEY_FLAVOR, "internal")
                put(KEY_BUILD_TIME, "12.12.2019")
            },
            Crashlytics.params
        )
        checkParams(expectedBoolParams, Crashlytics.boolParams)
        checkParams(
            expectedLimitedParams.apply {
                put(KEY_IS_BOOTER, "true")
            },
            YandexMetricaInternalConfig.params
        )
    }

    @Test
    fun `when firmware version and date are empty then null passed to analytics`() {
        expectedLimitedParams[KEY_FIRMWARE_BUILD_NUMBER] = EMPTY_FIRMWARE
        expectedLimitedParams[KEY_FIRMWARE_BUILD_DATE_UTC] = EMPTY_FIRMWARE
        expectedExtendedParams[KEY_FIRMWARE_BUILD_NUMBER] = EMPTY_FIRMWARE
        expectedExtendedParams[KEY_FIRMWARE_BUILD_DATE_UTC] = EMPTY_FIRMWARE
        YapDevice.prepare(
            headId,
            vendor = "test_vendor",
            model = "test_model",
            type = "carsharing",
            mcu = "test_mcu",
            firmwareBuildNumber = null,
            firmwareBuildDate = null
        )
        initializeAndCheck(createConfig())

        checkParams(expectedExtendedParams, UserProfile.params)
        checkParams(expectedBoolParams, UserProfile.boolParams)
        checkParams(
            expectedExtendedParams.apply {
                put(KEY_FLAVOR, "internal")
                put(KEY_BUILD_TIME, "12.12.2019")
            },
            Crashlytics.params
        )
        checkParams(expectedBoolParams, Crashlytics.boolParams)
        checkParams(
            expectedLimitedParams.apply {
                put(KEY_IS_BOOTER, "false")
            },
            YandexMetricaInternalConfig.params
        )
    }

    @Test
    fun `when car properties are empty then unknown passed to analytics`() {
        expectedExtendedParams[KEY_VENDOR] = UNKNOWN_VALUE
        expectedExtendedParams[KEY_MODEL] = UNKNOWN_VALUE
        expectedExtendedParams[KEY_TYPE] = UNKNOWN_VALUE
        expectedExtendedParams[KEY_MCU] = UNKNOWN_VALUE
        YapDevice.prepare(
            headId,
            vendor = null,
            model = null,
            type = null,
            mcu = null,
            firmwareBuildNumber = firmwareBuildNumber,
            firmwareBuildDate = firmwareBuildDate
        )
        initializeAndCheck(createConfig())

        checkParams(expectedExtendedParams, UserProfile.params)
        checkParams(
            expectedExtendedParams.apply {
                put(KEY_FLAVOR, "internal")
                put(KEY_BUILD_TIME, "12.12.2019")
            },
            Crashlytics.params
        )
        checkParams(expectedLimitedParams, YandexMetricaInternalConfig.params)

        checkNoParams(setOf(KEY_IS_BOOTER), UserProfile.boolParams)
        checkNoParams(setOf(KEY_IS_BOOTER), Crashlytics.boolParams)
        checkNoParams(setOf(KEY_IS_BOOTER), YandexMetricaInternalConfig.params)
    }

    @Test
    fun `when initialized not in main process then crashlytics inits explicitly but not receive params`() {
        ActivityManagerMock.processName = "yandex.auto.another:process"
        initializeAndCheck(createConfig(), true)
        val noParams = expectedExtendedParams.keys.toMutableSet().apply {
            addAll(setOf(KEY_FLAVOR, KEY_BUILD_TIME))
        }
        checkNoParams(noParams, Crashlytics.params)
        checkNoParams(setOf(KEY_IS_BOOTER), Crashlytics.boolParams)

        checkParams(expectedExtendedParams, UserProfile.params)
        checkParams(expectedBoolParams, UserProfile.boolParams)
        checkParams(expectedLimitedParams.apply { put(KEY_IS_BOOTER, "false") }, YandexMetricaInternalConfig.params)
    }

    @Test
    fun `when initialized with disabled crashlytics then none of crashlytics initialized`() {
        initializeAndCheck(createConfig(crashlyticsEnabled = false), crashlyticsEnabled = false)
        val noParams = expectedExtendedParams.keys.toMutableSet().apply {
            addAll(setOf(KEY_FLAVOR, KEY_BUILD_TIME))
        }
        checkNoParams(noParams, Crashlytics.params)
        checkNoParams(setOf(KEY_IS_BOOTER), Crashlytics.boolParams)

        checkParams(expectedExtendedParams, UserProfile.params)
        checkParams(expectedBoolParams, UserProfile.boolParams)
        checkParams(expectedLimitedParams.apply { put(KEY_IS_BOOTER, "false") }, YandexMetricaInternalConfig.params)
    }

    @Test
    fun `when adding custom param for all configs then it passed to them`() {
        val custom1 = Pair("customstring", "customparam")
        val custom2 = Pair("custombool", true)
        initializeAndCheck(
            createConfig()
                .addStringParam(custom1.first, custom1.second)
                .addBoolParam(custom2.first, custom2.second)
        )
        checkParams(expectedExtendedParams.apply { put(custom1.first, custom1.second) }, UserProfile.params)
        checkParams(expectedBoolParams.apply { put(custom2.first, custom2.second) }, UserProfile.boolParams)
        checkParams(
            expectedExtendedParams.apply {
                put(KEY_FLAVOR, "internal")
                put(KEY_BUILD_TIME, "12.12.2019")
            },
            Crashlytics.params
        )
        checkParams(expectedBoolParams.apply { put(custom2.first, custom2.second) }, Crashlytics.boolParams)
        checkParams(
            expectedLimitedParams.apply {
                put(custom1.first, custom1.second)
                put(custom2.first, custom2.second.toString())
                put(KEY_IS_BOOTER, "false")
            },
            YandexMetricaInternalConfig.params
        )
    }

    @Test
    fun `when adding custom param and exclude from some configs then it should not be passed to them`() {
        val custom1 = Pair("custom", "customparam")
        val custom2 = Pair("secondcustom", "second")
        initializeAndCheck(
            createConfig()
                .addStringParam(custom1.first, custom1.second)
                .addStringParam(custom2.first, custom2.second)
        )
        checkParams(expectedExtendedParams, UserProfile.params)
        checkParams(expectedBoolParams, UserProfile.boolParams)
        checkParams(
            expectedExtendedParams.apply {
                put(KEY_FLAVOR, "internal")
                put(KEY_BUILD_TIME, "12.12.2019")
            },
            Crashlytics.params
        )
        checkParams(expectedBoolParams, Crashlytics.boolParams)
        checkParams(expectedLimitedParams.apply { put(KEY_IS_BOOTER, "false") }, YandexMetricaInternalConfig.params)
    }

    @Test
    fun `when not a carsharing then no isBooter provided`() {
        expectedExtendedParams[KEY_TYPE] = "not_a_carsharing"
        YapDevice.prepare(
            headId,
            vendor = "test_vendor",
            model = "test_model",
            type = "not_a_carsharing",
            mcu = "test_mcu",
            firmwareBuildNumber = firmwareBuildNumber,
            firmwareBuildDate = firmwareBuildDate
        )
        initializeAndCheck(createConfig())
        val noParams = setOf(KEY_IS_BOOTER)
        checkNoParams(noParams, UserProfile.boolParams)
        checkNoParams(noParams, Crashlytics.boolParams)
        checkNoParams(noParams, YandexMetricaInternalConfig.params)
        checkParams(expectedExtendedParams, Crashlytics.params)
        checkParams(expectedLimitedParams, YandexMetricaInternalConfig.params)
        checkParams(expectedExtendedParams, UserProfile.params)
    }

    @Test
    fun `when no interact accross user permission then no isBooter provided`() {
        userPermissionsTest(interact = false, manage = true)
    }

    @Test
    fun `when no manage users permission then no isBooter provided`() {
        userPermissionsTest(interact = true, manage = false)
    }

    @Test
    fun `when no any user permissions then no isBooter provided`() {
        userPermissionsTest(interact = false, manage = false)
    }

    private fun userPermissionsTest(interact: Boolean, manage: Boolean) {
        ContextCompat.currentPermissionState[ContextCompat.INTERACT_ACROSS_USERS] =
            if (interact) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        ContextCompat.currentPermissionState[ContextCompat.MANAGE_USERS] =
            if (manage) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        initializeAndCheck(createConfig())
        val noParams = setOf(KEY_IS_BOOTER)
        checkNoParams(noParams, UserProfile.boolParams)
        checkNoParams(noParams, Crashlytics.boolParams)
        checkNoParams(noParams, YandexMetricaInternalConfig.params)
        checkParams(expectedExtendedParams, Crashlytics.params)
        checkParams(expectedLimitedParams, YandexMetricaInternalConfig.params)
        checkParams(expectedExtendedParams, UserProfile.params)
    }

    private fun initializeAndCheck(
        analyticsConfig: AnalyticsConfig,
        fabricInitialized: Boolean = false,
        crashlyticsEnabled: Boolean = true
    ) {
        val initializer = Analytics.initializer()
            .applyConfig(analyticsConfig)
        checkNothingInitialized()
        assertNull(headId, Crashlytics.identifier)
        initializer.initialize()
        checkEverythingInitialized()
        checkFabricInitialized(crashlyticsEnabled && fabricInitialized)
        if (crashlyticsEnabled) {
            if (fabricInitialized) {
                assertNull(headId, Crashlytics.identifier)
            } else {
                assertEquals(headId, Crashlytics.identifier)
            }
        } else {
            assertNull(headId, Crashlytics.identifier)
        }
    }

    private fun <T> checkNoParams(noParams: Set<String>, params: Map<String, T?>) {
        noParams.forEach { assertNull(params[it]) }
    }

    private fun checkNothingInitialized() {
        checkFabricInitialized(false)
        assertNull(headId, Crashlytics.identifier)

        assertFalse(YandexMetrica.userProfileReported)
        assertFalse(YandexMetrica.activityTrackingEnabled)
        assertFalse(YandexMetricaInternal.isInitialized)
        assertFalse(YandexMetricaInternal.identifiersRequested)

        assertTrue(YandexMetricaInternalConfig.crashReportEnabled)
        assertTrue(YandexMetricaInternalConfig.logsEnabled)

        assertEquals(version, YandexMetricaInternalConfig.version)
    }

    private fun checkEverythingInitialized() {
        assertTrue(YandexMetrica.userProfileReported)
        assertTrue(YandexMetrica.activityTrackingEnabled)
        assertTrue(YandexMetricaInternal.isInitialized)
        assertTrue(YandexMetricaInternal.identifiersRequested)
        assertTrue(YandexMetricaInternalConfig.crashReportEnabled)
        assertTrue(YandexMetricaInternalConfig.logsEnabled)

        assertEquals(version, YandexMetricaInternalConfig.version)
    }

    private fun checkFabricInitialized(shouldBeInitialized: Boolean) {
        if (shouldBeInitialized) {
            assertTrue(Fabric.initializationChecked)
            assertTrue(Fabric.initialized)
            assertNull(Crashlytics.identifier)
        } else {
            assertFalse(Fabric.initializationChecked)
            assertFalse(Fabric.initialized)
        }
    }

    private fun <T> checkParams(expected: Map<String, T>, actual: Map<String, T?>) {
        expected.forEach { assertEquals(it.value, actual[it.key]) }
    }

    private fun createConfig(
        crashlyticsEnabled: Boolean = true,
        flavor: String? = AnalyticsConfigTest.flavor,
        buildTime: String? = AnalyticsConfigTest.buildTime
    ): AnalyticsConfig {
        return AnalyticsConfig(
            application, apiKey, version,
            isCrashlyticsEnabled = crashlyticsEnabled,
            flavor = flavor,
            buildTime = buildTime
        )
    }
}
