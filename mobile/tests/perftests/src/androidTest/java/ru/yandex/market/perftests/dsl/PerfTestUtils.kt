package ru.yandex.market.perftests.dsl

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.yandex.perftests.core.IntervalReporter
import com.yandex.perftests.runner.ProcessReporter
import ru.yandex.market.common.featureconfigs.constants.FeatureConfigConstants
import ru.yandex.market.common.featureconfigs.managers.CatalogDepthConfigManager
import ru.yandex.market.common.featureconfigs.override.FeatureConfigOverrideItem
import ru.yandex.market.perftests.scenario.MarketPerfTest

const val PERF_TESTS_UUID_KEY = "uuid"
const val PERF_TESTS_FEATURE_CONFIGS_KEY = "feature_configs"
const val PERF_TESTS_DEVICE_ID_KEY = "deviceId"
const val PERF_TESTS_REARR_FACTORS_QUERY_KEY = "rearrFactorsQuery"
const val PERF_TESTS_REGION_ID_KEY = "regionId"
const val PERF_TESTS_RANDOM_GENERATOR_KEY = "randomGeneratedString"

fun MarketPerfTest.startMainActivity(launchTimeout: Long = 100000L) {
    perfTestUtils.startMainActivity(launchTimeout)
}

fun MarketPerfTest.clearData() {
    perfTestUtils.clearData()
}

fun MarketPerfTest.removeAccounts() {
    perfTestUtils.removeAccounts()
}

fun MarketPerfTest.setMetricaIdentifiers(uuid: String?, deviceId: String?) {
    perfTestUtils.withSettings {
        put(PERF_TESTS_UUID_KEY, uuid)
        put(PERF_TESTS_DEVICE_ID_KEY, deviceId)
    }
}

fun MarketPerfTest.setRandomGeneratedString(randomString: String?) {
    perfTestUtils.withSettings {
        put(PERF_TESTS_RANDOM_GENERATOR_KEY, randomString)
    }
}

fun MarketPerfTest.setNavNodeDepthFeatureConfigs(): FeatureConfigOverrideItem {
    return FeatureConfigOverrideItem(
        name = "navNodeDepth",
        value = JsonObject().apply {
            addProperty(FeatureConfigConstants.ENABLED_KEY, true)
            add(
                FeatureConfigConstants.PAYLOAD_KEY,
                JsonObject().apply {
                    add(
                        CatalogDepthConfigManager.INFO_DTO_FIELD,
                        JsonObject().apply {
                            addProperty(
                                CatalogDepthConfigManager.DEPTH_DTO_FIELD,
                                CatalogDepthConfigManager.NEW_TREE_DEPTH
                            )
                        }
                    )
                }
            )
        }.toString()
    )
}

fun MarketPerfTest.setFeatureConfigs(configs: List<FeatureConfigOverrideItem>) {
    perfTestUtils.withSettings {
        put(PERF_TESTS_FEATURE_CONFIGS_KEY, Gson().toJson(configs).toString())
    }
}

fun MarketPerfTest.setRearrFactorsQueryString(rearrFactors: String?) {
    perfTestUtils.withSettings {
        put(PERF_TESTS_REARR_FACTORS_QUERY_KEY, rearrFactors)
    }
}

fun MarketPerfTest.setRegionId(regionId: Long?) {
    perfTestUtils.withSettings {
        put(PERF_TESTS_REGION_ID_KEY, regionId)
    }
}

fun MarketPerfTest.clearSettings() {
    setMetricaIdentifiers(null, null)
    setRearrFactorsQueryString(null)
    setRandomGeneratedString(null)
    setRegionId(null)
}

fun MarketPerfTest.waitFrameMetrics(name: String) {
    perfTestUtils.waitAllMetrics(
        "$name.minTotalDuration",
        "$name.maxTotalDuration",
        "$name.averageTotalDurationMs",
        "$name.totalDurationMs",
        "$name.above8Ms333UsFramesPercent",
        "$name.above16Ms667UsFramesPercent",
        "$name.above33Ms333UsFramesPercent",
    )
}

fun MarketPerfTest.measureTime(name: String, block: () -> Unit) {
    val intervalReporter = try {
        IntervalReporter(name)
    } catch (e: RuntimeException) {
        e.printStackTrace()
        null
    }

    if (intervalReporter == null) {
        block.invoke()
    } else {
        intervalReporter.use { block.invoke() }
    }
}

fun MarketPerfTest.measureCpuTick(name: String, block: () -> Unit) {
    val processReporter = ProcessReporter(packageName, device)
    block.invoke()

    try {
        processReporter.report(name)
    } catch (e: RuntimeException) {
        e.printStackTrace()
    }
}

fun MarketPerfTest.measure(name: String, block: () -> Unit) {
    measureCpuTick("${name}_cpu") {
        measureTime("${name}_time", block)
    }
}
