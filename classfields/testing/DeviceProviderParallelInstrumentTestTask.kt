package com.yandex.mobile.realty.testing

import com.android.build.gradle.internal.testing.ConnectedDeviceProvider
import com.android.builder.testing.api.DeviceConnector
import com.android.utils.NullLogger
import com.yandex.mobile.realty.testing.reporter.ReportType
import com.yandex.mobile.realty.testing.reporter.TestReport
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * @author rogovalex on 2019-10-30.
 */
abstract class DeviceProviderParallelInstrumentTestTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    private val configFile: File,
    private val appPackage: File,
    private val testsPackage: File,
    private val testUtilsPackage: File,
    private val orchestratorPackage: File,
    private val adbLocation: File,
    private val outputDir: File,
    private val xmlResultsDir: File,
    private val projectScreenshotDirPath: String,
    private val deviceScreenshotDirPath: String,
    private val testsPackageName: String,
    private val retryCount: Int,
    private val repeatCount: Int,
) : DefaultTask() {

    @get:Optional
    @get:Input
    abstract val testsNamesFile: RegularFileProperty

    @get:Optional
    @get:Input
    abstract val testsNotNamesFile: RegularFileProperty

    @get:Input
    abstract val testsNames: Property<String>

    @get:Input
    abstract val testsNotNames: Property<String>

    init {
        testsNamesFile.convention(null as RegularFile?)
        testsNotNamesFile.convention(null as RegularFile?)
        testsNames.convention("")
        testsNotNames.convention("")
    }

    @TaskAction
    fun run() {
        val deviceProvider = ConnectedDeviceProvider(adbLocation, timeOutInMs, NullLogger(), null)
        deviceProvider.init()

        var devices = deviceProvider.devices
        deviceProvider.terminate()
        val installPackages = listOf(
            appPackage,
            testsPackage,
            testUtilsPackage,
            orchestratorPackage
        )

        outputDir.deleteRecursively()
        outputDir.mkdirs()
        xmlResultsDir.mkdirs()

        val allureResultsDir = File(outputDir, "allure")
        allureResultsDir.mkdir()

        var startShardIndex = 0
        var index = 0
        var numShards = devices.size

        if (configFile.exists()) {
            startShardIndex = getShardValue("shardIndexStart")
            numShards = getShardValue("numShards")
            index = getShardValue("index")
            if (isDevicesMoreThanExpectedShards(devices.size, numShards, startShardIndex)) {
                println("Unnecessary emulators on agent. Adjusting emulators to shards count")
                devices = adjustEmulatorsCount(devices, numShards - startShardIndex)
            }

        } else {
            println("No ${configFile.name} file found")
        }

        val workQueue = workerExecutor.noIsolation()
        val testResultHolders = mutableListOf<TestRunResultHolder>()
        devices.forEachIndexed { shardIndex, device ->
            val testRunResultHolder = TestRunResultHolder()
            testResultHolders += testRunResultHolder
            workQueue.submit(DeviceProviderParallelInstrumentTestAction::class.java) {
                serialNumber.set(device.serialNumber)
                this.numShards.set(numShards)
                this.index.set(index)
                this.shardIndex.set(startShardIndex + shardIndex)
                this.installPackages.set(installPackages)
                xmlOutputDir.set(xmlResultsDir)
                allureOutputDir.set(allureResultsDir)
                adbLocation.set(this@DeviceProviderParallelInstrumentTestTask.adbLocation)
                packageName.set(testsPackageName)
                testsNamesFile.set(this@DeviceProviderParallelInstrumentTestTask.testsNamesFile)
                testsNotNamesFile.set(this@DeviceProviderParallelInstrumentTestTask.testsNotNamesFile)
                testsNames.set(this@DeviceProviderParallelInstrumentTestTask.testsNames)
                testsNotNames.set(this@DeviceProviderParallelInstrumentTestTask.testsNotNames)
                deviceScreenshotDir.set(deviceScreenshotDirPath)
                projectScreenshotDir.set(projectScreenshotDirPath)
                retryCount.set(this@DeviceProviderParallelInstrumentTestTask.retryCount)
                repeatCount.set(this@DeviceProviderParallelInstrumentTestTask.repeatCount)
                this.testRunResultHolder.set(testRunResultHolder)
            }
        }
        workQueue.await()
        val reportDir = File(outputDir, "html")
        reportDir.mkdirs()
        val report = TestReport(ReportType.SINGLE_FLAVOR, xmlResultsDir, reportDir)
        report.generateReport()

        val failuresCount = testResultHolders.sumOf { it.failedTestsNum }
        if (failuresCount > 0) {
            state.addFailure(
                TaskExecutionException(
                    this,
                    RuntimeException("Tests execution failed: $failuresCount failure(s)")
                )
            )
        }
    }

    private fun isDevicesMoreThanExpectedShards(devices: Int, totalShards: Int, startShard: Int) =
        totalShards - (startShard + devices) < 0

    private fun adjustEmulatorsCount(devices: List<DeviceConnector>, expectEmulators: Int): List<DeviceConnector> {
        val savedEmulators = devices.take(expectEmulators)
        val savedEmulatorsSerials = savedEmulators.map { it.serialNumber }
        println("saved emulators: $savedEmulatorsSerials")
        devices.forEach { if (it.serialNumber !in savedEmulatorsSerials) it.shutDownDevice(adbLocation) }
        return savedEmulators
    }

    private fun DeviceConnector.shutDownDevice(adbLocation: File) {
        fun createAdbCommand(command: String) = "${adbLocation.absolutePath} -s $serialNumber $command"
        println("Shutting down device  $serialNumber")
        // shut down emulator
        val command = "${createAdbCommand("reboot -p")} "
        Runtime.getRuntime()
            .exec(command)
            .waitFor(SHUTDOWN_TIMEOUT, TimeUnit.MINUTES)
        println("$serialNumber turning off...")
    }

    private fun getShardValue(key: String): Int {
        try {
            return org.json.JSONObject(configFile.readText()).getInt(key)
        } catch (e: org.json.JSONException) {
            println("Error occurred parsing ${configFile.name} file : " + e.message)
        }
        return 0
    }

    companion object {
        const val timeOutInMs = 60000
        const val SHUTDOWN_TIMEOUT = 1L
    }
}
