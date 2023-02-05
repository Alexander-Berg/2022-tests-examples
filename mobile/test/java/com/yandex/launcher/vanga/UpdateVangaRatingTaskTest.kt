package com.yandex.launcher.vanga

import android.content.ComponentName
import androidx.collection.ArrayMap
import androidx.collection.SimpleArrayMap
import com.android.launcher3.AppInfo
import org.mockito.kotlin.*
import com.yandex.launcher.BaseRobolectricTest
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.assertion.assertThat
import com.yandex.vanga.ClientVangaItem
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers

private const val COMPLETE_LAUNCHER_ITEMS_COUNT = 3

class UpdateVangaRatingTaskTest : BaseRobolectricTest() {

    private lateinit var completeLauncherItems: MutableList<LauncherVangaItem>
    private lateinit var completeClientItems: MutableList<ClientVangaItem>

    private lateinit var componentToAppMap: SimpleArrayMap<String, AppInfo>

    private val testVisitsMap = ArrayMap<Int, Int>().apply { put(1, 1) }

    @Before
    override fun setUp() {
        super.setUp()

        componentToAppMap = compToApp()

        completeLauncherItems = mutableListOf()
        completeClientItems = mutableListOf()

        for (i in 0 until COMPLETE_LAUNCHER_ITEMS_COUNT) {
            val packageName = "package$i"
            val className = "class$i"

            completeLauncherItems.add(
                LauncherVangaItem(
                    packageName,
                    className,
                    i,
                    testVisitsMap,
                    testVisitsMap,
                    i,
                    testVisitsMap,
                    testVisitsMap
                )
            )

            completeClientItems.add(
                ClientVangaItem(
                    "{$packageName/$className}",
                    i,
                    testVisitsMap,
                    testVisitsMap,
                    i,
                    testVisitsMap,
                    testVisitsMap
                )
            )

            componentToAppMap.put(appInfo(packageName, className))
        }
    }

    private fun launcherItem(pkg: String, className: String? = null, personalCount: Int = 0): LauncherVangaItem {
        return LauncherVangaItem(
            pkg,
            className,
            personalCount,
            testVisitsMap,
            testVisitsMap,
            0,
            testVisitsMap,
            testVisitsMap
        )
    }

    private fun task(
        launcherItems: MutableList<LauncherVangaItem> = completeLauncherItems,
        partialUpdateParams: VangaPartialUpdateParams? = null
    ) = UpdateVangaRatingTask(launcherItems, partialUpdateParams, mock())

    @Test
    fun `getMissingKeys partialUpdateParams is missing, none extra client vanga items are added`() {
        val clientItems = completeClientItems
        val copyClientItems: MutableList<ClientVangaItem> = ArrayList(clientItems)

        task().getMissingKeys(clientItems)

        assertThat(clientItems, equalTo(copyClientItems))
    }

    @Test
    fun `getMissingKeys launcherItems presence isn't affect result`() {
        val clientItems1 = ArrayList(completeClientItems)
        val clientItems2 = ArrayList(completeClientItems)

        task().getMissingKeys(clientItems1)
        task(mutableListOf()).getMissingKeys(clientItems2)

        assertThat(clientItems1, equalTo(clientItems2))
    }

    @Test
    fun `getMissingKeys clientItems is empty, single missed client vanga item is added`() {
        val partialUpdateParams = partialParams(listOf("missedKey"))
        val task = task(mutableListOf(), partialUpdateParams)

        val clientItems = mutableListOf<ClientVangaItem>()
        val missingKeys = task.getMissingKeys(clientItems)

        assertThat(missingKeys!!.first(), equalTo("missedKey"))
    }

    @Test
    fun `getMissingKeys clientItems is filled, none missed keys are detected`() {
        val partialUpdateParams = partialParams(listOf("missedKey"))
        val task = task(mutableListOf(), partialUpdateParams)

        val clientItems = mutableListOf(ClientVangaItem(partialUpdateParams.requiredKeys.first()))
        val missingKeys = task.getMissingKeys(clientItems)

        assertThat(missingKeys.isNullOrEmpty(), equalTo(true))
    }

    @Test
    fun `getMissingKeys clientItems is filled with extra N items, all missed client vanga items are added`() {
        val requiredKeys = mutableListOf<String>()

        val clientItems = completeClientItems.apply {
            forEach {
                requiredKeys.add(it.key)
                requiredKeys.add(it.key + "extra")
            }
        }

        val task = task(mutableListOf(), partialParams(requiredKeys))

        val missingKeys = task.getMissingKeys(clientItems)

        assertThat(clientItems.map { it.key }.union(missingKeys!!).sorted(), equalTo(requiredKeys.sorted()))
    }

    @Test
    fun `getClientVangaItems returns same count of items as launcherItems`() {
        val result = task().getClientVangaItems(componentToAppMap)

        assertThat(result.size, equalTo(COMPLETE_LAUNCHER_ITEMS_COUNT))
    }

    @Test
    fun `getClientVangaItems picks all 3 missing class names from componentToAppMap`() {
        val compToApp = compToApp(
            appInfo("pack1", "class1"),
            appInfo("pack2", "class2"),
            appInfo("pack3", "class3")
        )
        val launcherItems = mutableListOf(
            launcherItem("pack1"),
            launcherItem("pack2"),
            launcherItem("pack3")
        )
        val expectedClientItemsKeys = compToApp.toMap().values.map { it.componentStr }.sorted()

        val clientItems = task(launcherItems).getClientVangaItems(compToApp)

        assertThat(clientItems.map { it.key }.sorted(), equalTo(expectedClientItemsKeys))
    }

    @Test
    fun `getClientVangaItems picks 2 from 3 missing class names from componentToAppMap`() {
        val compToApp = compToApp(
            appInfo("pack1", "class1"),
            appInfo("pack2", "class2")
        )
        val launcherItems = mutableListOf(
            launcherItem("pack1"),
            launcherItem("pack2"),
            launcherItem("pack3")
        )
        val expectedClientItemsKeys = compToApp.toMap().values.map { it.componentStr }.sorted()

        val clientItems = task(launcherItems).getClientVangaItems(compToApp)

        assertThat(clientItems.map { it.key }.sorted(), equalTo(expectedClientItemsKeys))
    }

    @Test
    fun `getClientVangaItems default first, launcher item with class name is more priority than default (without class name)`() {
        val compToApp = compToApp(appInfo("pack1", "class1"))

        val personalCountForDefaultItem = 1
        val personalCountForSpecificItem = 0

        val launcherItems = mutableListOf(
            launcherItem("pack1", null, personalCountForDefaultItem),
            launcherItem("pack1", "class1", personalCountForSpecificItem)
        )
        val clientItems = task(launcherItems).getClientVangaItems(compToApp)

        assertThat(clientItems.first().personalCount, equalTo(personalCountForSpecificItem))
    }

    @Test
    fun `getClientVangaItems default last, launcher item with class name is more priority than default (without class name)`() {
        val compToApp = compToApp(appInfo("pack1", "class1"))

        val personalCountForDefaultItem = 1
        val personalCountForSpecificItem = 0

        val launcherItems = mutableListOf(
            launcherItem("pack1", "class1", personalCountForSpecificItem),
            launcherItem("pack1", null, personalCountForDefaultItem)
        )
        val clientItems = task(launcherItems).getClientVangaItems(compToApp)

        assertThat(clientItems.first().personalCount, equalTo(personalCountForSpecificItem))
    }

    @Test
    fun `getClientVangaItems skip 2 unknown items from launchItems`() {
        val compToApp = compToApp(
            appInfo("pack2", "class2"),
            appInfo("pack3", "class3")
        )
        val launcherItems = mutableListOf(
            launcherItem("pack0"),
            launcherItem("pack1", "class2"),
            launcherItem("pack2", "class2"),
            launcherItem("pack3")
        )
        val expectedClientItemsKeys = compToApp.toMap().values.map { it.componentStr }.sorted()

        val clientItems = task(launcherItems).getClientVangaItems(compToApp)

        assertThat(clientItems.map { it.key }.sorted(), equalTo(expectedClientItemsKeys))
    }

    @Test
    fun `getClientVangaItems 2 duplicates, 1 unknown, 1 valid items, is omitting duplicates and unknown item`() {
        val compToApp = compToApp(
            appInfo("pack2", "class2"),
            appInfo("pack3", "class3")
        )

        val launcherItems = mutableListOf(
            launcherItem("pack1"),
            launcherItem("pack2"),
            launcherItem("pack2"),
            launcherItem("pack3")
        )
        val expectedClientItemsKeys = compToApp.toMap().values.map { it.componentStr }.sorted()

        val clientItems = task(launcherItems).getClientVangaItems(compToApp)

        assertThat(clientItems.map { it.key }.sorted(), equalTo(expectedClientItemsKeys))
    }

    @Test
    fun `getClientVangaItems returns list with correct items`() {
        val result = task(completeLauncherItems).getClientVangaItems(componentToAppMap)

        assertThat(result.sortedBy { it.key }.toMutableList(), equalTo(completeClientItems))
    }

    @Test
    fun `getPackageToComponentMap returns empty map`() {
        val result = task().getPackageToComponentMap(SimpleArrayMap())

        assertThat(result.size(), equalTo(0))
    }

    @Test
    fun `getPackageToComponentMap 1 appInfo, returns correct map`() {
        val result = task().getPackageToComponentMap(compToApp(appInfo("test1", "test2")))

        assertThat(result.size(), equalTo(1))
        assertThat(result.keyAt(0), equalTo("test1"))
        assertThat(result.valueAt(0), equalTo("{test1/test2}"))
    }
}

private fun appInfo(pkg: String, cls: String): AppInfo {
    return AppInfo().apply {
        ReflectionHelpers.setField(this, "componentName", ComponentName(pkg, cls))
        ReflectionHelpers.setField(this, "componentStr", componentName.toShortString())
    }
}

private fun partialParams(requiredKeys: List<String>) =
    VangaPartialUpdateParams(requiredKeys, emptyList())

private fun compToApp(vararg appInfos: AppInfo) =
    SimpleArrayMap<String, AppInfo>().put(*appInfos)

private fun SimpleArrayMap<String, AppInfo>.put(vararg appInfos: AppInfo): SimpleArrayMap<String, AppInfo> =
    appInfos.forEach { put(it.componentStr, it) }.let { this }

private fun <K, V> SimpleArrayMap<K, V>.toMap() =
    HashMap<K, V>().apply {
        for (i in 0 until this@toMap.size()) {
            put(keyAt(i), valueAt(i))
        }
    }
