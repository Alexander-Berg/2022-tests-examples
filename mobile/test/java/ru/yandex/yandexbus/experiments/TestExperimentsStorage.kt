package ru.yandex.yandexbus.experiments

class TestExperimentsStorage : ExperimentsStorage {

    private var data = mutableMapOf<String, String>()

    override fun prepare() {
        // already have in-memory data
    }

    override fun readAll() = data

    override fun writeAll(experiments: Map<String, String>) {
        data = experiments.toMutableMap()
    }

    override fun write(experimentName: String, experimentValue: String) {
        data[experimentName] = experimentValue
    }

    override fun remove(experimentName: String) {
        data.remove(experimentName)
    }
}
