package ru.auto.test.settings.presentation

import ru.auto.ara.util.backgroundToUi
import ru.auto.test.common.data.SettingsRepository
import ru.auto.test.common.presentation.wrapWithCompletableEffectHandler
import ru.auto.test.common.presentation.wrapWithObservableEffectHandler
import ru.auto.test.experiments.presentation.Experiments

fun SettingsFeature.wrapWithSettingsEffectHandler(
    settingsRepository: SettingsRepository,
) = wrapWithCompletableEffectHandler(
    initialEffects = setOf(Settings.Eff.FetchSettings)
) {
    ofType(Settings.Eff.FetchSettings::class.java)
        .flatMapCompletable {
            settingsRepository
                .fetchSettings()
                .backgroundToUi()
        }
        .toCompletable()
}.wrapWithObservableEffectHandler(
    initialEffects = setOf(Settings.Eff.ObserveSettings)
) {
    ofType(Settings.Eff.ObserveSettings::class.java)
        .switchMap {
            settingsRepository.observeSettings()
                .map<Settings.Msg> { Settings.Msg.OnSettingsChanged(it) }
                .backgroundToUi()
        }
}.wrapWithCompletableEffectHandler {
    ofType(Settings.Eff.UpdatedSettingValue::class.java)
        .flatMap {
            settingsRepository.updateSettingValue(it.setting, it.value, it.store)
                .toObservable<Experiments.Msg>()
                .backgroundToUi()
        }
        .toCompletable()
}.wrapWithCompletableEffectHandler {
    ofType(Settings.Eff.UpdatedFirstLaunch::class.java)
        .flatMap {
            settingsRepository.updateFirstLaunch(it.isFirstLaunch, it.store)
                .toObservable<Experiments.Msg>()
                .backgroundToUi()
        }
        .toCompletable()
}
