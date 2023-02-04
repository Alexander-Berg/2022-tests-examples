package com.yandex.mobile.realty.testing

import com.google.gson.JsonObject
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * @author rogovalex on 2019-10-30.
 */
open class GenerateSandboxSplitConfigTestTask @Inject constructor(
    private val configFile: File,
    private val numTasks: Int,
    private val numShards: Int
) : DefaultTask() {

    @TaskAction
    fun run() {
        println("Generate split config")
        println("Split config path:   ${configFile.absolutePath}")
        println("Number of tasks:     $numTasks")
        println("Number of shards:    $numShards")
        val totalShards = numShards * numTasks
        val config = JsonObject()
        for (taskIndex in 0 until numTasks) {
            val taskConfig = JsonObject()
            taskConfig.addProperty(SplitConfig.TASK_INDEX, taskIndex)
            taskConfig.addProperty(SplitConfig.NUM_SHARDS, totalShards)
            taskConfig.addProperty(SplitConfig.SHARD_INDEX_OFFSET, taskIndex * numShards)
            config.add(taskIndex.toString(), taskConfig)
        }
        configFile.writeText(config.toString())
    }
}
