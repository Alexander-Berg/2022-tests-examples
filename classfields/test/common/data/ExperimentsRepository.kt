package ru.auto.test.common.data

import ru.auto.data.util.LoadableData
import ru.auto.data.util.sideEffectMap
import ru.auto.settings.provider.ExperimentEntity
import ru.auto.settings.provider.ExperimentKey
import ru.auto.settings.provider.ExperimentsContentResolver
import ru.auto.settings.provider.SettingsContentResolver
import ru.auto.settings.provider.TestId
import ru.auto.settings.provider.TestIdEntity
import ru.auto.test.common.model.ExperimentsStore
import ru.auto.test.common.model.addUserTestId
import ru.auto.test.common.model.removeAllNoKeyUserTestIds
import ru.auto.test.common.model.removeUserTestId
import ru.auto.test.common.model.updateExperimentSelectedTestId
import ru.auto.test.common.utils.wrapToLoadableData
import rx.Completable
import rx.Observable
import rx.Single
import rx.subjects.BehaviorSubject

class ExperimentsRepository(
    private val preferencesStorage: PreferencesStorage,
    private val experimentsContentResolver: ExperimentsContentResolver,
    private val settingsContentResolver: SettingsContentResolver,
) {

    private val experimentsSubject = BehaviorSubject.create<LoadableData<ExperimentsStore>>(LoadableData.Initial)

    fun fetchExperiments(): Completable =
        Single.zip(
            Single.fromCallable { experimentsContentResolver.getExperiments() },
            Single.fromCallable { experimentsContentResolver.getTestIds() },
            preferencesStorage.getUserTestIds(),
            preferencesStorage.getSelectedTestIds(),
            ::ExperimentsStore
        )
            .wrapToLoadableData()
            .doOnSuccess(experimentsSubject::onNext)
            .toCompletable()

    fun observeExperiments(): Observable<LoadableData<ExperimentsStore>> =
        experimentsSubject.distinctUntilChanged()

    fun updateExperimentSelectedTestId(key: ExperimentKey?, testId: TestIdEntity?, experiments: ExperimentsStore): Completable =
        Single.fromCallable { experiments.updateExperimentSelectedTestId(key, testId) }
            .sideEffectMap { preferencesStorage.putSelectedTestIds(it.selectedTestIds) }
            .doOnSuccess {
                experimentsSubject.onNext(LoadableData.Success(it))
                settingsContentResolver.notifySettingValuesChange()
            }
            .toCompletable()

    fun addUserTestId(testId: TestIdEntity, experiments: ExperimentsStore): Completable =
        Single.fromCallable { experiments.addUserTestId(testId) }.updateUserTestIds()

    fun removeUserTestId(testId: TestIdEntity, experiments: ExperimentsStore): Completable =
        Single.fromCallable { experiments.removeUserTestId(testId) }
            .sideEffectMap { preferencesStorage.putSelectedTestIds(it.selectedTestIds) }
            .updateUserTestIds()
            .doOnCompleted { settingsContentResolver.notifySettingValuesChange() }

    fun removeNoKeyUserTestIds(experiments: ExperimentsStore): Completable =
        Single.fromCallable { experiments.removeAllNoKeyUserTestIds() }
            .sideEffectMap { preferencesStorage.putSelectedTestIds(it.selectedTestIds) }
            .updateUserTestIds()
            .doOnCompleted { settingsContentResolver.notifySettingValuesChange() }

    private fun Single<ExperimentsStore>.updateUserTestIds() =
        sideEffectMap { preferencesStorage.putUserTestIds(it.userTestIds.flatMap { entry -> entry.value }) }
            .doOnSuccess { experimentsSubject.onNext(LoadableData.Success(it)) }
            .toCompletable()

    private fun ExperimentsStore(
        experiments: List<ExperimentEntity>,
        testIds: List<TestIdEntity>,
        userTestIds: List<TestIdEntity>,
        selectedTestIds: Set<TestId>,
    ): ExperimentsStore {
        val testIdsGrouped: Map<String, List<TestIdEntity>> = testIds.groupBy(TestIdEntity::experimentKey)
            .mapValues { it.value.distinctBy(TestIdEntity::testId) }
        return ExperimentsStore(
            experiments = experiments
                .onlyNotEmptyTestIds(testIdsGrouped)
                .sortedBy(ExperimentEntity::name)
                .associateBy(ExperimentEntity::key),
            testIds = testIdsGrouped,
            userTestIds = userTestIds.groupBy(TestIdEntity::experimentKey),
            selectedTestIds = selectedTestIds.onlyActualTestIds(testIds, userTestIds)
        )
    }

    private fun List<ExperimentEntity>.onlyNotEmptyTestIds(testIds: Map<ExperimentKey, List<TestIdEntity>>) =
        filterNot { testIds[it.key].isNullOrEmpty() }

    private fun Set<TestId>.onlyActualTestIds(testIds: List<TestIdEntity>, userTestIds: List<TestIdEntity>) =
        filterTo(HashSet()) { testId -> testIds.any { it.testId == testId } || userTestIds.any { it.testId == testId } }

}
