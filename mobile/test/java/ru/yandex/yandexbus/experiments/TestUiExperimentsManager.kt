package ru.yandex.yandexbus.experiments

import com.yandex.mapkit.experiments.UiExperimentsListener
import com.yandex.mapkit.experiments.UiExperimentsManager
import java.util.concurrent.CopyOnWriteArraySet

class TestUiExperimentsManager : UiExperimentsManager {

    private var parameters = mutableMapOf<String, String>()

    private val listeners = CopyOnWriteArraySet<UiExperimentsListener>()

    override fun setValue(serviceId: String, parameterName: String, value: String?) {
        throw NotImplementedError("use addOrUpdateExperiment instead")
    }

    fun addOrUpdateExperiment(experimentGroup: ExperimentGroup) {
        parameters[experimentGroup.name] = experimentGroup.value

        listeners.forEach { it.onParametersUpdated() }
    }

    fun removeExperiment(name: String) {
        parameters.remove(name)

        listeners.forEach { it.onParametersUpdated() }
    }

    override fun getParameters() = parameters

    override fun getValue(key: String) = parameters[key]

    override fun isValid() = true

    override fun subscribe(experimentsListener: UiExperimentsListener) {
        listeners.add(experimentsListener)
    }

    override fun unsubscribe(experimentsListener: UiExperimentsListener) {
        listeners.remove(experimentsListener)
    }
}
