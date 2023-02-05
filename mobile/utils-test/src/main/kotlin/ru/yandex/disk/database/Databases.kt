@file:JvmName(name = "Databases")

package ru.yandex.disk.database

import android.content.Context
import com.google.common.io.Files
import java.io.File
import java.io.IOException

@JvmName(name = "recreateDbFromTestResources")
fun recreateDbFromTestResources(context: Context, dbName: String, dbPath: String) {
    context.openOrCreateDatabase(dbName, 0, null, null) //need to have access through getDatabasePath
    val storedDBfile = File(context.javaClass.getResource(dbPath)!!.file)
    val output = context.getDatabasePath(dbName)
    try {
        Files.copy(storedDBfile, output)
    } catch (e: IOException) {
        throw RuntimeException(e)
    }

}