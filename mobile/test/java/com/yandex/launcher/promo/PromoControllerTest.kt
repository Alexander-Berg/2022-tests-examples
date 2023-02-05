package com.yandex.launcher.promo

import android.os.SystemClock
import androidx.collection.SimpleArrayMap
import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThanOrEqualTo
import com.natpryce.hamkrest.lessThanOrEqualTo
import org.mockito.kotlin.*
import com.yandex.launcher.common.util.InitHelper
import com.yandex.launcher.common.util.IntentUtils
import com.yandex.launcher.BaseRobolectricTest
import com.yandex.launcher.app.GlobalAppState
import com.yandex.launcher.preferences.Preference
import com.yandex.launcher.preferences.PreferencesManager
import com.yandex.launcher.promo.validitycheckers.PromoValidityChecker
import com.yandex.launcher.statistics.Statistics
import org.junit.After
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.util.ReflectionHelpers

class PromoControllerTest : BaseRobolectricTest() {

    private val emptyCheckers: Array<PromoValidityChecker> = arrayOf()
    private val checkerValidAlways = object : PromoValidityChecker() {
        override val serverName = "valid"

        override fun isValid(serverValue: String, promoBlock: PromoBlock): Boolean {
            return true
        }

        override fun onTerminate() {
        }
    }
    private val checkerInvalidAlways = object : PromoValidityChecker() {
        override val serverName = "invalid"

        override fun isValid(serverValue: String, promoBlock: PromoBlock): Boolean {
            return false
        }

        override fun onTerminate() {
        }
    }

    @After
    override fun tearDown() {
        super.tearDown()
        ReflectionHelpers.setStaticField(GlobalAppState::class.java, "instance", null)
        ReflectionHelpers.setStaticField(Statistics::class.java, "instance", null)
    }

    @Test
    fun `getPromo() should return null if there's no promo`() {
        // init
        val promoProvider: PromoProvider = mock()
        val promoHistoryProvider: PromoHistoryProvider = mock()

        // controller
        val controller = createController(promoProvider, promoHistoryProvider, emptyCheckers)

        // action
        val promo: PromoBlock? = controller.promo

        // check
        assertThat(promo, absent())
    }

    @Test
    fun `getPromo() should return promo from promoresponse`() {
        // init
        val promoProvider: PromoProvider = mock()
        val promoHistoryProvider: PromoHistoryProvider = mock()
        val blockId = "block_id"
        val block = createPromoBlock(blockId)
        val response = PromoResponse(1, arrayOf(block))
        val history = createPromoHistory(blockId)
        `when`(promoProvider.promoResponse).thenReturn(response)
        `when`(promoHistoryProvider.getPromoHistory(block)).thenReturn(history)

        // controller
        val controller = createController(promoProvider, promoHistoryProvider, emptyCheckers)

        // action
        val promo: PromoBlock? = controller.promo

        // check
        assertThat(response.blocks[0], equalTo(promo))
    }

    @Test
    fun `listShown() should update shown time and latest id in history`() {
        // init
        val promoProvider: PromoProvider = mock()
        val promoHistoryProvider: PromoHistoryProvider = mock()
        val blockId = "block_id"
        val block = createPromoBlock(blockId)
        val history = createPromoHistory(blockId)
        `when`(promoHistoryProvider.getPromoHistory(block)).thenReturn(history)

        // controller
        val controller = createController(promoProvider, promoHistoryProvider, emptyCheckers)

        // action
        val realtimeRangeStart = SystemClock.elapsedRealtime()
        controller.listShown(block)
        val realtimeRangeEnd = SystemClock.elapsedRealtime()

        // check
        assertThat(history.lastShownRealtime, greaterThanOrEqualTo(realtimeRangeStart))
        assertThat(history.lastShownRealtime, lessThanOrEqualTo(realtimeRangeEnd))
        verify(promoHistoryProvider, times(1)).updatePromoHistory(history)
        assertPrefs(false, 0, blockId)
    }

    @Test
    fun `hide promo should update prefs and history, but last shown time is unchanged`() {
        // init
        val promoProvider: PromoProvider = mock()
        val promoHistoryProvider: PromoHistoryProvider = mock()
        val blockId = "block_id"
        val block = createPromoBlock(blockId)
        val history = createPromoHistory(blockId, hidden = false, lastShownRealtime = -1)
        `when`(promoHistoryProvider.getPromoHistory(block)).thenReturn(history)

        // controller
        val controller = createController(promoProvider, promoHistoryProvider, emptyCheckers)

        // action
        controller.promoHideOption(block)

        // check
        assertThat(history.hidden, equalTo(true))
        assertThat(history.lastShownRealtime, equalTo(-1L))
        verify(promoHistoryProvider, times(1)).updatePromoHistory(history)
        assertPrefs(true, 0, null)
    }

    @Test
    fun `choose action on promo should update prefs and history, but last shown time is unchanged`() {
        // init
        val promoProvider: PromoProvider = mock()
        val promoHistoryProvider: PromoHistoryProvider = mock()
        val blockId = "block_id"
        val block = createPromoBlock(blockId)
        val history = createPromoHistory(blockId, hidden = false, lastShownRealtime = -1, hiddenForever = false)
        `when`(promoHistoryProvider.getPromoHistory(block)).thenReturn(history)
        ReflectionHelpers.setStaticField(IntentUtils::class.java, "sUnsafeActivityStartAllowed", true)

        // controller
        val controller = createController(promoProvider, promoHistoryProvider, emptyCheckers)

        // action
        controller.promoActionOption(block)

        // check
        assertThat(history.hidden, equalTo(true))
        assertThat(history.hiddenForever, equalTo(true))
        assertThat(history.lastShownRealtime, equalTo(-1L))
        verify(promoHistoryProvider, times(1)).updatePromoHistory(history)
        assertPrefs(true, 0, null)
    }

    @Test
    fun `getPromo() should return null if it is hidden and sessions delay is active`() {
        // init
        val promoProvider: PromoProvider = mock()
        val promoHistoryProvider: PromoHistoryProvider = mock()
        val blockId = "block_id"
        val block = createPromoBlock(blockId)
        val promoResponse = PromoResponse(1, arrayOf(block))
        val blockHistory = createPromoHistory(blockId)
        `when`(promoProvider.promoResponse).thenReturn(promoResponse)
        `when`(promoHistoryProvider.getPromoHistory(block)).thenReturn(blockHistory)
        initPrefs(true, 0, null)

        // controller
        val controller = createController(promoProvider, promoHistoryProvider, emptyCheckers)

        // action 1
        val initialPromo = controller.promo

        // check 1
        assertThat(initialPromo, absent())
        verify(promoProvider, times(1)).promoResponse
        verifyNoInteractions(promoHistoryProvider)
        assertPrefs(true, 0, null)

        // action 2
        controller.listShown(null)

        // check 2
        assertPrefs(true, 1, null)
        verifyNoInteractions(promoHistoryProvider)

        // action 3
        val visiblePromo = controller.promo

        // check 3
        assertThat(block, equalTo(visiblePromo))
        verify(promoProvider, times(2)).promoResponse
        assertPrefs(true, 1, null)

        // action 4
        controller.listShown(block)

        // check 4
        assertPrefs(false, 0, block.id)
    }

    @Test
    fun `getPromo() should return null if promo is hidden forever`() {
        // init
        val promoProvider: PromoProvider = mock()
        val promoHistoryProvider: PromoHistoryProvider = mock()
        val blockId = "block_id"
        val block = createPromoBlock(blockId)
        val promoResponse = PromoResponse(1, arrayOf(block))
        val blockHistory = createPromoHistory(blockId, hidden = true, lastShownRealtime = SystemClock.elapsedRealtime(), hiddenForever = true)
        `when`(promoProvider.promoResponse).thenReturn(promoResponse)
        `when`(promoHistoryProvider.getPromoHistory(block)).thenReturn(blockHistory)
        initPrefs(true, 0, null)

        // controller
        val controller = createController(promoProvider, promoHistoryProvider, emptyCheckers)

        // action
        val promo = controller.promo

        // check
        assertThat(promo, absent())
    }

    @Test
    fun `getPromo() should return promo if there was previously shown block with the same priority`() {
        // init
        val promoProvider: PromoProvider = mock()
        val promoHistoryProvider: PromoHistoryProvider = mock()
        val blockId1 = "block_id_1"
        val blockId2 = "block_id_2"
        val block1 = createPromoBlock(blockId1, priority = 100)
        val block2 = createPromoBlock(blockId2, priority = 100)
        val response = PromoResponse(1, arrayOf(block1, block2))
        val history1 = createPromoHistory(blockId1)
        val history2 = createPromoHistory(blockId2, lastShownRealtime = 1000)
        `when`(promoProvider.promoResponse).thenReturn(response)
        `when`(promoHistoryProvider.getPromoHistory(block1)).thenReturn(history1)
        `when`(promoHistoryProvider.getPromoHistory(block2)).thenReturn(history2)
        initPrefs(false, 0, blockId2)

        // controller
        val controller = createController(promoProvider, promoHistoryProvider, emptyCheckers)

        // action
        val promo: PromoBlock? = controller.promo

        // check
        assertThat(block2, equalTo(promo))
    }

    @Test
    fun `getPromo() should return block with higher priority when there was previously shown block with a lower priority`() {
        // init
        val promoProvider: PromoProvider = mock()
        val promoHistoryProvider: PromoHistoryProvider = mock()
        val blockId1 = "block_id_1"
        val blockId2 = "block_id_2"
        val block1 = createPromoBlock(blockId1, priority = 200)
        val block2 = createPromoBlock(blockId2, priority = 100)
        val response = PromoResponse(1, arrayOf(block1, block2))
        val history1 = createPromoHistory(blockId1)
        val history2 = createPromoHistory(blockId2, lastShownRealtime = 1000)
        `when`(promoProvider.promoResponse).thenReturn(response)
        `when`(promoHistoryProvider.getPromoHistory(block1)).thenReturn(history1)
        `when`(promoHistoryProvider.getPromoHistory(block2)).thenReturn(history2)
        initPrefs(false, 0, blockId2)

        // controller
        val controller = createController(promoProvider, promoHistoryProvider, emptyCheckers)

        // action
        val promo: PromoBlock? = controller.promo

        // check
        assertThat(block1, equalTo(promo))
    }

    @Test
    fun `getPromo() should return promo if all checks passed`() {
        // init
        val promoProvider: PromoProvider = mock()
        val promoHistoryProvider: PromoHistoryProvider = mock()
        val block = createPromoBlock(
            showConditions = SimpleArrayMap<String, String>().apply {
                put(checkerValidAlways.serverName, true.toString())
                put(checkerValidAlways.serverName, true.toString())
            }
        )
        val response = PromoResponse(1, arrayOf(block))
        val history = createPromoHistory()
        `when`(promoProvider.promoResponse).thenReturn(response)
        `when`(promoHistoryProvider.getPromoHistory(block)).thenReturn(history)

        // controller
        val controller = createController(promoProvider, promoHistoryProvider, arrayOf(checkerValidAlways))

        // action
        val promo: PromoBlock? = controller.promo

        // check
        assertThat(block, equalTo(promo))
    }

    @Test
    fun `getPromo() should return null if any check failed`() {
        // init
        val promoProvider: PromoProvider = mock()
        val promoHistoryProvider: PromoHistoryProvider = mock()
        val block = createPromoBlock(
            showConditions = SimpleArrayMap<String, String>().apply {
                put(checkerValidAlways.serverName, true.toString())
                put(checkerInvalidAlways.serverName, true.toString())
            }
        )
        val response = PromoResponse(1, arrayOf(block))
        val history = createPromoHistory()
        `when`(promoProvider.promoResponse).thenReturn(response)
        `when`(promoHistoryProvider.getPromoHistory(block)).thenReturn(history)

        // controller
        val controller = createController(promoProvider, promoHistoryProvider, arrayOf(checkerValidAlways, checkerInvalidAlways))

        // action
        val promo: PromoBlock? = controller.promo

        // check
        assertThat(promo, absent())
    }

    @Test
    fun `getPromo() should return null when any check is missing`() {
        // init
        val promoProvider: PromoProvider = mock()
        val promoHistoryProvider: PromoHistoryProvider = mock()
        val block = createPromoBlock(
            showConditions = SimpleArrayMap<String, String>().apply {
                put(checkerValidAlways.serverName, true.toString())
            }
        )
        val response = PromoResponse(1, arrayOf(block))
        val history = createPromoHistory()
        `when`(promoProvider.promoResponse).thenReturn(response)
        `when`(promoHistoryProvider.getPromoHistory(block)).thenReturn(history)

        // controller
        val controller = createController(promoProvider, promoHistoryProvider, emptyCheckers)

        // action
        val promo: PromoBlock? = controller.promo

        // check
        assertThat(promo, absent())
    }

    private fun createPromoHistory(
        id: String = "block_id",
        hidden: Boolean = false,
        lastShownRealtime: Long = -1,
        hiddenForever: Boolean = false
    ): PromoHistory {
        return PromoHistory(id).apply {
            this.hidden = hidden
            this.lastShownRealtime = lastShownRealtime
            this.hiddenForever = hiddenForever
        }
    }

    private fun createPromoBlock(
        id: String = "block_id",
        priority: Int = 500,
        imageUrl: String = "",
        title: String = "block title",
        description: String = "block description",
        showBadge: Boolean = true,
        showConditions: SimpleArrayMap<String, String> = SimpleArrayMap(),
        skipButton: PromoBlock.PromoButton? = null,
        primaryButton: PromoBlock.PromoButton = createPromoButton()
    ): PromoBlock {
        return PromoBlock(
            id,
            priority,
            imageUrl,
            title,
            description,
            showBadge,
            showConditions,
            if (skipButton == null) { arrayOf(primaryButton) } else { arrayOf(primaryButton) }
        )
    }

    private fun createPromoButton(
        id: String = "button id",
        @PromoBlock.PromoButton.ButtonStyle style: String = PromoBlock.PromoButton.BUTTON_STYLE_PRIMARY,
        caption: String = "button caption",
        action: String = "button action"
    ): PromoBlock.PromoButton {
        return PromoBlock.PromoButton(
            id,
            style,
            caption,
            action
        )
    }

    private fun initPrefs(hidden: Boolean, notShownTimes: Int, latestShownId: String?) {
        PreferencesManager.put(Preference.PROMO_HIDDEN, hidden)
        PreferencesManager.put(Preference.PROMO_NOT_SHOWN_TIMES, notShownTimes)
        PreferencesManager.put(Preference.PROMO_LATEST_SHOWN_ID, latestShownId)
    }

    private fun assertPrefs(hidden: Boolean, notShownTimes: Int, latestShownId: String?) {
        assertThat(hidden, equalTo(PreferencesManager.getBoolean(Preference.PROMO_HIDDEN) ?: false))
        assertThat(notShownTimes, equalTo(PreferencesManager.getInt(Preference.PROMO_NOT_SHOWN_TIMES) ?: 0))
        assertThat(latestShownId, equalTo(PreferencesManager.getString(Preference.PROMO_LATEST_SHOWN_ID)))
    }

    private fun createController(
        promoProvider: PromoProvider,
        promoHistoryProvider: PromoHistoryProvider,
        checkers: Array<PromoValidityChecker>
    ): PromoController {
        val globalAppState: GlobalAppState = mock()
        val statistics: Statistics = mock()
        val initHelper = InitHelper({}, null, null)
        val validityCheckers = SimpleArrayMap<String, PromoValidityChecker>()
        initHelper.forceInitialize()
        checkers.forEach { validityCheckers.put(it.serverName, it) }

        ReflectionHelpers.setStaticField(GlobalAppState::class.java, "instance", globalAppState)
        ReflectionHelpers.setStaticField(Statistics::class.java, "instance", statistics)

        val controller = spy(PromoController(appContext))

        ReflectionHelpers.setField(controller, "initialized", true)
        ReflectionHelpers.setField(controller, "initHelper", initHelper)
        ReflectionHelpers.setField(controller, "validityCheckers", validityCheckers)
        ReflectionHelpers.setField(controller, "promoProvider", promoProvider)
        ReflectionHelpers.setField(controller, "promoHistoryProvider", promoHistoryProvider)

        return controller
    }
}
