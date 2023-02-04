package com.yandex.mobile.realty.testing

import com.android.build.gradle.internal.testing.ConnectedDeviceProvider
import com.android.builder.testing.api.DeviceConnector
import com.android.ddmlib.ShellCommandUnresponsiveException
import com.android.ddmlib.testrunner.AndroidTestOrchestratorRemoteAndroidTestRunner
import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner
import com.android.ddmlib.testrunner.TestIdentifier
import com.android.ddmlib.testrunner.TestRunResult
import io.qameta.allure.kotlin.AllureLifecycle
import io.qameta.allure.kotlin.FileSystemResultsWriter
import org.gradle.workers.WorkAction
import java.io.BufferedReader
import java.io.File
import java.util.concurrent.TimeUnit

private const val TESTS_NAME = "/data/local/tmp/tests.txt"
private const val ORCHESTRATOR_RESPONSE_TIMEOUT = 4L
private const val REBOOT_TIMEOUT = 3L

@Suppress("StringLiteralDuplication", "UnstableApiUsage")
abstract class DeviceProviderParallelInstrumentTestAction : WorkAction<DeviceProviderParallelInstrumentTestParameters> {

    override fun execute() {
        val deviceProvider = ConnectedDeviceProvider(parameters.adbLocation.get(), timeOutInMs, NullLogger(), null)
        deviceProvider.init()
        val devices = deviceProvider.devices
        deviceProvider.terminate()
        val serialNumber = parameters.serialNumber.get()
        val device = devices.first { it.serialNumber == serialNumber }
        clearScreenshots(serialNumber, parameters.deviceScreenshotDir.get())
        pushScreenshots(serialNumber, parameters.projectScreenshotDir.get(), parameters.deviceScreenshotDir.get())

        parameters.installPackages.get().forEach { file -> installPackage(serialNumber, file) }

        updateSetting(serialNumber, "window_animation_scale", "0")
        updateSetting(serialNumber, "transition_animation_scale", "0")
        updateSetting(serialNumber, "animator_duration_scale", "0")

        val allureResultsPath = "/sdcard/googletest/test_outputfiles/allure-results"

        val lifecycle = AllureLifecycle(
            FileSystemResultsWriter {
                File(
                    parameters.allureOutputDir.get(),
                    "allure-results"
                )
            }
        )

        val repeatCount = parameters.repeatCount.get()
        val topLevelTestRunResultAggregator = TestRunResultAggregator()
        repeat(repeatCount) { repeatIndex ->
            val testRunResultAggregator = TestRunResultAggregator()

            testRunResultAggregator.testRuns += runTests(device, lifecycle) {
                createMainTestRunner(device)
            }

            val retryCount = parameters.retryCount.get()

            for (i in 0 until retryCount) {
                val failedTests = testRunResultAggregator.getLatestFailedTests()
                if (failedTests.isEmpty()) break
                testRunResultAggregator.testRuns += runTests(device, lifecycle) {
                    createTestRunner(device).apply {
                        setRunName(device.name)
                        setClassNames(failedTests.map(TestIdentifier::toString).toTypedArray())
                    }
                }
            }
            val outputFile = File(
                parameters.xmlOutputDir.get(),
                "TEST-${device.name}-${parameters.index.get()}-${repeatIndex}.xml"
            )
            val testRunResult = testRunResultAggregator.aggregateTestRunResult()
            topLevelTestRunResultAggregator.testRuns += testRunResult
            TestRunResultXmlPrinter(testRunResult, outputFile)
                .generateDocument()
        }


        pullDirectory(
            serialNumber,
            allureResultsPath,
            parameters.allureOutputDir.get().absolutePath
        )
        parameters.testRunResultHolder.get().failedTestsNum =
            topLevelTestRunResultAggregator.aggregateTestRunResult().numAllFailedTests
    }

    private fun createMainTestRunner(device: DeviceConnector): RemoteAndroidTestRunner {

        val runner = createTestRunner(device)

        /** androidx.test.internal.runner.RunnerArgs - all possible args */
        fun Any.addAsArg(key: String): Unit = runner.addInstrumentationArg(key, toString())

        parameters.numShards.get().addAsArg("numShards")
        parameters.shardIndex.get().addAsArg("shardIndex")
        val packageName = parameters.packageName.get().takeIf { it.isNotEmpty() }
        val testsNames = parameters.testsNames.get().takeIf { it.isNotEmpty() }
        val notTestsNames = parameters.testsNotNames.get().takeIf { it.isNotEmpty() }
        val testsNamesFile = parameters.testsNamesFile.orNull
        if (testsNamesFile != null) {
            pushFile(device.serialNumber, testsNamesFile.asFile.path, TESTS_NAME)
            TESTS_NAME.addAsArg("testFile")
        }
        val notTestsNamesFile = parameters.testsNotNamesFile.orNull
        if (notTestsNamesFile != null) {
            pushFile(device.serialNumber, notTestsNamesFile.asFile.path, TESTS_NAME)
            TESTS_NAME.addAsArg("notTestFile")
        }
        testsNames?.addAsArg("class") ?: packageName?.addAsArg("package") ?: notTestsNames?.addAsArg("notClass")

        return runner
    }

    @Suppress("SwallowedException") // We use this exception as a timeout indicator
    private fun runTests(
        device: DeviceConnector,
        lifecycle: AllureLifecycle,
        runnerFactory: () -> IRemoteAndroidTestRunner,
    ): TestRunResult {
        val runListener = buildResultTestRunListener(
            device,
            lifecycle,
            errorsFilter = ResultTestRunListener.filterFailedRunAndTimeout
        )
        try {
            runnerFactory().run(runListener)
        } catch (e: ShellCommandUnresponsiveException) {
            rebootDevice(device)
            val retryRunListener = buildResultTestRunListener(device, lifecycle)
            runnerFactory().run(retryRunListener)
            return retryRunListener
        }
        return runListener
    }

    private fun buildResultTestRunListener(
        device: DeviceConnector,
        lifecycle: AllureLifecycle,
        errorsFilter: ErrorsFilter = ResultTestRunListener.filterFailedRunErrorsOnly,
    ): ResultTestRunListener = ResultTestRunListener(
        device,
        parameters.index.get(),
        errorsFilter,
        lifecycle
    )

    private fun installPackage(serialNumber: String, file: File) {
        println("$serialNumber installing '$file'")
        exec(
            parameters.adbLocation.get().path, "-s", serialNumber,
            "install", "-r", "--force-queryable", "-t", file.absolutePath
        )
    }

    private fun clearScreenshots(serialNumber: String, deviceDir: String) {
        println("clear screenshots on $serialNumber device")
        exec(
            parameters.adbLocation.get().path, "-s", serialNumber,
            "shell", "rm", "-rf", deviceDir
        )
    }

    private fun pushScreenshots(serialNumber: String, projectDir: String, deviceDir: String) {
        println("upload screenshots on $serialNumber device")
        exec(
            parameters.adbLocation.get().path, "-s", serialNumber,
            "push", "${projectDir}/.", deviceDir
        )
    }

    private fun pushFile(serialNumber: String, projectFile: String, deviceFile: String) {
        println("upload screenshots on $serialNumber device")
        exec(
            parameters.adbLocation.get().path, "-s", serialNumber,
            "push", projectFile, deviceFile
        )
    }

    private fun updateSetting(serialNumber: String, key: String, value: String) {
        println("$serialNumber setting '$key' to '$value'")
        exec(
            parameters.adbLocation.get().path, "-s", serialNumber,
            "shell", "-n", "settings", "put", "global", key, value
        )
    }

    private fun createTestRunner(device: DeviceConnector): RemoteAndroidTestRunner {
        val runner = AndroidTestOrchestratorRemoteAndroidTestRunner(
            "ru.auto.ara.debug.test",
            "ru.auto.ara.core.runner.AutoTestInstrumentationRunner",
            device,
            true
        )
        runner.setDebug(false)
        runner.addBooleanArg("clearPackageData", true)
        runner.setRunName(device.name)
        runner.setMaxTimeToOutputResponse(ORCHESTRATOR_RESPONSE_TIMEOUT, TimeUnit.MINUTES)
        return runner
    }

    private fun pullDirectory(serialNumber: String, remotePath: String, localPath: String) = exec(
        parameters.adbLocation.get().path, "-s", serialNumber,
        "pull", remotePath, localPath
    )

    private fun createDirectory(serialNumber: String, remotePath: String) = exec(
        parameters.adbLocation.get().path, "-s", serialNumber,
        "shell", "mkdir", remotePath
    )

    private fun rebootDevice(device: DeviceConnector) {
        val adbLocation = parameters.adbLocation.get()
        fun createAdbCommand(command: String) = "${adbLocation.absolutePath} -s ${device.serialNumber} $command"
        println("Rebooting ${device.serialNumber}")
        // reboot emulator
        val command = "${createAdbCommand("reboot")} " +
            //wait for reboot to complete
            "&& ${createAdbCommand("wait-for-disconnect")} " +
            "&& ${createAdbCommand("wait-for-device")} " +
            // rotate device back to portrait mode
            "&& ${createAdbCommand("shell settings put system accelerometer_rotation 0")} " +
            "&& ${createAdbCommand("shell settings put system user_rotation 0")}"
        Runtime.getRuntime()
            .exec(command)
            .waitFor(REBOOT_TIMEOUT, TimeUnit.MINUTES)
        println("${device.serialNumber} rebooted")
    }

    private fun exec(vararg params: String): Int {
        val process = ProcessBuilder().command(*params).start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val input = process.inputStream.bufferedReader().use(BufferedReader::readText)
            println(input)
            val error = process.errorStream.bufferedReader().use(BufferedReader::readText)
            println(error)
            println("The command exited with code <$exitCode>")
        }
        return exitCode
    }

    companion object {
        const val timeOutInMs = 60000
    }
}
