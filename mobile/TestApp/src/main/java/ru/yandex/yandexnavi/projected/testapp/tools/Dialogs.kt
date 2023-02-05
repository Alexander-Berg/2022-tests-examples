package ru.yandex.yandexnavi.projected.testapp.tools

import android.content.Context
import androidx.appcompat.app.AlertDialog
import ru.yandex.yandexnavi.projected.testapp.R

fun showBuildRouteDialog(context: Context, callback: () -> Unit) {
    AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog)
        .setMessage("Build route to?")
        .setCancelable(true)
        .setPositiveButton("Ok") { _, _ ->
            callback()
        }
        .setNegativeButton("Cancel") { _, _ -> }
        .show()
}
