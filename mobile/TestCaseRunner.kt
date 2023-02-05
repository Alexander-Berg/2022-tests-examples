package com.yandex.frankenstein.agent.client

import android.app.Activity
import okhttp3.OkHttpClient
import org.json.JSONObject
import timber.log.Timber

fun runTestCase(
    activity: Activity,
    environment: JSONObject,
    commandImplementations: CommandImplementations,
    client: OkHttpClient,
    testObjectStorage: TestObjectStorage = TestObjectStorage()
) = runTestCase({ activity }, environment, commandImplementations, client, testObjectStorage)

fun runTestCase(
    activityProvider: () -> Activity,
    environment: JSONObject,
    commandImplementations: CommandImplementations,
    client: OkHttpClient,
    testObjectStorage: TestObjectStorage = TestObjectStorage()
) {
    val commandsProvider = CommandsProvider(client, environment.getJSONObject("commandRequest"), Thread::sleep)
    val resultReporter = ResultReporter(client, environment.getJSONObject("commandResult"))
    val callbackReporter = CallbackReporter(client, environment.getJSONObject("commandCallback"))

    commandsProvider.request { commands ->
        commands.forEach { command ->
            val commandImplementation = commandImplementations.get(command.clazz, command.name)
            val commandLog = "#${command.id}: ${command.clazz}::${command.name}${command.arguments}"
            Timber.i("Run command %s", commandLog)
            commandImplementation(CommandInput(activityProvider(), client, testObjectStorage,
                command.id, command.arguments, resultReporter, callbackReporter))
            Timber.i("Finish command %s", commandLog)
        }
    }
}
