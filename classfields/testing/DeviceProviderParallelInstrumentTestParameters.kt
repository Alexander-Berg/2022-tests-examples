package com.yandex.mobile.realty.testing

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters
import java.io.File

/**
 * @author rogovalex on 2019-10-30.
 */
interface DeviceProviderParallelInstrumentTestParameters : WorkParameters {
    val serialNumber: Property<String>
    val numShards: Property<Int>
    val index: Property<Int>
    val shardIndex: Property<Int>
    val packageName: Property<String>
    val testsNames: Property<String>
    val testsNotNames: Property<String>
    val testsNamesFile: RegularFileProperty
    val testsNotNamesFile: RegularFileProperty
    val installPackages: ListProperty<File>
    val xmlOutputDir: Property<File>
    val allureOutputDir: Property<File>
    val projectScreenshotDir: Property<String>
    val deviceScreenshotDir: Property<String>
    val adbLocation: Property<File>
    val retryCount: Property<Int>
    val repeatCount: Property<Int>
    val testRunResultHolder: Property<TestRunResultHolder>
}
