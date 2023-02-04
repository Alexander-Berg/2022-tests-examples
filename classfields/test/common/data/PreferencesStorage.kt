package ru.auto.test.common.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import ru.auto.data.util.fromJson
import ru.auto.settings.provider.SettingValue
import ru.auto.settings.provider.TestId
import ru.auto.settings.provider.TestIdEntity
import rx.Completable
import rx.Single

class PreferencesStorage(
    private val gson: Gson,
    private val context: Context,
) {

    private val prefs by lazy { context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) }

    fun putUserTestIds(testIds: List<TestIdEntity>): Completable =
        prefs.putStringAsCompletable(KEY_USER_TEST_IDS) { gson.toJson(testIds) }

    fun getUserTestIds(): Single<List<TestIdEntity>> =
        prefs.getStringAsSingle(KEY_USER_TEST_IDS)
            .map { userTestIdsJson ->
                userTestIdsJson?.let {
                    gson.fromJson<List<TestIdEntity>>(userTestIdsJson)
                }.orEmpty()
            }
            .onErrorReturn { emptyList() }

    fun putSelectedTestIds(testIds: Set<TestId>): Completable =
        prefs.putStringAsCompletable(KEY_SELECTED_TEST_IDS) { gson.toJson(testIds) }

    fun getSelectedTestIds(): Single<Set<TestId>> =
        prefs.getStringAsSingle(KEY_SELECTED_TEST_IDS)
            .map { selectedTestIdsJson ->
                selectedTestIdsJson?.let {
                    gson.fromJson<Set<TestId>>(selectedTestIdsJson)
                }.orEmpty()
            }
            .onErrorReturn { emptySet() }

    fun getRawSelectedTestIds(): Single<String?> =
        prefs.getStringAsSingle(KEY_SELECTED_TEST_IDS).onErrorReturn { null }

    fun putSettingsValues(values: Map<String, SettingValue>): Completable =
        prefs.putStringAsCompletable(KEY_SETTINGS_VALUES) { gson.toJson(values.mapValues { it.value.value.toString() }) }

    fun getSettingValues(): Single<Map<String, SettingValue>> =
        prefs.getStringAsSingle(KEY_SETTINGS_VALUES)
            .map { valuesJson ->
                valuesJson?.let {
                    gson.fromJson<Map<String, String>>(valuesJson)
                        .mapValues {
                            val booleanValue = it.value.toBooleanStrictOrNull()
                            if (booleanValue != null) {
                                SettingValue.BooleanValue(booleanValue)
                            } else {
                                SettingValue.StringValue(it.value)
                            }
                        }
                }.orEmpty()
            }
            .onErrorReturn { emptyMap() }

    fun putFirstLaunch(isFirstLaunch: Boolean): Completable =
        prefs.putBooleanAsCompletable(KEY_IS_FIRST_LAUNCH) { isFirstLaunch }

    fun isFirstLaunch(): Single<Boolean> =
        prefs.getBooleanAsSingle(KEY_IS_FIRST_LAUNCH, defValue = true)

    private fun SharedPreferences.putStringAsCompletable(key: String, value: () -> String?) =
        Completable.fromAction { edit().putString(key, value.invoke()).commit() }

    private fun SharedPreferences.getStringAsSingle(key: String, defValue: String? = null): Single<String?> =
        Single.fromCallable { getString(key, defValue) }

    private fun SharedPreferences.putBooleanAsCompletable(key: String, value: () -> Boolean) =
        Completable.fromAction { edit().putBoolean(key, value.invoke()).commit() }

    private fun SharedPreferences.getBooleanAsSingle(key: String, defValue: Boolean = false): Single<Boolean> =
        Single.fromCallable { getBoolean(key, defValue) }

    companion object {

        private const val PREFERENCES_NAME = "settings"
        private const val KEY_USER_TEST_IDS = "user_test_ids"
        private const val KEY_SELECTED_TEST_IDS = "selected_test_ids"
        private const val KEY_SETTINGS_VALUES = "settings_values"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
    }


}
