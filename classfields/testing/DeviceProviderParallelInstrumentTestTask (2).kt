package com.yandex.mobile.realty.testing

import com.android.utils.NullLogger
import com.google.gson.JsonParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/**
 * @author rogovalex on 2019-10-30.
 */
open class DeviceProviderParallelInstrumentTestTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    private val configFile: File,
    private val appPackage: File,
    private val testsPackage: File,
    private val testUtilsPackage: File,
    private val orchestratorPackage: File,
    private val adbLocation: File,
    private val outputDir: File,
    private val testScreenshotsDir: File,
    private val testPackageName: String,
    private val testClassName: String,
    private val takeScreenshotsMode: Boolean,
) : DefaultTask() {

    @TaskAction
    fun run() {
        val installPackages = listOf(
            appPackage,
            testsPackage,
            testUtilsPackage,
            orchestratorPackage
        )

        outputDir.deleteRecursively()
        outputDir.mkdirs()
        val xmlResultsDir = File(outputDir, "xml")
        xmlResultsDir.mkdirs()
        val allureResultsDir = File(outputDir, "allure")
        allureResultsDir.mkdir()
        val screenshotsOutputDir = File(outputDir, "screenshots")
        screenshotsOutputDir.mkdir()

        val devices = with(ConnectedDeviceProvider(adbLocation, 60000, NullLogger())) {
            init()
            devices.also { terminate() }
        }

        val taskIndex: Int
        val numShards: Int
        val shardIndexOffset: Int

        if (configFile.exists()) {
            val config = JsonParser.parseString(configFile.readText()).asJsonObject
            taskIndex = config.getAsJsonPrimitive(SplitConfig.TASK_INDEX).asInt
            numShards = config.getAsJsonPrimitive(SplitConfig.NUM_SHARDS).asInt
            shardIndexOffset = config.getAsJsonPrimitive(SplitConfig.SHARD_INDEX_OFFSET).asInt
        } else {
            taskIndex = 0
            numShards = devices.size
            shardIndexOffset = 0
        }

        val workQueue = workerExecutor.noIsolation()
        devices.forEachIndexed { shardIndex, device ->
            workQueue.submit(DeviceProviderParallelInstrumentTestAction::class.java) {
                getSerialNumber().set(device.serialNumber)
                getTaskIndex().set(taskIndex)
                getNumShards().set(numShards)
                getShardIndex().set(shardIndexOffset + shardIndex)
                getTestPackageName().set(testPackageName)
                getTestClassName().set(testClassName)
                getInstallPackages().set(installPackages)
                getXmlOutputDir().set(xmlResultsDir)
                getAllureOutputDir().set(allureResultsDir)
                getAdbLocation().set(adbLocation)
                getTestScreenshotsDir().set(testScreenshotsDir)
                getTakeScreenshotsMode().set(takeScreenshotsMode)
                getScreenshotsOutputDir().set(screenshotsOutputDir)
            }
        }
        workQueue.await()
    }
}
