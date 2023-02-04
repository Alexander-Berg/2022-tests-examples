package ru.auto.test.common.data

import ru.auto.data.util.LoadableData
import ru.auto.data.util.sideEffectMap
import ru.auto.settings.provider.SettingEntity
import ru.auto.settings.provider.SettingId
import ru.auto.settings.provider.SettingValue
import ru.auto.settings.provider.SettingsContentResolver
import ru.auto.test.common.model.SettingsStore
import ru.auto.test.common.model.updateSettingsValue
import ru.auto.test.common.utils.wrapToLoadableData
import rx.Completable
import rx.Observable
import rx.Single
import rx.subjects.BehaviorSubject

class SettingsRepository(
    private val settingsContentResolver: SettingsContentResolver,
    private val preferencesStorage: PreferencesStorage,
) {

    private val settingsSubject = BehaviorSubject.create<LoadableData<SettingsStore>>(LoadableData.Initial)

    fun fetchSettings(): Completable =
        Single.zip(
            Single.fromCallable { settingsContentResolver.getSettings() },
            preferencesStorage.getSettingValues(),
            preferencesStorage.isFirstLaunch(),
            ::SettingsStore
        )
            .wrapToLoadableData()
            .doOnSuccess(settingsSubject::onNext)
            .toCompletable()

    fun observeSettings(): Observable<LoadableData<SettingsStore>> =
        settingsSubject.distinctUntilChanged()

    fun updateSettingValue(setting: SettingEntity, value: SettingValue?, store: SettingsStore): Completable =
        Single.fromCallable { store.updateSettingsValue(setting, value) }
            .sideEffectMap { preferencesStorage.putSettingsValues(it.settingsValues) }
            .doOnSuccess {
                settingsSubject.onNext(LoadableData.Success(it))
                settingsContentResolver.notifySettingValuesChange()
            }
            .toCompletable()

    fun updateFirstLaunch(isFirstLaunch: Boolean, store: SettingsStore): Completable =
        Single.fromCallable { store.copy(isFirstLaunch = isFirstLaunch) }
            .sideEffectMap { preferencesStorage.putFirstLaunch(it.isFirstLaunch) }
            .doOnSuccess { settingsSubject.onNext(LoadableData.Success(it)) }
            .toCompletable()

    private fun SettingsStore(
        settings: List<SettingEntity>,
        settingsValues: Map<SettingId, SettingValue>,
        isFirstLaunch: Boolean
    ): SettingsStore = SettingsStore(
        settings = settings.groupBy(SettingEntity::group).mapValues { it.value.associateBy(SettingEntity::id) },
        settingsValues = settingsValues,
        isFirstLaunch = isFirstLaunch
    )

}
