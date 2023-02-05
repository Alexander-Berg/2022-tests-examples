package com.yandex.mail.tools

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

class TestWorkerFactory : WorkerFactory() {

    private val startedWorkers: MutableList<WorkerInfo> = mutableListOf()

    val workerWhiteList: MutableList<Class<*>> = mutableListOf()

    fun getAllStartedWorkers(): List<WorkerInfo> {
        return startedWorkers
    }

    fun getAllStartedWorkers(clazz: Class<*>): List<WorkerInfo> {
        return startedWorkers.filter { it.workerClassName == clazz.name }
    }

    fun clearStartedWorkers() {
        startedWorkers.clear()
    }

    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
        return if (workerWhiteList.isEmpty() || workerWhiteList.any { it.name == workerClassName }) {
            startedWorkers.add(WorkerInfo(workerClassName, workerParameters))
            null
        } else {
            NoOpWorker(appContext, workerParameters)
        }
    }

    fun reset() {
        startedWorkers.clear()
        workerWhiteList.clear()
    }

    class WorkerInfo(
        val workerClassName: String,
        val workerParameters: WorkerParameters
    ) {

        fun getInputBoolean(key: String, defaultValue: Boolean = false): Boolean = workerParameters.inputData.getBoolean(key, defaultValue)

        fun getInputBooleanArray(key: String): BooleanArray? = workerParameters.inputData.getBooleanArray(key)

        fun getInputByte(key: String, defaultValue: Byte = 0): Byte = workerParameters.inputData.getByte(key, defaultValue)

        fun getInputByteArray(key: String): ByteArray? = workerParameters.inputData.getByteArray(key)

        fun getInputInt(key: String, defaultValue: Int = 0): Int = workerParameters.inputData.getInt(key, defaultValue)

        fun getInputIntArray(key: String): IntArray? = workerParameters.inputData.getIntArray(key)

        fun getInputLong(key: String, defaultValue: Long = 0L): Long = workerParameters.inputData.getLong(key, defaultValue)

        fun getInputLongArray(key: String): LongArray? = workerParameters.inputData.getLongArray(key)

        fun getInputFloat(key: String, defaultValue: Float = 0f): Float = workerParameters.inputData.getFloat(key, defaultValue)

        fun getInputFloatArray(key: String): FloatArray? = workerParameters.inputData.getFloatArray(key)

        fun getInputDouble(key: String, defaultValue: Double = 0.0): Double = workerParameters.inputData.getDouble(key, defaultValue)

        fun getInputDoubleArray(key: String): DoubleArray? = workerParameters.inputData.getDoubleArray(key)

        fun getInputString(key: String): String? = workerParameters.inputData.getString(key)

        fun getInputStringArray(key: String): Array<String>? = workerParameters.inputData.getStringArray(key)
    }
}
