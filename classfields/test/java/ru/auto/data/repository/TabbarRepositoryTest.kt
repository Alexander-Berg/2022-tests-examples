package ru.auto.data.repository

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.ara.RxTest
import ru.auto.data.model.tabbar.MainTabbarTab
import ru.auto.data.prefs.IReactivePrefsDelegate
import ru.auto.data.prefs.MemoryReactivePrefsDelegate
import kotlin.test.assertEquals

@RunWith(AllureRunner::class) class TabbarRepositoryTest : RxTest() {

    private val prefs: IReactivePrefsDelegate = MemoryReactivePrefsDelegate()
    private val tabbarRepository = TabbarRepository(prefs)

    @Test
    fun `defaultMiddleTabType is always ADD`() {
        assertEquals(MainTabbarTab.TabType.ADD, tabbarRepository.defaultOffersTabType)
    }

    @Test
    fun `first call of getMiddleTabType() returns ADD`() {
        tabbarRepository.getOffersTabType()
            .test()
            .assertValue(MainTabbarTab.TabType.ADD)
    }

    @Test
    fun `next call of getMiddleTabType() returns saved value`() {
        prefs.saveString("PREF_MIDDLE_TAB_TYPE", MainTabbarTab.TabType.OFFERS.name).subscribe()
        tabbarRepository.getOffersTabType()
            .test()
            .assertValue(MainTabbarTab.TabType.OFFERS)
    }

    @Test
    fun `saveMiddleTabType() saves data in preferences`() {
        tabbarRepository.saveOffersTabType(MainTabbarTab.TabType.DEALER).subscribe()
        prefs.getString("PREF_MIDDLE_TAB_TYPE")
            .test()
            .assertValue(MainTabbarTab.TabType.DEALER.name)
    }

}
