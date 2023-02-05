package ru.yandex.yandexmaps.common.conductor

class ConfigurationChangeAwareTestActivity : TestActivity() {

    private var _isChangingConfigurations: Boolean = false

    fun triggerConfigurationChange() {
        _isChangingConfigurations = true
        try {
            onConfigurationChanged(resources.configuration)
            router.destroyViews()
            router.createViews()
        } finally {
            _isChangingConfigurations = false
        }
    }

    override fun isChangingConfigurations(): Boolean {
        return _isChangingConfigurations
    }
}
