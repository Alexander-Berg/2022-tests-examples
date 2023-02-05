package ru.yandex.yandexnavi.analytics

class TestParamsConsumer(
    private val acceptableParams: Set<String>,
    private val verifier: (params: Map<String, String>) -> Unit
) : ParamsConsumer {

    var counter = 0

    override fun getAcceptableParams(): Set<String> = acceptableParams

    override fun setParams(params: Map<String, String>) {
        verifier(params)
        counter++
    }
}
