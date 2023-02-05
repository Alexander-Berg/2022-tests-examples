package ru.yandex.disk.experiments

import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.yandex.disk.test.Assert2.assertThat
import ru.yandex.disk.test.TestObjectsFactory
import ru.yandex.disk.util.ScopedKeyValueStore

private const val WHITE_EXP_ENABLED = "disk_android_white_enabled"
private const val WHITE_EXP_ENABLED_2 = "disk_android_white_enabled_2"
private const val WHITE_EXP_ENABLED_WITH_CONTROL = "disk_android_white_enabled_3"
private const val WHITE_EXP_DISABLED = "disk_android_white_disabled"
private const val WHITE_EXP_DISABLED_WITH_CONTROL = "disk_android_white_disabled_2"
private const val BLACK_EXP_ENABLED = "disk_android_black_enabled"
private const val BLACK_EXP_ENABLED_WITH_CONTROL = "disk_android_black_enabled_2"
private const val CONTROL_EXP = "disk_android_white_enabled_3_control"
private const val CONTROL_EXP_2 = "disk_android_white_disabled_2_control"
private const val CONTROL_EXP_3 = "disk_android_black_enabled_2_control"
private const val UNEXPECTED_FLAG = "unexpected_flag"

@Config(sdk = [26])
@RunWith(RobolectricTestRunner::class)
class ExperimentsSettingsTest {

    private val whiteList = listOf(
        WHITE_EXP_ENABLED,
        WHITE_EXP_ENABLED_2,
        WHITE_EXP_ENABLED_WITH_CONTROL,
        WHITE_EXP_DISABLED,
        WHITE_EXP_DISABLED_WITH_CONTROL
    )
    private val enabledList = listOf(
        WHITE_EXP_ENABLED,
        WHITE_EXP_ENABLED_2,
        WHITE_EXP_ENABLED_WITH_CONTROL,
        BLACK_EXP_ENABLED,
        BLACK_EXP_ENABLED_WITH_CONTROL,
        CONTROL_EXP,
        CONTROL_EXP_2,
        CONTROL_EXP_3
    )

    private val settings = TestObjectsFactory.createSettings(RuntimeEnvironment.application!!)
    private val store = ScopedKeyValueStore(settings, "test")
    private val whitelistProvider = ExperimentsWhitelistProvider { whiteList }
    private val experiments = ExperimentsSettings(store, whitelistProvider)

    @Test
    fun shouldDisabledUnexpectedFlags() {
        updateExperiments(enabledList)

        val result = experiments.getFlag(UNEXPECTED_FLAG)

        assertThat(result, equalTo(false))
    }

    @Test
    fun shouldReturnDefaultValueForUnexpectedFlags() {
        updateExperiments(enabledList)

        val enabledByDefault = experiments.getFlag(UNEXPECTED_FLAG, true)
        val disabledByDefault = experiments.getFlag(UNEXPECTED_FLAG, false)

        assertThat(enabledByDefault, equalTo(true))
        assertThat(disabledByDefault, equalTo(false))
    }

    @Test
    fun shouldReturnDefaultValueBlackListFlags() {
        updateExperiments(enabledList)

        val enabledFlag = experiments.getFlag(BLACK_EXP_ENABLED, true)
        val disabledFlag = experiments.getFlag(BLACK_EXP_ENABLED, false)

        assertThat(enabledFlag, equalTo(true))
        assertThat(disabledFlag, equalTo(false))
    }

    @Test
    fun shouldReturnDefaultValueForNotEnabledFlags() {
        updateExperiments(enabledList)

        val enabledByDefault = experiments.getFlag(WHITE_EXP_DISABLED, true)
        val disabledByDefault = experiments.getFlag(WHITE_EXP_DISABLED, false)

        assertThat(enabledByDefault, equalTo(true))
        assertThat(disabledByDefault, equalTo(false))
    }

    @Test
    fun shouldReturnDefaultValueForNotEnabledInControlGroupsFlags() {
        updateExperiments(enabledList)

        val enabledByDefault = experiments.getFlag(WHITE_EXP_DISABLED_WITH_CONTROL, true)
        val disabledByDefault = experiments.getFlag(WHITE_EXP_DISABLED_WITH_CONTROL, false)

        assertThat(enabledByDefault, equalTo(true))
        assertThat(disabledByDefault, equalTo(false))
    }

    @Test
    fun shouldReturnDefaultValueForBlackListedFlagsInControlGroups() {
        updateExperiments(enabledList)

        val enabledByDefault = experiments.getFlag(BLACK_EXP_ENABLED_WITH_CONTROL, true)
        val disabledByDefault = experiments.getFlag(BLACK_EXP_ENABLED_WITH_CONTROL, false)

        assertThat(enabledByDefault, equalTo(true))
        assertThat(disabledByDefault, equalTo(false))
    }

    @Test
    fun shouldDisableEnabledFlagsInControlGroups() {
        updateExperiments(enabledList)

        val flag = experiments.getFlag(WHITE_EXP_ENABLED_WITH_CONTROL, true)

        assertThat(flag, equalTo(false))
    }

    @Test
    fun shouldEnableAllEnabledWhiteListFlagsWithoutControl() {
        updateExperiments(enabledList)

        val exp1 = experiments.getFlag(WHITE_EXP_ENABLED, false)
        val exp2 = experiments.getFlag(WHITE_EXP_ENABLED_2, false)

        assertThat(exp1, equalTo(true))
        assertThat(exp2, equalTo(true))
    }

    @Test
    fun shouldReturnEnabledFlagsIncludingBlackList() {
        updateExperiments(enabledList)

        val flags = experiments.getFlags()

        assertThat(flags, equalTo(enabledList))
    }

    @Test
    fun shouldUpdateFlagsList() {
        updateExperiments(enabledList)

        val newEnabledList = listOf(WHITE_EXP_ENABLED, WHITE_EXP_ENABLED_2)
        updateExperiments(newEnabledList)
        val flags = experiments.getFlags()

        assertThat(flags, equalTo(newEnabledList))
    }

    @Test
    fun shouldAddNewItemsToList() {
        updateExperiments(listOf(WHITE_EXP_ENABLED, WHITE_EXP_ENABLED_2))

        updateExperiments(listOf(WHITE_EXP_ENABLED, WHITE_EXP_ENABLED_2, WHITE_EXP_DISABLED))
        val exp = experiments.getFlag(WHITE_EXP_DISABLED, false)

        assertThat(exp, equalTo(true))
    }

    @Test
    fun shouldRemoveOldItemsFromList() {
        updateExperiments(listOf(WHITE_EXP_ENABLED, WHITE_EXP_ENABLED_2))

        updateExperiments(listOf(WHITE_EXP_ENABLED_2, WHITE_EXP_DISABLED))
        val exp = experiments.getFlag(WHITE_EXP_ENABLED, false)

        assertThat(exp, equalTo(false))
    }

    @Test
    fun shouldNotRemoveOldItemIfStillExist() {
        updateExperiments(listOf(WHITE_EXP_ENABLED, WHITE_EXP_ENABLED_2))

        updateExperiments(listOf(WHITE_EXP_ENABLED_2, WHITE_EXP_DISABLED))
        val exp = experiments.getFlag(WHITE_EXP_ENABLED_2, false)

        assertThat(exp, equalTo(true))
    }

    private fun updateExperiments(list: List<String>) {
        experiments.apply(list, emptyList(), null)
    }
}
