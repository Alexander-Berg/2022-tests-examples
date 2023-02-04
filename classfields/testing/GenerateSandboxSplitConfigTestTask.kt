package com.yandex.mobile.realty.testing

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import kotlin.math.ceil

/**
 * @author rogovalex on 2019-10-30.
 */
open class GenerateSandboxSplitConfigTestTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    private val dir: File,
    private val configFile: File,
    private val emulatorsPerAgent: String,
    private val countOfAgent: String,
    private val testsPerEmulator: String,
) : DefaultTask() {

    @TaskAction
    fun run() {
        if (countOfAgent.toIntOrNull() != null) {
            println("Generate split config")
            writeConfig(countOfAgent.toInt(), emulatorsPerAgent.toInt())
        } else {
            val testsCount = ChangedTestListGenerator().generate().size
            println("testCount $testsCount")
            val dynamicEmulatorCount = ceil(testsCount.toDouble() / testsPerEmulator.toInt()).toInt()
            val dynamicAgentCount = ceil(dynamicEmulatorCount.toDouble() / emulatorsPerAgent.toInt()).toInt()
            println("Generate split config dynamically")
            writeConfig(dynamicAgentCount, emulatorsPerAgent.toInt(), dynamicEmulatorCount)
        }
        val workQueue = workerExecutor.noIsolation()
        workQueue.await()
    }

    private fun writeConfig(agentCount: Int, emulatorsCount: Int, totalShards: Int? = null) {
        println("Split config path:   ${configFile.absolutePath}")
        println("Count of emulators per agent:  $emulatorsCount")
        println("Count of agents:     $agentCount")
        val numShards = totalShards ?: (emulatorsCount * agentCount)
        println("Total shards:     $numShards")
        val config = JSONObject()
        for (i in 0 until agentCount) {
            config.put(
                i.toString(), JSONObject()
                    .put("index", i)
                    .put("shardIndexStart", i * emulatorsCount)
                    .put("numShards", numShards)
            )
        }
        println("Split config: $config")
        dir.mkdirs()
        configFile.writeText(config.toString())
    }
}
