package com.yandex.mobile.realty.testing.shell

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ShellScript internal constructor(workingDirectory: File? = null) {
    private val processBuilder = ProcessBuilder(listOf())
        .directory(workingDirectory)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)

    var defaultCallbacks: ProcessCallbacks = EmptyProcessCallbacks

    val arc = ArcCommand(this)

    fun command(
        command: String,
        arguments: List<String> = listOf(),
        callbacks: ProcessCallbacks = EmptyProcessCallbacks
    ): String = runCommand(command, arguments, callbacks) { it.retrieveOutput() }

    private fun <OutputT> runCommand(
        command: String,
        arguments: List<String>,
        callbacks: ProcessCallbacks,
        prepareOutput: (Process) -> OutputT
    ): OutputT = try {
        val splitCommand = listOf(command) + arguments
        val process = processBuilder
            .command(splitCommand)
            .start()
        onProcessStart(process, callbacks)
        process.waitFor(COMMAND_TIMEOUT, TimeUnit.MINUTES)
        prepareOutput(process)
    } catch (exception: IOException) {
        println(exception)
        throw ShellFailedException(exception)
    } catch (exception: InterruptedException) {
        println(exception)
        throw ShellFailedException(exception)
    }

    private fun Process.retrieveOutput(): String {
        val outputText = inputStream.bufferedReader().use(BufferedReader::readText)
        val exitCode = exitValue()
        if (exitCode != 0) {
            val errorText = errorStream.bufferedReader().use(BufferedReader::readText)
            if (errorText.isNotEmpty()) {
                println("exception: $errorText")
                throw ShellRunException(exitCode, errorText.trim())
            }
        }
        return outputText.trim()
    }

    private fun onProcessStart(process: Process, callbacks: ProcessCallbacks) {
        defaultCallbacks.onProcessStart(process)
        callbacks.onProcessStart(process)
    }

    private object EmptyProcessCallbacks : ProcessCallbacks

    companion object {
        private const val COMMAND_TIMEOUT = 5L
    }
}

interface ProcessCallbacks {
    fun onProcessStart(process: Process) {}
}

class ShellFailedException(cause: Throwable) : RuntimeException("Running shell command failed", cause)

data class ShellRunException(
    val exitCode: Int,
    val errorText: String
) : RuntimeException(
    "Running shell command failed with code $exitCode and message: $errorText"
)
