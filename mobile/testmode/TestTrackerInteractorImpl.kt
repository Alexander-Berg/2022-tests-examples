package ru.yandex.yandexmaps.tools.tanker.sync.impl.testmode

import ru.yandex.yandexmaps.tools.tanker.sync.impl.Key
import ru.yandex.yandexmaps.tools.tanker.sync.impl.TrackerInteractor
import javax.inject.Inject

class TestTrackerInteractorImpl @Inject constructor() : TrackerInteractor {

    override fun createTask(keys: List<Key>, linkedTaskKey: String?): TrackerInteractor.Result {
        println("creating task for keys:\n${keys.joinToString("\n")}")
        println("linked task: $linkedTaskKey")
        return TrackerInteractor.Result.Success
    }
}
