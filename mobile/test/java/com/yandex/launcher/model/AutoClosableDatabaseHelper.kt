package com.yandex.launcher.model

import android.content.Context

/**
 * Wrapper class for [LauncherGlobalPreferencesProvider.DatabaseHelper] to avoid implementing [AutoCloseable] in prod
 */
class AutoClosableDatabaseHelper(context: Context): LauncherGlobalPreferencesProvider.DatabaseHelper(context), AutoCloseable