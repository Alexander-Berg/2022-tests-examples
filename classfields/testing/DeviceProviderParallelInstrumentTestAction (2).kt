package com.yandex.mobile.realty.testing

import com.android.builder.testing.api.DeviceConnector
import com.android.ddmlib.testrunner.AndroidTestOrchestratorRemoteAndroidTestRunner
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner
import com.android.utils.NullLogger
import org.gradle.workers.WorkAction
import java.io.File

/**
 * @author rogovalex on 2019-10-30.
 */
abstract class DeviceProviderParallelInstrumentTestAction : WorkAction<DeviceProviderParallelInstrumentTestParameters> {

    override fun execute() {
        val adbLocation = parameters.getAdbLocation().get()
        val devices = with(ConnectedDeviceProvider(adbLocation, 60000, NullLogger())) {
            init()
            devices.also { terminate() }
        }

        val serialNumber = parameters.getSerialNumber().get()
        val device = devices.first { it.serialNumber == serialNumber }

        parameters.getInstallPackages().get().forEach { file ->
            installPackage(serialNumber, file)
        }
        updateSetting(serialNumber, "window_animation_scale", "0")
        updateSetting(serialNumber, "transition_animation_scale", "0")
        updateSetting(serialNumber, "animator_duration_scale", "0")

        val screenshotsPath = "/sdcard/realty_screenshots"
        val updatedScreenshotsPath = "/sdcard/realty_screenshots_updated"
        removeDirectory(serialNumber, screenshotsPath)
        val takeScreenshotsMode = parameters.getTakeScreenshotsMode().get()
        if (!takeScreenshotsMode) {
            pushDirectory(
                serialNumber,
                screenshotsPath,
                parameters.getTestScreenshotsDir().get().absolutePath
            )
        }

        val allureResultsPath = "/sdcard/allure-results"
        removeDirectory(serialNumber, allureResultsPath)

        val runName = "${device.name}-${parameters.getTaskIndex().get()}"
        val runner = createTestRunner(device, runName, takeScreenshotsMode)
        runner.addInstrumentationArg("numShards", "${parameters.getNumShards().get()}")
        runner.addInstrumentationArg("shardIndex", "${parameters.getShardIndex().get()}")

        val testPackageName = parameters.getTestPackageName().get()
        val testClassName = parameters.getTestClassName().get()
        if (testClassName.isNotEmpty()) {
            runner.setClassName(testClassName)
        } else {
            runner.setTestPackageName(testPackageName)
        }

        val runListener = ResultTestRunListener(runName) {
            createTestRunner(device, runName, takeScreenshotsMode)
        }
        runListener.setReportDir(parameters.getXmlOutputDir().get())

        runner.run(runListener)

        pullDirectory(
            serialNumber,
            allureResultsPath,
            parameters.getAllureOutputDir().get().absolutePath
        )
        pullDirectory(
            serialNumber,
            "$updatedScreenshotsPath/.",
            parameters.getScreenshotsOutputDir().get().absolutePath
        )
    }

    private fun installPackage(serialNumber: String, file: File) {
        println("$serialNumber installing '$file'")
        ProcessBuilder()
            .command(
                parameters.getAdbLocation().get().path,
                "-s", serialNumber,
                "install", "-r", "-t", file.absolutePath
            )
            .start()
            .waitFor()
    }

    private fun updateSetting(serialNumber: String, key: String, value: String) {
        println("$serialNumber setting '$key' to '$value'")
        ProcessBuilder()
            .command(
                parameters.getAdbLocation().get().path,
                "-s", serialNumber,
                "shell", "-n", "settings", "put", "global", key, value
            )
            .start()
            .waitFor()
    }

    private fun createTestRunner(
        device: DeviceConnector,
        runName: String,
        takeScreenshotsMode: Boolean
    ): RemoteAndroidTestRunner {
        val runner = AndroidTestOrchestratorRemoteAndroidTestRunner(
            "com.yandex.mobile.realty.test",
            "com.yandex.mobile.realty.RealtyTestRunner",
            device,
            true
        )
        runner.setDebug(false)
        runner.addBooleanArg("clearPackageData", true)
        runner.addBooleanArg("takeScreenshotsMode", takeScreenshotsMode)
        runner.setRunName(runName)
        return runner
    }

    private fun pushDirectory(serialNumber: String, remotePath: String, localPath: String) {
        ProcessBuilder()
            .command(
                "adb",
                "-s", serialNumber,
                "push",
                localPath,
                remotePath
            )
            .start()
            .waitFor()
    }

    private fun pullDirectory(serialNumber: String, remotePath: String, locaPath: String) {
        ProcessBuilder()
            .command(
                "adb",
                "-s", serialNumber,
                "pull",
                remotePath,
                locaPath
            )
            .start()
            .waitFor()
    }

    private fun removeDirectory(serialNumber: String, remotePath: String) {
        ProcessBuilder()
            .command(
                parameters.getAdbLocation().get().path,
                "-s", serialNumber,
                "shell", "rm", "-rf", remotePath
            )
            .start()
            .waitFor()
    }
}
