package ru.auto.ara.core

import ru.auto.experiments.ExperimentPayloadDefinition
import ru.auto.experiments.Experiments
import kotlin.reflect.KFunction1

typealias ExperimentsDefinition = ExperimentsScope.() -> Unit

fun experimentsOf(experiments: ExperimentsDefinition = {}) =
    TestExperimentsManager().apply { experiments(experimentsRegistry) }

fun TestExperimentsManager.withExperiments(experiments: ExperimentsDefinition, action: () -> Unit) {
    val snapshot = experimentsRegistry.copy()
    experiments(experimentsRegistry)
    action()
    experimentsRegistry = snapshot
}

class TestExperimentsManager : Experiments {

    var experimentsRegistry = ExperimentsRegistry()

    fun experiments(experiments: ExperimentsDefinition) {
        experimentsRegistry.apply(experiments)
    }

    override fun experiment(key: String, defaultValue: Boolean, payloadDefinition: ExperimentPayloadDefinition?): Boolean =
        experimentsRegistry.booleanExperiments[key] ?: defaultValue

    override fun experiment(key: String, defaultValue: Int, payloadDefinition: ExperimentPayloadDefinition?): Int =
        experimentsRegistry.intExperiments[key] ?: defaultValue

    override fun experiment(key: String, defaultValue: String, payloadDefinition: ExperimentPayloadDefinition?): String =
        experimentsRegistry.stringExperiments[key] ?: defaultValue

}

interface ExperimentsScope {
    infix fun KFunction1<Experiments, Boolean>.then(value: Boolean)
    infix fun KFunction1<Experiments, Int>.then(value: Int)
    infix fun KFunction1<Experiments, String>.then(value: String)
}

class ExperimentsRegistry(
    val booleanExperiments: MutableMap<String, Boolean> = HashMap(),
    val intExperiments: MutableMap<String, Int> = HashMap(),
    val stringExperiments: MutableMap<String, String> = HashMap(),
) : ExperimentsScope {


    override fun KFunction1<Experiments, Boolean>.then(value: Boolean) {
        booleanExperiments[ExperimentValuesProvider.experimentKey(this)] = value
    }

    override fun KFunction1<Experiments, Int>.then(value: Int) {
        intExperiments[ExperimentValuesProvider.experimentKey(this)] = value
    }

    override fun KFunction1<Experiments, String>.then(value: String) {
        stringExperiments[ExperimentValuesProvider.experimentKey(this)] = value
    }

    fun copy() = ExperimentsRegistry(
        booleanExperiments = booleanExperiments,
        intExperiments = intExperiments,
        stringExperiments = stringExperiments
    )
}

object ExperimentValuesProvider : Experiments {

    private var key: String? = null

    @JvmName("experimentKeyBoolean")
    fun experimentKey(experiment: Experiments.() -> Boolean): String {
        experiment.invoke(this)
        return requireNotNull(key)
    }

    @JvmName("experimentKeyInt")
    fun experimentKey(experiment: Experiments.() -> Int): String {
        experiment.invoke(this)
        return requireNotNull(key)
    }

    @JvmName("experimentKeyString")
    fun experimentKey(experiment: Experiments.() -> String): String {
        experiment.invoke(this)
        return requireNotNull(key)
    }

    override fun experiment(key: String, defaultValue: Boolean, payloadDefinition: ExperimentPayloadDefinition?): Boolean {
        this.key = key
        return defaultValue
    }

    override fun experiment(key: String, defaultValue: Int, payloadDefinition: ExperimentPayloadDefinition?): Int {
        this.key = key
        return defaultValue
    }

    override fun experiment(key: String, defaultValue: String, payloadDefinition: ExperimentPayloadDefinition?): String {
        this.key = key
        return defaultValue
    }

}
