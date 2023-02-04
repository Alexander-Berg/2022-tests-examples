package ru.auto.test.common.model

import ru.auto.settings.provider.ExperimentEntity
import ru.auto.settings.provider.ExperimentKey
import ru.auto.settings.provider.TestId
import ru.auto.settings.provider.TestIdEntity

data class ExperimentsStore(
    val experiments: Map<String, ExperimentEntity>,
    val testIds: Map<String, List<TestIdEntity>>,
    val userTestIds: Map<String, List<TestIdEntity>>,
    val selectedTestIds: Set<String>,
) {
    val noKeyUserTestIds: List<TestIdEntity> = userTestIds.filterKeys { it !in experiments }.flatMap { it.value }
}

fun ExperimentsStore.updateExperimentSelectedTestId(experimentKey: ExperimentKey?, testId: TestIdEntity?): ExperimentsStore {
    val experimentTestIds = HashSet<TestId>()
    return when {
        experimentKey != null -> {
            testIds[experimentKey].orEmpty().mapTo(experimentTestIds, TestIdEntity::testId)
            userTestIds[experimentKey].orEmpty().mapTo(experimentTestIds, TestIdEntity::testId)
            copy(selectedTestIds = selectedTestIds.minus(experimentTestIds).plusNotNull(testId?.testId))
        }
        testId != null -> {
            if (testId.testId in selectedTestIds) {
                copy(selectedTestIds = selectedTestIds.minus(testId.testId))
            } else {
                copy(selectedTestIds = selectedTestIds.plus(testId.testId))
            }
        }
        else -> {
            this
        }
    }
}

fun ExperimentsStore.addUserTestId(testId: TestIdEntity): ExperimentsStore {
    val testIds = userTestIds[testId.experimentKey].orEmpty().plus(testId)
    return copy(userTestIds = userTestIds.plus(testId.experimentKey to testIds))
}

fun ExperimentsStore.removeUserTestId(testId: TestIdEntity): ExperimentsStore {
    val testIds = userTestIds[testId.experimentKey].orEmpty().minus(testId)
    return copy(
        userTestIds = if (testIds.isNotEmpty()) {
            userTestIds.plus(testId.experimentKey to testIds)
        } else {
            userTestIds.minus(testId.experimentKey)
        },
        selectedTestIds = selectedTestIds.minus(testId.testId)
    )
}

fun ExperimentsStore.removeAllNoKeyUserTestIds(): ExperimentsStore =
    copy(
        userTestIds = userTestIds.filterKeys { it in experiments },
        selectedTestIds = selectedTestIds.minus(noKeyUserTestIds.mapTo(HashSet()) { it.testId })
    )

private fun <T> Set<T>.plusNotNull(element: T?): Set<T> = if (element != null) plus(element) else this
