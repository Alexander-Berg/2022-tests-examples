package ru.yandex.disk.testing

import android.content.ContentResolver
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import ru.yandex.disk.DeveloperSettings
import ru.yandex.disk.GlobalPreferences
import ru.yandex.disk.RuntimeConfig
import ru.yandex.disk.util.DevSettingsKeyValueStore

private const val TAG = "TestingConfigHandler"
private const val SETTINGS_AUTOTEST = "yandex.disk.boolean.autotest"
private const val SETTINGS_EXPERIMENTS = "yandex.disk.string.experiments"
private const val SETTINGS_ENABLED = "1"

class TestingConfigHandler(
    private val contentResolver: ContentResolver,
    @GlobalPreferences private val globalPreferences: SharedPreferences,
) {

    private val keyValueStore = DevSettingsKeyValueStore(globalPreferences)
    private val settingRegexpPattern = Regex("yandex\\.disk\\.(\\w+)\\.(.*)")

    fun apply(developerSettings: DeveloperSettings) {
        if (!isAutotest()) {
            return
        }

        prepareForTesting()
        val experiments = tryReadExperiments() ?: return
        applyExperiments(developerSettings, experiments)
    }

    private fun prepareForTesting() {
        contentResolver.query(
            Settings.Global.CONTENT_URI, arrayOf(Settings.NameValueTable.NAME, Settings.NameValueTable.VALUE),
            null, null, null
        ).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val settingRegexp = settingRegexpPattern.find(cursor.getString(0))
                    if (settingRegexp != null) {
                        val (type, settingName) = settingRegexp.destructured
                        when (type) {
                            "boolean" -> keyValueStore.put(settingName, cursor.getString(1) == SETTINGS_ENABLED)
                            "long" -> keyValueStore.put(settingName, cursor.getLong(1))
                            "int" -> keyValueStore.put(settingName, cursor.getInt(1))
                            "string" -> keyValueStore.put(settingName, cursor.getString(1))
                        }
                    }
                } while (cursor.moveToNext())
            }
        }
        RuntimeConfig.AUTOTEST = true
    }

    private fun applyExperiments(developerSettings: DeveloperSettings, experiments: List<String>) {
        developerSettings.setExperimentFlags(experiments)
        developerSettings.localExperimentsEnabled = true
    }

    private fun tryReadExperiments(): List<String>? {
        return getSetting(SETTINGS_EXPERIMENTS)
            ?.split(", ")
    }

    private fun isAutotest(): Boolean {
        return getSetting(SETTINGS_AUTOTEST) == SETTINGS_ENABLED
    }

    private fun getSetting(name: String): String? {
        return Settings.Global.getString(contentResolver, name).apply {
            Log.d(TAG, "setting: $name, value: $this")
        }
    }
}
