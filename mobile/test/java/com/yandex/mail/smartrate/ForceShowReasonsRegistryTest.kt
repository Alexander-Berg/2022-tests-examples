package com.yandex.mail.smartrate

import android.content.Context
import android.content.SharedPreferences
import com.yandex.mail.runners.UnitTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(UnitTestRunner::class)
class ForceShowReasonsRegistryTest {
    private lateinit var prefs: SharedPreferences

    @Before
    fun beforeEachTest() {
        prefs = UnitTestRunner.app().getSharedPreferences("pref", Context.MODE_PRIVATE)
    }

    @Test
    fun `Should register new force show reason as pending`() {
        val registry = ForceShowReasonsRegistry()
        registry.registerForceShowReason(prefs, TEST_REASON)

        assertThat(registry.hasPendingForceShowReasons()).isEqualTo(true)
    }

    @Test
    fun `Should clear pending force show reasons`() {
        val registry = ForceShowReasonsRegistry()
        registry.registerForceShowReason(prefs, TEST_REASON)
        registry.clearPendingForceShowReasons()

        assertThat(registry.hasPendingForceShowReasons()).isEqualTo(false)
    }

    @Test
    fun `Should not register fulfilled force show reason as pending`() {
        prefs.edit().putStringSet(ForceShowReasonsRegistry.FULFILLED_REASONS_KEY, hashSetOf(TEST_REASON)).apply()

        val registry = ForceShowReasonsRegistry()
        registry.registerForceShowReason(prefs, TEST_REASON)

        assertThat(registry.hasPendingForceShowReasons()).isEqualTo(false)
    }

    @Test
    fun `Should clear force show reasons on fulfillment`() {
        val registry = ForceShowReasonsRegistry()
        registry.registerForceShowReason(prefs, TEST_REASON)
        registry.fulfillPendingForceShowReasons(prefs)

        assertThat(registry.hasPendingForceShowReasons()).isEqualTo(false)
    }

    @Test
    fun `Should not persist force show reasons between initializations`() {
        var registry = ForceShowReasonsRegistry()
        registry.registerForceShowReason(prefs, TEST_REASON)
        registry = ForceShowReasonsRegistry()

        assertThat(registry.hasPendingForceShowReasons()).isEqualTo(false)
    }

    companion object {
        private const val TEST_REASON = "TEST_REASON"
    }
}
