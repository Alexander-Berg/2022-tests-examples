package ru.yandex.yandexnavi.analytics

internal class TestParamsProvider(
    private val params: Map<String, String>,
    private val secondaryParams: Map<String, String> = emptyMap()
) : ParamsProvider {

    private lateinit var callback: ParamsCallback

    override fun getProvidableParams(): Set<String> = params.keys + secondaryParams.keys
    override fun requestParams(callback: ParamsCallback) {
        this.callback = callback
    }

    fun triggerFirstParams() {
        callback.onParamsProvided(params)
    }

    fun triggerSecondParams() {
        if (secondaryParams.isNotEmpty()) {
            callback.onParamsProvided(secondaryParams)
        }
    }
}
