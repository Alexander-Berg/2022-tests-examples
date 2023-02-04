package ru.auto.test.settings.presentation

import ru.auto.core_ui.tea.Feature
import ru.auto.data.util.LoadableData
import ru.auto.data.util.makeIf
import ru.auto.settings.provider.SettingEntity
import ru.auto.settings.provider.SettingValue
import ru.auto.test.common.model.SettingsStore

typealias SettingsFeature = Feature<Settings.Msg, Settings.State, Settings.Eff>
typealias SettingsReduceResult = Pair<Settings.State, Set<Settings.Eff>>

object Settings {

    data class State(
        val settings: LoadableData<SettingsStore> = LoadableData.Initial,
        val isHelpTooltipVisible: Boolean = false,
    )

    sealed class Msg {
        // data
        class OnSettingsChanged(val settings: LoadableData<SettingsStore>) : Msg()

        // ui
        class OnSettingValueChanged(val setting: SettingEntity, val value: SettingValue?) : Msg()
        object OnLogoClick : Msg()
        object OnHelpTooltipDismissRequest : Msg()
    }

    sealed class Eff {
        object FetchSettings : Eff()
        object ObserveSettings : Eff()

        data class UpdatedSettingValue(
            val setting: SettingEntity,
            val value: SettingValue?,
            val store: SettingsStore,
        ) : Eff()

        data class UpdatedFirstLaunch(val isFirstLaunch: Boolean, val store: SettingsStore) : Eff()
    }

    fun reduce(msg: Msg, state: State): SettingsReduceResult = when (msg) {
        is Msg.OnSettingsChanged -> {
            when (val settings = msg.settings) {
                is LoadableData.Success -> {
                    state.copy(
                        settings = settings,
                        isHelpTooltipVisible = state.isHelpTooltipVisible || settings.value.isFirstLaunch
                    ) to setOfNotNull(
                        makeIf(settings.value.isFirstLaunch) { Eff.UpdatedFirstLaunch(false, settings.value) }
                    )
                }
                else -> {
                    state.copy(settings = msg.settings) to emptySet()
                }
            }
        }
        is Msg.OnSettingValueChanged -> {
            when (state.settings) {
                is LoadableData.Success -> state to setOf(Eff.UpdatedSettingValue(msg.setting, msg.value, state.settings.value))
                else -> state to setOf()
            }
        }
        is Msg.OnLogoClick -> {
            when (state.settings) {
                is LoadableData.Success -> state.copy(isHelpTooltipVisible = true) to setOf()
                else -> state to setOf()
            }
        }
        is Msg.OnHelpTooltipDismissRequest -> {
            state.copy(isHelpTooltipVisible = false) to setOf()
        }
    }
}
