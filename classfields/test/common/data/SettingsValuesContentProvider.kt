package ru.auto.test.common.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import ru.auto.settings.provider.SettingValuesContract.AUTHORITY
import ru.auto.settings.provider.SettingValuesContract.AUTHORITY_DEBUG
import ru.auto.settings.provider.SettingValuesContract.SettingValues
import ru.auto.test.common.di.componentManager
import ru.auto.test.common.utils.isBuildTypeDebug

class SettingsValuesContentProvider : ContentProvider() {

    private val preferencesStorage by lazy {
        requireNotNull(context?.componentManager).settingsValuesContentProviderDependencies().preferencesStorage
    }

    private val uriMatcher by lazy {
        UriMatcher(UriMatcher.NO_MATCH).apply {
            val authority = if (isBuildTypeDebug) AUTHORITY_DEBUG else AUTHORITY
            addURI(authority, SettingValues.PATH, CODE_SETTING_VALUES)
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = when (uriMatcher.match(uri)) {
        CODE_SETTING_VALUES -> {
            val settingValues = preferencesStorage.getSettingValues().toBlocking().value()
            val selectedTestIds = preferencesStorage.getRawSelectedTestIds().toBlocking().value()
            val cursor = MatrixCursor(arrayOf(SettingValues.COLUMN_ID, SettingValues.COLUMN_VALUE))
            settingValues.entries.forEach { (id, value) -> cursor.addRow(arrayOf(id, value.value.toString())) }
            if (selectedTestIds != null) {
                cursor.addRow(arrayOf(SettingValues.SELECTED_TEST_IDS_KEY, selectedTestIds))
            }
            cursor
        }
        else -> null
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    interface Dependencies {
        val preferencesStorage: PreferencesStorage
    }

    companion object {
        private const val CODE_SETTING_VALUES = 0
    }
}
