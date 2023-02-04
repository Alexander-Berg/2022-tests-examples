package com.yandex.mobile.realty.testing

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters
import java.io.File

/**
 * @author rogovalex on 2019-10-30.
 */
@Suppress("ComplexInterface")
interface DeviceProviderParallelInstrumentTestParameters : WorkParameters {
    fun getSerialNumber(): Property<String>
    fun getNumShards(): Property<Int>
    fun getTaskIndex(): Property<Int>
    fun getShardIndex(): Property<Int>
    fun getTestPackageName(): Property<String>
    fun getTestClassName(): Property<String>
    fun getInstallPackages(): ListProperty<File>
    fun getXmlOutputDir(): Property<File>
    fun getAllureOutputDir(): Property<File>
    fun getAdbLocation(): Property<File>
    fun getTestScreenshotsDir(): Property<File>
    fun getTakeScreenshotsMode(): Property<Boolean>
    fun getScreenshotsOutputDir(): Property<File>
}
