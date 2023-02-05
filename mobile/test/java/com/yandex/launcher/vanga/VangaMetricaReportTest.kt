package com.yandex.launcher.vanga

import android.content.ComponentName
import android.content.Context
import android.graphics.Point
import android.view.View
import androidx.collection.ArraySet
import androidx.collection.SimpleArrayMap
import androidx.test.core.app.ApplicationProvider
import com.android.launcher3.AppInfo
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.mockito.kotlin.*
import com.yandex.launcher.BaseRobolectricTest
import com.yandex.launcher.ProgramList
import com.yandex.launcher.allapps.AllAppsRoot
import com.yandex.launcher.allapps.NewAppsGrid
import com.yandex.launcher.app.GlobalAppState
import com.yandex.launcher.device.DeviceProfile
import com.yandex.launcher.device.GridMetrics
import com.yandex.launcher.device.profile.DeviceProfileManager
import com.yandex.launcher.loaders.CategoryLoader
import com.yandex.launcher.loaders.experiments.ExperimentManager
import com.yandex.launcher.statistics.*
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow.extract
import org.robolectric.util.ReflectionHelpers

val testComponentName = ComponentName("test", "test")
val point = Point(100, 500)

const val ALL_APPS_COLUMN_NUMBER = 4

@Config(shadows = [StoryManagerShadow::class])
class VangaMetricaReportTest : BaseRobolectricTest() {

    private val statistics = ReflectionHelpers.callConstructor(StoryManager::class.java)
    private val experimentManager = mock<ExperimentManager>()

    private val testEntry = mock<LauncherVangaRatingEntry> {
        on { key } doReturn testComponentName.toShortString()
    }
    private val vangaHistory = mock<VangaHistory> {
        on { get(testComponentName.toShortString()) } doReturn testEntry
    }

    private val testAppInfo = AppInfo()
    private val componentToAppMap = SimpleArrayMap<String, AppInfo>()

    private val mockView = mock<View> {
        on { tag } doReturn testAppInfo
    }
    private val mockGridMetrica = mock<GridMetrics>()
    private val mockDeviceProfile = mock<DeviceProfile> {
        on { getGridMetrics(any()) } doReturn mockGridMetrica
    }
    private val mockAllAppsRoot = mock<AllAppsRoot>()
    private lateinit var mockGlobalAppState: GlobalAppState

    private lateinit var mockProgramList: ProgramList
    private var mockCategoryLoader: CategoryLoader = mock()
    private lateinit var shadowStatistics: StoryManagerShadow
    private lateinit var newAppsGrid: NewAppsGridForTest

    @Before
    override fun setUp() {
        super.setUp()
        ReflectionHelpers.setStaticField(Statistics::class.java, "instance", statistics)
        shadowStatistics = extract(statistics)
        mockProgramList = spy(ProgramList(ApplicationProvider.getApplicationContext(), experimentManager))
        doReturn(ALL_APPS_COLUMN_NUMBER).`when`(mockProgramList).allAppsColumnNumber
        ReflectionHelpers.setField(testAppInfo, "componentName", testComponentName)
        componentToAppMap.put(testComponentName.toShortString(), testAppInfo)
        ReflectionHelpers.setField(mockProgramList, "componentToApp", componentToAppMap)

        ReflectionHelpers.setStaticField(DeviceProfileManager::class.java, "profile", mockDeviceProfile)
        mockGlobalAppState = mock()
        ReflectionHelpers.setStaticField(GlobalAppState::class.java, "instance", mockGlobalAppState)
        ReflectionHelpers.setField(mockGlobalAppState, "programList", mockProgramList)
        ReflectionHelpers.setField(mockGlobalAppState, "categoryLoader", mockCategoryLoader)
        newAppsGrid = NewAppsGridForTest(ApplicationProvider.getApplicationContext())
        ReflectionHelpers.setField(newAppsGrid, "programList", mockProgramList)
        ReflectionHelpers.setField(newAppsGrid, "allAppsRoot", mockAllAppsRoot)
        ReflectionHelpers.setField(mockCategoryLoader, "hiddenAppsComponentNames", ArraySet<ComponentName>())
    }

    @After
    override fun tearDown() {
        super.tearDown()
        ReflectionHelpers.setStaticField(Statistics::class.java, "instance", null)
        ReflectionHelpers.setStaticField(GlobalAppState::class.java, "instance", null)
        ReflectionHelpers.setStaticField(DeviceProfileManager::class.java, "profile", null)
    }

    @Test
    fun `app launched, not vanga experiment, AppStart reported in metrica`() {
        ReflectionHelpers.setField(mockProgramList, "headerAppsHistory", vangaHistory)

        try {
            newAppsGrid.onClick(mockView)
        } catch (e: VerifyError) {
            // reckit fails by some reason. It's ok to ignore it, all the necessary info is already obtained
        }

        assertThat(
            shadowStatistics.events.count { it.type == StoryEvent.Events.EVENT_APP_START },
            equalTo(1)
        )

        shadowStatistics.events[0].run {
            assertThat(type, equalTo(StoryEvent.Events.EVENT_APP_START))
            assertThat(param0, equalTo(Statistics.POSITION_ALL_APPS_RECENT))
            val itemAnalyticsInfo = param1 as ItemAnalyticsInfo
            assertThat(itemAnalyticsInfo.componentName, equalTo(testComponentName))
            assertThat(itemAnalyticsInfo.positionX, equalTo(point.x))
            assertThat(itemAnalyticsInfo.positionY, equalTo(point.y))
        }
    }

    @Test
    fun `app launched, VangaLaunch reported in metrica`() {
        ReflectionHelpers.setField(mockProgramList, "headerAppsHistory", vangaHistory)

        try {
            newAppsGrid.onClick(mockView)
        } catch (e: VerifyError) {
            // reckit fails by some reason. It's ok to ignore it, all the necessary info is already obtained
        }

        assertThat(
            shadowStatistics.events.count { it.type == StoryEvent.Events.EVENT_VANGA_LAUNCH },
            equalTo(1)
        )

        shadowStatistics.events
            .first { it.type == StoryEvent.Events.EVENT_VANGA_LAUNCH }
            .run {
                assertThat(type, equalTo(StoryEvent.Events.EVENT_VANGA_LAUNCH))
                assertThat(param0, equalTo(Statistics.POSITION_ALL_APPS_RECENT))
                val vangaInfo = param1 as VangaStory.VangaInfo
                assertThat(vangaInfo.column, equalTo(point.x + 1))
                assertThat(vangaInfo.row, equalTo(point.y + 1))
            }
    }

    @Test
    fun `apps shown, vanga experiment, VangaView reported metrica`() {
        prepareDependenciesForVangaViewReport()

        newAppsGrid.reportAppsShownPublic()

        assertThat(shadowStatistics.events.size, equalTo(1))

        shadowStatistics.events[0].run {
            assertThat(type, equalTo(StoryEvent.Events.EVENT_VANGA_VIEW))
            assertThat(param0, equalTo(Statistics.POSITION_ALL_APPS_RECENT))
        }

    }

    @Test
    fun `apps shown, vanga experiment, correct list passed to metrica`() {
        prepareDependenciesForVangaViewReport()

        newAppsGrid.reportAppsShownPublic()

        assertThat(shadowStatistics.events.size, equalTo(1))

        @Suppress("UNCHECKED_CAST")
        val resultList = shadowStatistics.events[0].param1 as List<VangaStory.VangaInfo>
        assertThat(resultList.size, equalTo(ALL_APPS_COLUMN_NUMBER))
        assertThat(isVangaInfoListIsCorrect(resultList), equalTo(true))
    }

    private fun getAppInfoList(): List<AppInfo> {
        val result = ArrayList<AppInfo>()

        for (i in 0..9) {
            val info = AppInfo()
            ReflectionHelpers.setField(info, "componentName", ComponentName("test $i", ""))
            result.add(info)
        }

        return result
    }

    private fun isVangaInfoListIsCorrect(list: List<VangaStory.VangaInfo>): Boolean {
        for (i in 0 until ALL_APPS_COLUMN_NUMBER) {
            val item = list[i]
            if (item.packageName != "test $i") {
                return false
            }
            if (item.row != 1) {
                return false
            }
            if (item.column != (i + 1)) {
                return false
            }
        }

        return true
    }

    private fun prepareDependenciesForVangaViewReport() {
        ReflectionHelpers.setField(mockProgramList, "headerAppsHistory", vangaHistory)
        val appInfoList = getAppInfoList()
        assumeThat(appInfoList.size, `is`(10))
        ReflectionHelpers.setField(newAppsGrid, "columnCount", ALL_APPS_COLUMN_NUMBER)
        whenever(mockProgramList.allAppsColumnNumber).doReturn(ALL_APPS_COLUMN_NUMBER)
        doReturn(appInfoList).`when`(mockAllAppsRoot).apps
        doReturn(appInfoList).`when`(mockProgramList).getHeaderApps(ALL_APPS_COLUMN_NUMBER)
    }

    private class NewAppsGridForTest(context: Context): NewAppsGrid(context, null) {

        override fun getGridChildPos(child: View): Point {
            return point
        }

        override fun getGridChildPos(childIndex: Int): Point {
            return point
        }

        fun reportAppsShownPublic() {
            reportAppsShown()
        }
    }
}
