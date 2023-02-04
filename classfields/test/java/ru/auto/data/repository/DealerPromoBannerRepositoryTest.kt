package ru.auto.data.repository

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.ara.RxTest
import ru.auto.data.model.dealer.DealerPromoBannerShowState
import ru.auto.data.prefs.IPrefsDelegate
import ru.auto.data.prefs.MemoryPrefsDelegate
import ru.auto.data.repository.DealerPromoBannerRepository.Companion.PREF_DEALER_PROMO_BANNER_NEXT_SHOW_TIME
import ru.auto.data.repository.DealerPromoBannerRepository.Companion.PREF_DEALER_PROMO_BANNER_SHOW_COUNT
import java.util.*
import kotlin.test.assertEquals

@RunWith(AllureRunner::class) class DealerPromoBannerRepositoryTest : RxTest() {

    private val prefs: IPrefsDelegate = MemoryPrefsDelegate()
    private val repo: IDealerPromoBannerRepository = DealerPromoBannerRepository(prefs)

    @Test
    fun `test getShowState when prefs contain default values`() {
        prefs.saveInt(PREF_DEALER_PROMO_BANNER_SHOW_COUNT, 0)
        prefs.saveLong(PREF_DEALER_PROMO_BANNER_NEXT_SHOW_TIME, 0L)

        repo.getShowState()
            .test()
            .assertValue(DealerPromoBannerShowState(0, Date(0L)))
    }

    @Test
    fun `test getShowState when prefs contain show interval`() {
        val date = Date()
        prefs.saveInt(PREF_DEALER_PROMO_BANNER_SHOW_COUNT, 1)
        prefs.saveLong(PREF_DEALER_PROMO_BANNER_NEXT_SHOW_TIME, date.time)

        repo.getShowState()
            .test()
            .assertValue(DealerPromoBannerShowState(1, date))
    }

    @Test
    fun `test saveShowState`() {
        assertEquals(1, prefs.getInt(PREF_DEALER_PROMO_BANNER_SHOW_COUNT))
        assertEquals(1L, prefs.getLong(PREF_DEALER_PROMO_BANNER_NEXT_SHOW_TIME))

        val date = Date()
        val state = DealerPromoBannerShowState(2, date)
        repo.saveShowState(state)
            .test()
            .assertCompleted()

        assertEquals(2, prefs.getInt(PREF_DEALER_PROMO_BANNER_SHOW_COUNT))
        assertEquals(date.time, prefs.getLong(PREF_DEALER_PROMO_BANNER_NEXT_SHOW_TIME))
    }

}
